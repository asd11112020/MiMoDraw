package com.mimo.draw

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.mimo.draw.engine.BitmapPool
import com.mimo.draw.engine.FilterEngine
import com.mimo.draw.engine.LayerBitmapManager
import com.mimo.draw.engine.PressureEngine
import com.mimo.draw.engine.EnhancedDrawingRenderer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class DrawingViewModel : ViewModel() {
    private val bitmapPool = BitmapPool()
    private val layerManager = LayerBitmapManager(bitmapPool)
    private val filterEngine = FilterEngine(bitmapPool)
    private val pressureEngine = PressureEngine()
    private val renderer = EnhancedDrawingRenderer(bitmapPool, layerManager)

    private val _state = MutableStateFlow(DrawingState())
    val state: StateFlow<DrawingState> = _state.asStateFlow()

    private var pendingPressures = ArrayList<Float>()

    fun initialize(width: Int, height: Int) {
        renderer.initialize(width, height)
        layerManager.createLayer("default", width, height)
    }

    fun onToolSelected(tool: Tool) {
        _state.value = _state.value.copy(
            selectedTool = tool,
            eyedropperActive = tool == Tool.EYEDROPPER
        )
    }

    fun onColorSelected(color: Color) {
        _state.value = _state.value.copy(selectedColor = color)
    }

    fun onStrokeWidthChanged(width: Float) {
        _state.value = _state.value.copy(strokeWidth = width)
    }

    fun onAlphaChanged(alpha: Float) {
        _state.value = _state.value.copy(alpha = alpha)
    }

    fun onZoomChanged(zoom: Float) {
        _state.value = _state.value.copy(zoom = zoom.coerceIn(0.1f, 5f))
    }

    fun onPanChanged(offset: Offset) {
        _state.value = _state.value.copy(panOffset = offset)
    }

    fun togglePressure() {
        _state.value = _state.value.copy(pressureEnabled = !_state.value.pressureEnabled)
    }

    fun onPressureSensitivityChanged(sensitivity: Float) {
        _state.value = _state.value.copy(pressureSensitivity = sensitivity)
        pressureEngine.sensitivity = sensitivity
    }

    fun toggleSymmetry() {
        _state.value = _state.value.copy(isSymmetryEnabled = !_state.value.isSymmetryEnabled)
    }

    fun onSymmetryTypeChanged(type: SymmetryType) {
        _state.value = _state.value.copy(symmetryType = type)
    }

    fun toggleGrid() {
        _state.value = _state.value.copy(showGrid = !_state.value.showGrid)
    }

    fun onGridSizeChanged(size: Float) {
        _state.value = _state.value.copy(gridSize = size)
    }

    fun toggleSnapToGrid() {
        _state.value = _state.value.copy(snapToGrid = !_state.value.snapToGrid)
    }

    fun onTouchStart(position: Offset, pressure: Float = 0.5f) {
        val currentState = _state.value
        val adjustedPosition = if (currentState.snapToGrid) {
            snapToGrid(position, currentState.gridSize)
        } else {
            position
        }

        pendingPressures.clear()
        pendingPressures.add(pressure)

        renderer.startNewStroke()

        when {
            currentState.selectedTool == Tool.EYEDROPPER -> return
            currentState.selectedTool == Tool.FILL -> return
            currentState.selectedTool == Tool.MOVE -> {
                _state.value = currentState.copy(isPanning = true)
                return
            }
            currentState.selectedTool == Tool.TEXT -> {
                val text = TextElement(
                    text = "点击编辑",
                    position = adjustedPosition,
                    color = currentState.selectedColor,
                    fontSize = currentState.textFontSize,
                    fontFamily = currentState.textFontFamily,
                    isBold = currentState.textBold,
                    isItalic = currentState.textItalic,
                    alpha = currentState.alpha,
                    layerId = currentState.activeLayerId
                )
                val action = DrawingAction.AddText(text)
                _state.value = currentState.copy(
                    texts = currentState.texts + text,
                    undoStack = currentState.undoStack + action,
                    redoStack = emptyList(),
                    showTextEditor = true
                )
                return
            }
            currentState.selectedTool in listOf(Tool.LINE, Tool.RECTANGLE, Tool.CIRCLE, Tool.TRIANGLE) -> {
                val shape = ShapePath(
                    start = adjustedPosition,
                    end = adjustedPosition,
                    color = currentState.selectedColor,
                    strokeWidth = currentState.strokeWidth,
                    shape = when (currentState.selectedTool) {
                        Tool.LINE -> Shape.LINE
                        Tool.RECTANGLE -> Shape.RECTANGLE
                        Tool.CIRCLE -> Shape.CIRCLE
                        Tool.TRIANGLE -> Shape.TRIANGLE
                        else -> Shape.LINE
                    },
                    alpha = currentState.alpha,
                    layerId = currentState.activeLayerId
                )
                _state.value = currentState.copy(currentShape = shape)
            }
            else -> {
                val effectiveWidth = if (currentState.pressureEnabled) {
                    currentState.strokeWidth * pressure
                } else {
                    currentState.strokeWidth
                }

                val path = DrawPath(
                    points = listOf(adjustedPosition),
                    pressures = listOf(pressure),
                    color = if (currentState.selectedTool == Tool.ERASER) currentState.canvasBackground else currentState.selectedColor,
                    strokeWidth = effectiveWidth,
                    tool = currentState.selectedTool,
                    alpha = currentState.alpha,
                    layerId = currentState.activeLayerId,
                    usePressure = currentState.pressureEnabled
                )
                _state.value = currentState.copy(currentPath = path)
            }
        }
    }

    fun onTouchMove(position: Offset, pressure: Float = 0.5f) {
        val currentState = _state.value
        if (currentState.isPanning) {
            _state.value = currentState.copy(panOffset = currentState.panOffset + position)
            return
        }

        val adjustedPosition = if (currentState.snapToGrid) {
            snapToGrid(position, currentState.gridSize)
        } else {
            position
        }

        pendingPressures.add(pressure)

        when {
            currentState.currentShape != null -> {
                _state.value = currentState.copy(
                    currentShape = currentState.currentShape.copy(end = adjustedPosition)
                )
            }
            currentState.currentPath != null -> {
                val points = currentState.currentPath.points + adjustedPosition
                val pressures = currentState.currentPath.pressures + pressure
                val effectiveWidth = if (currentState.pressureEnabled) {
                    currentState.strokeWidth * pressure
                } else {
                    currentState.strokeWidth
                }
                _state.value = currentState.copy(
                    currentPath = currentState.currentPath.copy(
                        points = points,
                        pressures = pressures,
                        strokeWidth = effectiveWidth
                    )
                )

                if (currentState.pressureEnabled && points.size >= 2) {
                    renderer.renderStrokeIncremental(
                        layerId = currentState.activeLayerId,
                        point = adjustedPosition,
                        pressure = pressure,
                        color = currentState.currentPath.color,
                        strokeWidth = currentState.strokeWidth,
                        tool = currentState.currentPath.tool,
                        alpha = currentState.alpha
                    )
                }
            }
        }
    }

    fun onTouchEnd() {
        val currentState = _state.value

        if (currentState.isPanning) {
            _state.value = currentState.copy(isPanning = false)
            return
        }

        currentState.currentPath?.let { path ->
            if (path.points.size > 1) {
                if (currentState.pressureEnabled) {
                    renderer.renderStroke(
                        layerId = path.layerId,
                        points = path.points,
                        pressures = path.pressures,
                        color = path.color,
                        strokeWidth = currentState.strokeWidth,
                        tool = path.tool,
                        alpha = path.alpha
                    )
                }

                val action = DrawingAction.AddPath(path)
                _state.value = currentState.copy(
                    paths = currentState.paths + path,
                    currentPath = null,
                    undoStack = currentState.undoStack + action,
                    redoStack = emptyList()
                )
            } else {
                _state.value = currentState.copy(currentPath = null)
            }
        }
        currentState.currentShape?.let { shape ->
            val action = DrawingAction.AddShape(shape)
            _state.value = _state.value.copy(
                shapes = _state.value.shapes + shape,
                currentShape = null,
                undoStack = _state.value.undoStack + action,
                redoStack = emptyList()
            )
        }

        pendingPressures.clear()
        updateBitmapPoolStats()
    }

    fun undo() {
        val currentState = _state.value
        if (currentState.undoStack.isEmpty()) return

        val lastAction = currentState.undoStack.last()
        val newUndoStack = currentState.undoStack.dropLast(1)

        when (lastAction) {
            is DrawingAction.AddPath -> {
                _state.value = currentState.copy(
                    paths = currentState.paths.dropLast(1),
                    undoStack = newUndoStack,
                    redoStack = currentState.redoStack + lastAction
                )
            }
            is DrawingAction.AddShape -> {
                _state.value = currentState.copy(
                    shapes = currentState.shapes.dropLast(1),
                    undoStack = newUndoStack,
                    redoStack = currentState.redoStack + lastAction
                )
            }
            is DrawingAction.AddText -> {
                _state.value = currentState.copy(
                    texts = currentState.texts.dropLast(1),
                    undoStack = newUndoStack,
                    redoStack = currentState.redoStack + lastAction
                )
            }
            is DrawingAction.AddImage -> {
                _state.value = currentState.copy(
                    images = currentState.images.dropLast(1),
                    undoStack = newUndoStack,
                    redoStack = currentState.redoStack + lastAction
                )
            }
            else -> {}
        }
    }

    fun redo() {
        val currentState = _state.value
        if (currentState.redoStack.isEmpty()) return

        val lastAction = currentState.redoStack.last()
        val newRedoStack = currentState.redoStack.dropLast(1)

        when (lastAction) {
            is DrawingAction.AddPath -> {
                _state.value = currentState.copy(
                    paths = currentState.paths + lastAction.path,
                    undoStack = currentState.undoStack + lastAction,
                    redoStack = newRedoStack
                )
            }
            is DrawingAction.AddShape -> {
                _state.value = currentState.copy(
                    shapes = currentState.shapes + lastAction.shape,
                    undoStack = currentState.undoStack + lastAction,
                    redoStack = newRedoStack
                )
            }
            is DrawingAction.AddText -> {
                _state.value = currentState.copy(
                    texts = currentState.texts + lastAction.text,
                    undoStack = currentState.undoStack + lastAction,
                    redoStack = newRedoStack
                )
            }
            is DrawingAction.AddImage -> {
                _state.value = currentState.copy(
                    images = currentState.images + lastAction.image,
                    undoStack = currentState.undoStack + lastAction,
                    redoStack = newRedoStack
                )
            }
            else -> {}
        }
    }

    fun clearCanvas() {
        val currentState = _state.value
        if (currentState.paths.isEmpty() && currentState.shapes.isEmpty() &&
            currentState.texts.isEmpty() && currentState.images.isEmpty()) return

        val action = DrawingAction.ClearCanvas(
            currentState.paths, currentState.shapes, currentState.texts, currentState.images
        )
        _state.value = currentState.copy(
            paths = emptyList(),
            shapes = emptyList(),
            texts = emptyList(),
            images = emptyList(),
            undoStack = currentState.undoStack + action,
            redoStack = emptyList()
        )
    }

    fun toggleColorPicker() {
        _state.value = _state.value.copy(
            showColorPicker = !_state.value.showColorPicker,
            showToolSettings = false, showLayers = false,
            showTextEditor = false, showFilters = false, showHistory = false
        )
    }

    fun toggleToolSettings() {
        _state.value = _state.value.copy(
            showToolSettings = !_state.value.showToolSettings,
            showColorPicker = false, showLayers = false,
            showTextEditor = false, showFilters = false, showHistory = false
        )
    }

    fun toggleLayers() {
        _state.value = _state.value.copy(
            showLayers = !_state.value.showLayers,
            showColorPicker = false, showToolSettings = false,
            showTextEditor = false, showFilters = false, showHistory = false
        )
    }

    fun toggleTextEditor() {
        _state.value = _state.value.copy(
            showTextEditor = !_state.value.showTextEditor,
            showColorPicker = false, showToolSettings = false,
            showLayers = false, showFilters = false, showHistory = false
        )
    }

    fun toggleFilters() {
        _state.value = _state.value.copy(
            showFilters = !_state.value.showFilters,
            showColorPicker = false, showToolSettings = false,
            showLayers = false, showTextEditor = false, showHistory = false
        )
    }

    fun toggleHistory() {
        _state.value = _state.value.copy(
            showHistory = !_state.value.showHistory,
            showColorPicker = false, showToolSettings = false,
            showLayers = false, showTextEditor = false, showFilters = false
        )
    }

    fun dismissAllPanels() {
        _state.value = _state.value.copy(
            showColorPicker = false, showToolSettings = false,
            showLayers = false, showTextEditor = false,
            showFilters = false, showHistory = false
        )
    }

    fun addLayer() {
        val currentState = _state.value
        if (currentState.layers.size >= 100) return

        val width = currentState.canvasSize.width.toInt()
        val height = currentState.canvasSize.height.toInt()
        val newLayerId = UUID.randomUUID().toString()
        val newLayer = Layer(id = newLayerId, name = "图层 ${currentState.layers.size + 1}")

        layerManager.createLayer(newLayerId, width, height)

        val action = DrawingAction.AddLayer(newLayer)
        _state.value = currentState.copy(
            layers = currentState.layers + newLayer,
            activeLayerId = newLayerId,
            layerCount = currentState.layerCount + 1,
            undoStack = currentState.undoStack + action
        )
        updateBitmapPoolStats()
    }

    fun removeLayer(layerId: String) {
        val currentState = _state.value
        if (currentState.layers.size <= 1) return

        layerManager.removeLayer(layerId)

        val action = DrawingAction.RemoveLayer(layerId)
        _state.value = currentState.copy(
            layers = currentState.layers.filter { it.id != layerId },
            activeLayerId = currentState.layers.firstOrNull { it.id != layerId }?.id ?: "default",
            paths = currentState.paths.filter { it.layerId != layerId },
            shapes = currentState.shapes.filter { it.layerId != layerId },
            texts = currentState.texts.filter { it.layerId != layerId },
            images = currentState.images.filter { it.layerId != layerId },
            layerCount = currentState.layerCount - 1,
            undoStack = currentState.undoStack + action
        )
        updateBitmapPoolStats()
    }

    fun toggleLayerVisibility(layerId: String) {
        val currentState = _state.value
        val action = DrawingAction.ToggleLayerVisibility(layerId)
        _state.value = currentState.copy(
            layers = currentState.layers.map {
                if (it.id == layerId) it.copy(isVisible = !it.isVisible) else it
            },
            undoStack = currentState.undoStack + action
        )
    }

    fun setActiveLayer(layerId: String) {
        _state.value = _state.value.copy(activeLayerId = layerId)
    }

    fun onTextChanged(text: String) {
        _state.value = _state.value.copy(currentText = text)
    }

    fun onTextFontSizeChanged(size: Float) {
        _state.value = _state.value.copy(textFontSize = size)
    }

    fun onTextFontFamilyChanged(family: String) {
        _state.value = _state.value.copy(textFontFamily = family)
    }

    fun toggleTextBold() {
        _state.value = _state.value.copy(textBold = !_state.value.textBold)
    }

    fun toggleTextItalic() {
        _state.value = _state.value.copy(textItalic = !_state.value.textItalic)
    }

    fun onImageAdded(uri: Uri, position: Offset, size: Size) {
        val currentState = _state.value
        val image = ImageElement(
            uri = uri, position = position, size = size,
            alpha = currentState.alpha, layerId = currentState.activeLayerId
        )
        val action = DrawingAction.AddImage(image)
        _state.value = currentState.copy(
            images = currentState.images + image,
            undoStack = currentState.undoStack + action,
            redoStack = emptyList()
        )
    }

    fun onFilterChanged(type: FilterType, intensity: Float) {
        val currentState = _state.value
        val updatedFilters = currentState.filters.toMutableList()
        val existingIndex = updatedFilters.indexOfFirst { it.type == type }
        if (existingIndex >= 0) {
            updatedFilters[existingIndex] = Filter(type, intensity)
        } else {
            updatedFilters.add(Filter(type, intensity))
        }
        _state.value = currentState.copy(filters = updatedFilters)
    }

    fun applyTemplate(templateIndex: Int) {
        val currentState = _state.value
        val templateSizes = listOf(
            Size(1080f, 1920f), Size(1080f, 1080f), Size(1080f, 1920f),
            Size(1280f, 720f), Size(2480f, 3508f), Size(1050f, 590f),
            Size(2880f, 4320f), Size(320f, 320f)
        )
        if (templateIndex in templateSizes.indices) {
            _state.value = currentState.copy(
                canvasSize = templateSizes[templateIndex],
                showTemplates = false
            )
        }
    }

    fun setCanvasBackground(color: Color) {
        _state.value = _state.value.copy(canvasBackground = color)
    }

    private fun snapToGrid(position: Offset, gridSize: Float): Offset {
        return Offset(
            x = (position.x / gridSize).toInt() * gridSize,
            y = (position.y / gridSize).toInt() * gridSize
        )
    }

    private fun updateBitmapPoolStats() {
        val poolStats = bitmapPool.getStats()
        _state.value = _state.value.copy(
            bitmapPoolStats = BitmapPoolStats(
                poolSize = poolStats.poolSize,
                hitRate = poolStats.hitRate,
                totalLayers = layerManager.getLayerCount()
            )
        )
    }

    fun saveToGallery(context: Context, bitmap: Bitmap): Boolean {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "MiMoDraw_${System.currentTimeMillis()}.png")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MiMoDraw")
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun onCleared() {
        super.onCleared()
        layerManager.clear()
        bitmapPool.clear()
    }
}
