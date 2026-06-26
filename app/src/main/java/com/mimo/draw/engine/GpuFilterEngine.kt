package com.mimo.draw.engine

import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class GpuFilterEngine {
    private var programId = 0
    private var vertexBuffer: FloatBuffer? = null
    private var texCoordBuffer: FloatBuffer? = null
    private var textureId = 0
    private var fboId = 0
    private var fboTextureId = 0
    private var isInitialized = false

    private val vertexShaderSource = """
        #version 300 es
        layout(location = 0) in vec4 aPosition;
        layout(location = 1) in vec2 aTexCoord;
        out vec2 vTexCoord;
        void main() {
            gl_Position = aPosition;
            vTexCoord = aTexCoord;
        }
    """.trimIndent()

    private val fragmentShaderTemplate = """
        #version 300 es
        precision highp float;
        uniform sampler2D uTexture;
        uniform float uIntensity;
        uniform vec2 uTexelSize;
        in vec2 vTexCoord;
        out vec4 fragColor;

        %s

        void main() {
            vec4 color = texture(uTexture, vTexCoord);
            fragColor = applyFilter(color);
        }
    """.trimIndent()

    private val filterShaders = mapOf(
        "blur" to """
            vec4 applyFilter(vec4 color) {
                vec4 sum = vec4(0.0);
                float kernel[9] = float[](
                    1.0/16.0, 2.0/16.0, 1.0/16.0,
                    2.0/16.0, 4.0/16.0, 2.0/16.0,
                    1.0/16.0, 2.0/16.0, 1.0/16.0
                );
                int index = 0;
                for(int y = -1; y <= 1; y++) {
                    for(int x = -1; x <= 1; x++) {
                        vec2 offset = vec2(float(x), float(y)) * uTexelSize * uIntensity;
                        sum += texture(uTexture, vTexCoord + offset) * kernel[index];
                        index++;
                    }
                }
                return sum;
            }
        """,
        "sharpen" to """
            vec4 applyFilter(vec4 color) {
                vec4 sum = vec4(0.0);
                float kernel[9] = float[](
                    0.0, -1.0, 0.0,
                    -1.0, 5.0, -1.0,
                    0.0, -1.0, 0.0
                );
                int index = 0;
                for(int y = -1; y <= 1; y++) {
                    for(int x = -1; x <= 1; x++) {
                        vec2 offset = vec2(float(x), float(y)) * uTexelSize;
                        sum += texture(uTexture, vTexCoord + offset) * kernel[index] * uIntensity;
                        index++;
                    }
                }
                return sum + color * (1.0 - uIntensity);
            }
        """,
        "brightness" to """
            vec4 applyFilter(vec4 color) {
                return color + vec4(uIntensity, uIntensity, uIntensity, 0.0);
            }
        """,
        "contrast" to """
            vec4 applyFilter(vec4 color) {
                return vec4(
                    (color.r - 0.5) * uIntensity + 0.5,
                    (color.g - 0.5) * uIntensity + 0.5,
                    (color.b - 0.5) * uIntensity + 0.5,
                    color.a
                );
            }
        """,
        "saturation" to """
            vec4 applyFilter(vec4 color) {
                float gray = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
                return mix(vec4(gray, gray, gray, color.a), color, uIntensity);
            }
        """,
        "hue_rotate" to """
            vec4 applyFilter(vec4 color) {
                float angle = uIntensity * 3.14159265;
                float s = sin(angle);
                float c = cos(angle);
                vec3 weights = vec3(0.2126, 0.7152, 0.0722);
                float oneminusc = 1.0 - c;
                mat3 hueRotate = mat3(
                    vec3(weights.x + oneminusc * (1.0 - weights.x), weights.x + oneminusc * (0.0 - weights.x) - s * 0.143, weights.x + oneminusc * (0.0 - weights.x) + s * (1.0 - weights.x)),
                    vec3(weights.y + oneminusc * (0.0 - weights.y) + s * 0.140, weights.y + oneminusc * (1.0 - weights.y), weights.y + oneminusc * (0.0 - weights.y) - s * 0.283),
                    vec3(weights.z + oneminusc * (0.0 - weights.z) - s * (1.0 - weights.z), weights.z + oneminusc * (0.0 - weights.z) + s * 0.283, weights.z + oneminusc * (1.0 - weights.z))
                );
                return vec4(clamp(color.rgb * hueRotate, 0.0, 1.0), color.a);
            }
        """,
        "vignette" to """
            vec4 applyFilter(vec4 color) {
                vec2 uv = vTexCoord * (1.0 - vTexCoord);
                float vig = uv.x * uv.y * 15.0;
                vig = pow(vig, 0.2 + uIntensity * 0.3);
                return vec4(color.rgb * vig, color.a);
            }
        """,
        "grayscale" to """
            vec4 applyFilter(vec4 color) {
                float gray = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
                return mix(color, vec4(gray, gray, gray, color.a), uIntensity);
            }
        """,
        "sepia" to """
            vec4 applyFilter(vec4 color) {
                vec3 sepia = vec3(
                    dot(color.rgb, vec3(0.393, 0.769, 0.189)),
                    dot(color.rgb, vec3(0.349, 0.686, 0.168)),
                    dot(color.rgb, vec3(0.272, 0.534, 0.131))
                );
                return vec4(mix(color.rgb, sepia, uIntensity), color.a);
            }
        """,
        "invert" to """
            vec4 applyFilter(vec4 color) {
                return mix(color, vec4(1.0 - color.rgb, color.a), uIntensity);
            }
        """,
        "noise" to """
            float rand(vec2 co) {
                return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
            }
            vec4 applyFilter(vec4 color) {
                float noise = rand(vTexCoord) * uIntensity * 0.2;
                return vec4(color.rgb + vec3(noise), color.a);
            }
        """,
        "pixelate" to """
            vec4 applyFilter(vec4 color) {
                float pixels = max(1.0, 100.0 - uIntensity * 99.0);
                vec2 coord = floor(vTexCoord * pixels) / pixels;
                return texture(uTexture, coord);
            }
        """,
        "emboss" to """
            vec4 applyFilter(vec4 color) {
                vec4 sum = vec4(0.0);
                float kernel[9] = float[](
                    -2.0, -1.0, 0.0,
                    -1.0, 1.0, 1.0,
                    0.0, 1.0, 2.0
                );
                int index = 0;
                for(int y = -1; y <= 1; y++) {
                    for(int x = -1; x <= 1; x++) {
                        vec2 offset = vec2(float(x), float(y)) * uTexelSize;
                        sum += texture(uTexture, vTexCoord + offset) * kernel[index];
                        index++;
                    }
                }
                return mix(color, vec4(sum.rgb, color.a), uIntensity);
            }
        """
    )

    fun initialize() {
        if (isInitialized) return

        val vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexShaderSource)
        val fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, buildFragmentShader("blur"))
        programId = linkProgram(vertexShader, fragmentShader)

        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)

        val vertices = floatArrayOf(
            -1f, -1f, 0f, 0f,
             1f, -1f, 1f, 0f,
            -1f,  1f, 0f, 1f,
             1f,  1f, 1f, 1f
        )

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
            .also { it.position(0) }

        texCoordBuffer = ByteBuffer.allocateDirect(8 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f))
            .also { it.position(0) }

        val texIds = IntArray(1)
        GLES30.glGenTextures(1, texIds, 0)
        textureId = texIds[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        isInitialized = true
    }

    fun applyFilter(input: Bitmap, filterType: String, intensity: Float): Bitmap {
        if (!isInitialized) return input

        val width = input.width
        val height = input.height

        val program = compileFilterProgram(filterType)
        GLES30.glUseProgram(program)

        GLES30.glViewport(0, 0, width, height)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, input, 0)

        val texelSizeLoc = GLES30.glGetUniformLocation(program, "uTexelSize")
        GLES30.glUniform2f(texelSizeLoc, 1f / width, 1f / height)

        val intensityLoc = GLES30.glGetUniformLocation(program, "uIntensity")
        GLES30.glUniform1f(intensityLoc, intensity)

        val posLoc = GLES30.glGetAttribLocation(program, "aPosition")
        GLES30.glEnableVertexAttribArray(posLoc)
        GLES30.glVertexAttribPointer(posLoc, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer)

        val texLoc = GLES30.glGetAttribLocation(program, "aTexCoord")
        GLES30.glEnableVertexAttribArray(texLoc)
        GLES30.glVertexAttribPointer(texLoc, 2, GLES30.GL_FLOAT, false, 8, texCoordBuffer)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val buffer = ByteBuffer.allocateDirect(width * height * 4)
        GLES30.glReadPixels(0, 0, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer)

        buffer.position(0)
        output.copyPixelsFromBuffer(buffer)

        GLES30.glDeleteProgram(program)

        return output
    }

    fun applyMultipleFilters(
        input: Bitmap,
        filters: List<Pair<String, Float>>
    ): Bitmap {
        var current = input
        for ((filterType, intensity) in filters) {
            if (intensity == 0f) continue
            val result = applyFilter(current, filterType, intensity)
            if (current !== input) {
                current.recycle()
            }
            current = result
        }
        return current
    }

    private fun buildFragmentShader(filterType: String): String {
        val filterBody = filterShaders[filterType] ?: filterShaders["blur"]!!
        return fragmentShaderTemplate.replace("%s", filterBody)
    }

    private fun compileFilterProgram(filterType: String): Int {
        val vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexShaderSource)
        val fragmentShader = compileShader(
            GLES30.GL_FRAGMENT_SHADER,
            buildFragmentShader(filterType)
        )
        return linkProgram(vertexShader, fragmentShader)
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)

        val compiled = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val log = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            throw RuntimeException("Shader compilation failed: $log")
        }

        return shader
    }

    private fun linkProgram(vertexShader: Int, fragmentShader: Int): Int {
        val program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vertexShader)
        GLES30.glAttachShader(program, fragmentShader)
        GLES30.glLinkProgram(program)

        val linked = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linked, 0)
        if (linked[0] == 0) {
            val log = GLES30.glGetProgramInfoLog(program)
            GLES30.glDeleteProgram(program)
            throw RuntimeException("Program linking failed: $log")
        }

        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)

        return program
    }

    fun destroy() {
        if (!isInitialized) return
        GLES30.glDeleteTextures(1, intArrayOf(textureId), 0)
        GLES30.glDeleteProgram(programId)
        isInitialized = false
    }

    companion object {
        val AVAILABLE_FILTERS = listOf(
            "blur", "sharpen", "brightness", "contrast", "saturation",
            "hue_rotate", "vignette", "grayscale", "sepia", "invert",
            "noise", "pixelate", "emboss"
        )
    }
}
