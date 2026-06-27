package com.mimo.draw

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

data class DrawPath(
    val points: List<Offset>,
    val pressures: List<Float> = emptyList(),
    val color: Color,
    val strokeWidth: Float,
    val tool: Tool,
    val alpha: Float = 1f,
    val layerId: String = "default",
    val blurRadius: Float = 0f,
    val hardness: Float = 1f,
    val usePressure: Boolean = false
)

data class ShapePath(
    val start: Offset,
    val end: Offset,
    val color: Color,
    val strokeWidth: Float,
    val shape: Shape,
    val alpha: Float = 1f,
    val filled: Boolean = false,
    val layerId: String = "default",
    val cornerRadius: Float = 0f
)

data class TextElement(
    val text: String,
    val position: Offset,
    val color: Color,
    val fontSize: Float,
    val fontFamily: String = "default",
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val alpha: Float = 1f,
    val layerId: String = "default",
    val rotation: Float = 0f,
    val letterSpacing: Float = 0f
)

data class ImageElement(
    val uri: Uri,
    val position: Offset,
    val size: Size,
    val alpha: Float = 1f,
    val rotation: Float = 0f,
    val layerId: String = "default"
)

data class Layer(
    val id: String,
    val name: String,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val opacity: Float = 1f,
    val blendMode: BlendMode = BlendMode.NORMAL
)

enum class BlendMode {
    NORMAL, MULTIPLY, SCREEN, OVERLAY, DARKEN, LIGHTEN
}

data class Gradient(
    val colors: List<Color>,
    val type: GradientType,
    val start: Offset = Offset.Zero,
    val end: Offset = Offset(1f, 1f)
)

enum class GradientType {
    LINEAR, RADIAL, SWEEP
}

data class Filter(
    val type: FilterType,
    val intensity: Float = 1f
)

enum class FilterType {
    BLUR, SHARPEN, BRIGHTNESS, CONTRAST, SATURATION, HUE_ROTATE, INVERT, GRAYSCALE, SEPIA, VIGNETTE,
    NOISE, PIXELATE, EMBOSS
}

data class DrawingState(
    val paths: List<DrawPath> = emptyList(),
    val shapes: List<ShapePath> = emptyList(),
    val texts: List<TextElement> = emptyList(),
    val images: List<ImageElement> = emptyList(),
    val currentPath: DrawPath? = null,
    val currentShape: ShapePath? = null,
    val selectedColor: Color = Color.Black,
    val selectedTool: Tool = Tool.PEN,
    val strokeWidth: Float = 8f,
    val alpha: Float = 1f,
    val undoStack: List<DrawingAction> = emptyList(),
    val redoStack: List<DrawingAction> = emptyList(),
    val canvasBackground: Color = Color.White,
    val showColorPicker: Boolean = false,
    val showToolSettings: Boolean = false,
    val showLayers: Boolean = false,
    val showTextEditor: Boolean = false,
    val showFilters: Boolean = false,
    val showTemplates: Boolean = false,
    val showHistory: Boolean = false,
    val showExportOptions: Boolean = false,
    val layers: List<Layer> = listOf(Layer("default", "图层 1")),
    val activeLayerId: String = "default",
    val zoom: Float = 1f,
    val panOffset: Offset = Offset.Zero,
    val isSymmetryEnabled: Boolean = false,
    val symmetryType: SymmetryType = SymmetryType.VERTICAL,
    val showGrid: Boolean = false,
    val gridSize: Float = 20f,
    val snapToGrid: Boolean = false,
    val selectedGradient: Gradient? = null,
    val currentText: String = "",
    val textFontSize: Float = 24f,
    val textFontFamily: String = "default",
    val textBold: Boolean = false,
    val textItalic: Boolean = false,
    val filters: List<Filter> = emptyList(),
    val canvasSize: Size = Size(1080f, 1920f),
    val historyIndex: Int = -1,
    val isPanning: Boolean = false,
    val eyedropperActive: Boolean = false,
    val eyedropperColor: Color? = null,
    val pressureEnabled: Boolean = false,
    val pressureSensitivity: Float = 1.5f,
    val gpuFiltersEnabled: Boolean = true,
    val layerCount: Int = 1,
    val maxLayers: Int = Int.MAX_VALUE,
    val bitmapPoolStats: BitmapPoolStats? = null
)

data class BitmapPoolStats(
    val poolSize: Int,
    val hitRate: Float,
    val totalLayers: Int
)

sealed class DrawingAction {
    data class AddPath(val path: DrawPath) : DrawingAction()
    data class AddShape(val shape: ShapePath) : DrawingAction()
    data class AddText(val text: TextElement) : DrawingAction()
    data class AddImage(val image: ImageElement) : DrawingAction()
    data class RemovePath(val path: DrawPath) : DrawingAction()
    data class RemoveShape(val shape: ShapePath) : DrawingAction()
    data class ClearCanvas(val paths: List<DrawPath>, val shapes: List<ShapePath>, val texts: List<TextElement>, val images: List<ImageElement>) : DrawingAction()
    data class AddLayer(val layer: Layer) : DrawingAction()
    data class RemoveLayer(val layerId: String) : DrawingAction()
    data class ToggleLayerVisibility(val layerId: String) : DrawingAction()
}

enum class Tool {
    PEN,
    BRUSH,
    CALLIGRAPHY,
    SPRAY,
    ERASER,
    LINE,
    RECTANGLE,
    CIRCLE,
    TRIANGLE,
    FILL,
    TEXT,
    EYEDROPPER,
    CROP,
    BLUR,
    LASSO,
    MOVE
}

enum class Shape {
    LINE,
    RECTANGLE,
    CIRCLE,
    TRIANGLE,
    ROUNDED_RECTANGLE,
    STAR,
    HEART,
    ARROW
}

enum class SymmetryType {
    VERTICAL,
    HORIZONTAL,
    QUADRANT,
    RADIAL
}

val DEFAULT_COLORS = listOf(
    Color(0xFF000000),
    Color(0xFFFFFFFF),
    Color(0xFFFF0000),
    Color(0xFFFF6B35),
    Color(0xFFFFC107),
    Color(0xFF4CAF50),
    Color(0xFF2196F3),
    Color(0xFF9C27B0),
    Color(0xFFE91E63),
    Color(0xFF795548),
    Color(0xFF607D8B),
    Color(0xFF00BCD4),
    Color(0xFF8BC34A),
    Color(0xFFFF5722),
    Color(0xFF3F51B5),
    Color(0xFF009688),
    Color(0xFFCDDC39),
    Color(0xFF673AB7),
    Color(0xFFFF9800),
    Color(0xFF03A9F4),
    Color(0xFFE040FB),
    Color(0xFF1DE9B6),
    Color(0xFFFF6E40),
    Color(0xFF536DFE),
    Color(0xFFFFAB40),
    Color(0xFFB2FF59),
    Color(0xFF448AFF),
    Color(0xFFFF4081),
    Color(0xFF18FFFF),
    Color(0xFFEEFF41)
)

val TOOL_COLORS = listOf(
    Color(0xFFFF5252),
    Color(0xFFFF4081),
    Color(0xFFE040FB),
    Color(0xFF7C4DFF),
    Color(0xFF536DFE),
    Color(0xFF448AFF),
    Color(0xFF40C4FF),
    Color(0xFF18FFFF),
    Color(0xFF64FFDA),
    Color(0xFF69F0AE),
    Color(0xFFB2FF59),
    Color(0xFFEEFF41),
    Color(0xFFFFD740),
    Color(0xFFFFAB40),
    Color(0xFFFF6E40),
    Color(0xFFFF5722)
)

val FONT_FAMILIES = listOf(
    "Default",
    "Serif",
    "Sans-Serif",
    "Monospace",
    "Cursive"
)

val FILTER_PRESETS = listOf(
    Filter(FilterType.BLUR, 0f),
    Filter(FilterType.SHARPEN, 0f),
    Filter(FilterType.BRIGHTNESS, 0f),
    Filter(FilterType.CONTRAST, 0f),
    Filter(FilterType.SATURATION, 0f),
    Filter(FilterType.HUE_ROTATE, 0f),
    Filter(FilterType.INVERT, 0f),
    Filter(FilterType.GRAYSCALE, 0f),
    Filter(FilterType.SEPIA, 0f),
    Filter(FilterType.VIGNETTE, 0f),
    Filter(FilterType.NOISE, 0f),
    Filter(FilterType.PIXELATE, 0f),
    Filter(FilterType.EMBOSS, 0f)
)

val TEMPLATES = listOf(
    "Blank Canvas",
    "Social Media Post (1080x1080)",
    "Story (1080x1920)",
    "YouTube Thumbnail (1280x720)",
    "A4 Document (210x297mm)",
    "Business Card (90x50mm)",
    "Poster (24x36in)",
    "Instagram Profile (320x320)"
)
