package com.mimo.draw

import android.graphics.Bitmap
import android.os.Bundle
import android.view.InputDevice
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    private var hardwarePressure = 0.5f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF4CAF50),
                    secondary = Color(0xFF8BC34A),
                    tertiary = Color(0xFFCDDC39),
                    background = Color(0xFF1A1A2E),
                    surface = Color(0xFF16213E),
                    onPrimary = Color.White,
                    onSecondary = Color.White,
                    onTertiary = Color.White,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                DrawingApp(viewModel = viewModel())
            }
        }
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_MOVE ||
            event.actionMasked == MotionEvent.ACTION_DOWN) {
            val pressure = event.getAxisValue(MotionEvent.AXIS_PRESSURE)
            if (pressure > 0f) {
                hardwarePressure = pressure.coerceIn(0.05f, 1f)
            }
        }
        return super.dispatchGenericMotionEvent(event)
    }

    fun getHardwarePressure(): Float = hardwarePressure
}

@Composable
fun DrawingApp(viewModel: DrawingViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    var initialized by remember { mutableStateOf(false) }
    val activity = context as? MainActivity

    LaunchedEffect(canvasSize) {
        if (!initialized && canvasSize.width > 0 && canvasSize.height > 0) {
            viewModel.initialize(canvasSize.width.toInt(), canvasSize.height.toInt())
            initialized = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                state = state,
                onUndo = viewModel::undo,
                onRedo = viewModel::redo,
                onClear = viewModel::clearCanvas,
                onSave = {
                    if (canvasSize.width > 0 && canvasSize.height > 0) {
                        val bitmap = Bitmap.createBitmap(
                            canvasSize.width.toInt(), canvasSize.height.toInt(),
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = android.graphics.Canvas(bitmap)
                        canvas.drawColor(state.canvasBackground.toArgb())

                        state.shapes.forEach { shapePath ->
                            val paint = android.graphics.Paint().apply {
                                color = shapePath.color.copy(alpha = shapePath.alpha).toArgb()
                                strokeWidth = shapePath.strokeWidth
                                style = if (shapePath.filled) android.graphics.Paint.Style.FILL
                                else android.graphics.Paint.Style.STROKE
                                isAntiAlias = true
                            }
                            when (shapePath.shape) {
                                Shape.LINE -> canvas.drawLine(
                                    shapePath.start.x, shapePath.start.y,
                                    shapePath.end.x, shapePath.end.y, paint
                                )
                                Shape.RECTANGLE, Shape.ROUNDED_RECTANGLE -> {
                                    val l = minOf(shapePath.start.x, shapePath.end.x)
                                    val t = minOf(shapePath.start.y, shapePath.end.y)
                                    val r = maxOf(shapePath.start.x, shapePath.end.x)
                                    val b = maxOf(shapePath.start.y, shapePath.end.y)
                                    canvas.drawRect(l, t, r, b, paint)
                                }
                                Shape.CIRCLE -> {
                                    val cx = (shapePath.start.x + shapePath.end.x) / 2
                                    val cy = (shapePath.start.y + shapePath.end.y) / 2
                                    val rad = kotlin.math.sqrt(
                                        (shapePath.end.x - shapePath.start.x).let { it * it } +
                                        (shapePath.end.y - shapePath.start.y).let { it * it }
                                    ) / 2
                                    canvas.drawCircle(cx, cy, rad, paint)
                                }
                                else -> {}
                            }
                        }

                        state.paths.forEach { drawPath ->
                            val paint = android.graphics.Paint().apply {
                                color = drawPath.color.copy(alpha = drawPath.alpha).toArgb()
                                style = android.graphics.Paint.Style.STROKE
                                strokeCap = android.graphics.Paint.Cap.ROUND
                                strokeJoin = android.graphics.Paint.Join.ROUND
                                isAntiAlias = true
                            }
                            if (drawPath.points.size >= 2) {
                                if (drawPath.usePressure && drawPath.pressures.size == drawPath.points.size) {
                                    for (i in 1 until drawPath.points.size) {
                                        val segPaint = android.graphics.Paint(paint).apply {
                                            strokeWidth = drawPath.strokeWidth * drawPath.pressures[i]
                                        }
                                        canvas.drawLine(
                                            drawPath.points[i - 1].x, drawPath.points[i - 1].y,
                                            drawPath.points[i].x, drawPath.points[i].y, segPaint
                                        )
                                    }
                                } else {
                                    paint.strokeWidth = drawPath.strokeWidth
                                    val path = android.graphics.Path().apply {
                                        moveTo(drawPath.points[0].x, drawPath.points[0].y)
                                        for (i in 1 until drawPath.points.size) {
                                            lineTo(drawPath.points[i].x, drawPath.points[i].y)
                                        }
                                    }
                                    canvas.drawPath(path, paint)
                                }
                            }
                        }

                        state.texts.forEach { textElement ->
                            val paint = android.graphics.Paint().apply {
                                color = textElement.color.copy(alpha = textElement.alpha).toArgb()
                                textSize = textElement.fontSize
                                isAntiAlias = true
                            }
                            canvas.drawText(textElement.text, textElement.position.x, textElement.position.y, paint)
                        }

                        val saved = viewModel.saveToGallery(context, bitmap)
                        Toast.makeText(
                            context,
                            if (saved) "Saved to gallery!" else "Failed to save",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onToggleGrid = viewModel::toggleGrid,
                onToggleSymmetry = viewModel::toggleSymmetry
            )

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                DrawingCanvas(
                    state = state,
                    onTouchStart = { pos ->
                        val pressure = activity?.getHardwarePressure() ?: 0.5f
                        viewModel.onTouchStart(pos, pressure)
                    },
                    onTouchMove = { pos ->
                        val pressure = activity?.getHardwarePressure() ?: 0.5f
                        viewModel.onTouchMove(pos, pressure)
                    },
                    onTouchEnd = viewModel::onTouchEnd,
                    onSizeChanged = { size -> canvasSize = size }
                )

                if (state.showColorPicker) {
                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        ColorPickerPanel(
                            selectedColor = state.selectedColor,
                            onColorSelected = viewModel::onColorSelected,
                            onDismiss = viewModel::toggleColorPicker
                        )
                    }
                }
                if (state.showToolSettings) {
                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        ToolSettingsPanel(
                            state = state,
                            onStrokeWidthChanged = viewModel::onStrokeWidthChanged,
                            onAlphaChanged = viewModel::onAlphaChanged,
                            onDismiss = viewModel::toggleToolSettings
                        )
                    }
                }
                if (state.showLayers) {
                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        LayersPanel(
                            state = state,
                            onAddLayer = viewModel::addLayer,
                            onRemoveLayer = viewModel::removeLayer,
                            onToggleVisibility = viewModel::toggleLayerVisibility,
                            onSetActiveLayer = viewModel::setActiveLayer,
                            onDismiss = viewModel::toggleLayers
                        )
                    }
                }
                if (state.showFilters) {
                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        FiltersPanel(
                            state = state,
                            onFilterChanged = viewModel::onFilterChanged,
                            onDismiss = viewModel::toggleFilters
                        )
                    }
                }
                if (state.showTextEditor) {
                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        TextEditorPanel(
                            state = state,
                            onTextChanged = viewModel::onTextChanged,
                            onFontSizeChanged = viewModel::onTextFontSizeChanged,
                            onFontFamilyChanged = viewModel::onTextFontFamilyChanged,
                            onToggleBold = viewModel::toggleTextBold,
                            onToggleItalic = viewModel::toggleTextItalic,
                            onDismiss = viewModel::toggleTextEditor
                        )
                    }
                }
                if (state.showHistory) {
                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        HistoryPanel(
                            state = state,
                            onDismiss = viewModel::toggleHistory
                        )
                    }
                }
            }

            BottomToolBar(
                state = state,
                onToolSelected = viewModel::onToolSelected,
                onColorPickerToggle = viewModel::toggleColorPicker,
                onToolSettingsToggle = viewModel::toggleToolSettings,
                onLayersToggle = viewModel::toggleLayers,
                onFiltersToggle = viewModel::toggleFilters
            )
        }
    }
}
