package com.mimo.draw

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
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
}

@Composable
fun DrawingApp(viewModel: DrawingViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopBar(
                state = state,
                onUndo = viewModel::undo,
                onRedo = viewModel::redo,
                onClear = viewModel::clearCanvas,
                onSave = {
                    if (canvasSize.width > 0 && canvasSize.height > 0) {
                        val bitmap = Bitmap.createBitmap(
                            canvasSize.width.toInt(),
                            canvasSize.height.toInt(),
                            Bitmap.Config.ARGB_8888
                        )
                        val canvas = android.graphics.Canvas(bitmap)
                        canvas.drawColor(state.canvasBackground.toArgb())

                        state.shapes.forEach { shapePath ->
                            val paint = android.graphics.Paint().apply {
                                color = shapePath.color.copy(alpha = shapePath.alpha).toArgb()
                                strokeWidth = shapePath.strokeWidth
                                style = if (shapePath.filled) android.graphics.Paint.Style.FILL else android.graphics.Paint.Style.STROKE
                                isAntiAlias = true
                            }

                            when (shapePath.shape) {
                                Shape.LINE -> {
                                    canvas.drawLine(
                                        shapePath.start.x, shapePath.start.y,
                                        shapePath.end.x, shapePath.end.y,
                                        paint
                                    )
                                }
                                Shape.RECTANGLE, Shape.ROUNDED_RECTANGLE -> {
                                    val left = minOf(shapePath.start.x, shapePath.end.x)
                                    val top = minOf(shapePath.start.y, shapePath.end.y)
                                    val right = maxOf(shapePath.start.x, shapePath.end.x)
                                    val bottom = maxOf(shapePath.start.y, shapePath.end.y)
                                    canvas.drawRect(left, top, right, bottom, paint)
                                }
                                Shape.CIRCLE -> {
                                    val centerX = (shapePath.start.x + shapePath.end.x) / 2
                                    val centerY = (shapePath.start.y + shapePath.end.y) / 2
                                    val radius = kotlin.math.sqrt(
                                        (shapePath.end.x - shapePath.start.x) * (shapePath.end.x - shapePath.start.x) +
                                        (shapePath.end.y - shapePath.start.y) * (shapePath.end.y - shapePath.start.y)
                                    ) / 2
                                    canvas.drawCircle(centerX, centerY, radius, paint)
                                }
                                Shape.TRIANGLE -> {
                                    val midX = (shapePath.start.x + shapePath.end.x) / 2
                                    val path = android.graphics.Path().apply {
                                        moveTo(midX, shapePath.start.y)
                                        lineTo(shapePath.end.x, shapePath.end.y)
                                        lineTo(shapePath.start.x, shapePath.end.y)
                                        close()
                                    }
                                    canvas.drawPath(path, paint)
                                }
                                else -> {}
                            }
                        }

                        state.paths.forEach { drawPath ->
                            val paint = android.graphics.Paint().apply {
                                color = drawPath.color.copy(alpha = drawPath.alpha).toArgb()
                                strokeWidth = drawPath.strokeWidth
                                style = android.graphics.Paint.Style.STROKE
                                strokeCap = android.graphics.Paint.Cap.ROUND
                                strokeJoin = android.graphics.Paint.Join.ROUND
                                isAntiAlias = true
                            }

                            if (drawPath.points.size >= 2) {
                                val path = android.graphics.Path().apply {
                                    moveTo(drawPath.points[0].x, drawPath.points[0].y)
                                    for (i in 1 until drawPath.points.size) {
                                        lineTo(drawPath.points[i].x, drawPath.points[i].y)
                                    }
                                }
                                canvas.drawPath(path, paint)
                            }
                        }

                        state.texts.forEach { textElement ->
                            val paint = android.graphics.Paint().apply {
                                color = textElement.color.copy(alpha = textElement.alpha).toArgb()
                                textSize = textElement.fontSize
                                isAntiAlias = true
                            }
                            canvas.drawText(
                                textElement.text,
                                textElement.position.x,
                                textElement.position.y,
                                paint
                            )
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
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                DrawingCanvas(
                    state = state,
                    onTouchStart = viewModel::onTouchStart,
                    onTouchMove = viewModel::onTouchMove,
                    onTouchEnd = viewModel::onTouchEnd,
                    onSizeChanged = { size -> canvasSize = size }
                )

                Box(
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    AnimatedVisibility(
                        visible = state.showColorPicker,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        ColorPickerPanel(
                            selectedColor = state.selectedColor,
                            onColorSelected = viewModel::onColorSelected,
                            onDismiss = viewModel::toggleColorPicker
                        )
                    }
                }

                Box(
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    AnimatedVisibility(
                        visible = state.showToolSettings,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        ToolSettingsPanel(
                            state = state,
                            onStrokeWidthChanged = viewModel::onStrokeWidthChanged,
                            onAlphaChanged = viewModel::onAlphaChanged,
                            onDismiss = viewModel::toggleToolSettings
                        )
                    }
                }

                Box(
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    AnimatedVisibility(
                        visible = state.showLayers,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
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

                Box(
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    AnimatedVisibility(
                        visible = state.showFilters,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        FiltersPanel(
                            state = state,
                            onFilterChanged = viewModel::onFilterChanged,
                            onDismiss = viewModel::toggleFilters
                        )
                    }
                }

                Box(
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    AnimatedVisibility(
                        visible = state.showTextEditor,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
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

                Box(
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    AnimatedVisibility(
                        visible = state.showHistory,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
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
