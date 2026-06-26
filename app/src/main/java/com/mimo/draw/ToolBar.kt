package com.mimo.draw

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api

@Composable
fun TopBar(
    state: DrawingState,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleSymmetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF1A1A2E),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MiMoDraw",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onToggleGrid,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_tool_grid),
                        contentDescription = "Grid",
                        tint = if (state.showGrid) Color(0xFF4CAF50) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onToggleSymmetry,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_tool_symmetry),
                        contentDescription = "Symmetry",
                        tint = if (state.isSymmetryEnabled) Color(0xFF4CAF50) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onUndo,
                    enabled = state.undoStack.isNotEmpty(),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (state.undoStack.isNotEmpty()) Color.White else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onRedo,
                    enabled = state.redoStack.isNotEmpty(),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (state.redoStack.isNotEmpty()) Color.White else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Clear",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onSave,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.Save,
                        contentDescription = "Save",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BottomToolBar(
    state: DrawingState,
    onToolSelected: (Tool) -> Unit,
    onColorPickerToggle: () -> Unit,
    onToolSettingsToggle: () -> Unit,
    onLayersToggle: () -> Unit,
    onFiltersToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF1A1A2E),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 4.dp)
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item {
                    ToolButton(
                        icon = painterResource(id = R.drawable.ic_tool_pen),
                        label = "Pen",
                        isSelected = state.selectedTool == Tool.PEN,
                        onClick = { onToolSelected(Tool.PEN) }
                    )
                }
                item {
                    ToolButton(
                        icon = painterResource(id = R.drawable.ic_tool_brush),
                        label = "Brush",
                        isSelected = state.selectedTool == Tool.BRUSH,
                        onClick = { onToolSelected(Tool.BRUSH) }
                    )
                }
                item {
                    ToolButton(
                        icon = painterResource(id = R.drawable.ic_tool_calligraphy),
                        label = "Callig",
                        isSelected = state.selectedTool == Tool.CALLIGRAPHY,
                        onClick = { onToolSelected(Tool.CALLIGRAPHY) }
                    )
                }
                item {
                    ToolButton(
                        icon = painterResource(id = R.drawable.ic_tool_spray),
                        label = "Spray",
                        isSelected = state.selectedTool == Tool.SPRAY,
                        onClick = { onToolSelected(Tool.SPRAY) }
                    )
                }
                item {
                    ToolButton(
                        icon = painterResource(id = R.drawable.ic_tool_eraser),
                        label = "Eraser",
                        isSelected = state.selectedTool == Tool.ERASER,
                        onClick = { onToolSelected(Tool.ERASER) }
                    )
                }
                item {
                    ToolButton(
                        icon = painterResource(id = R.drawable.ic_tool_blur),
                        label = "Blur",
                        isSelected = state.selectedTool == Tool.BLUR,
                        onClick = { onToolSelected(Tool.BLUR) }
                    )
                }
                item {
                    ToolButton(
                        icon = painterResource(id = R.drawable.ic_tool_eyedropper),
                        label = "Pick",
                        isSelected = state.selectedTool == Tool.EYEDROPPER,
                        onClick = { onToolSelected(Tool.EYEDROPPER) }
                    )
                }
                item {
                    ToolButton(
                        icon = painterResource(id = R.drawable.ic_tool_fill),
                        label = "Fill",
                        isSelected = state.selectedTool == Tool.FILL,
                        onClick = { onToolSelected(Tool.FILL) }
                    )
                }
                item {
                    ToolButton(
                        icon = painterResource(id = R.drawable.ic_tool_text),
                        label = "Text",
                        isSelected = state.selectedTool == Tool.TEXT,
                        onClick = { onToolSelected(Tool.TEXT) }
                    )
                }
                item {
                    Divider(
                        modifier = Modifier
                            .height(48.dp)
                            .width(1.dp)
                            .padding(vertical = 8.dp),
                        color = Color.Gray
                    )
                }
                item {
                    ToolButton(
                        icon = painterResource(id = R.drawable.ic_tool_line),
                        label = "Line",
                        isSelected = state.selectedTool == Tool.LINE,
                        onClick = { onToolSelected(Tool.LINE) }
                    )
                }
                item {
                    ToolButton(
                        icon = painterResource(id = R.drawable.ic_tool_rectangle),
                        label = "Rect",
                        isSelected = state.selectedTool == Tool.RECTANGLE,
                        onClick = { onToolSelected(Tool.RECTANGLE) }
                    )
                }
                item {
                    ToolButton(
                        icon = painterResource(id = R.drawable.ic_tool_circle),
                        label = "Circle",
                        isSelected = state.selectedTool == Tool.CIRCLE,
                        onClick = { onToolSelected(Tool.CIRCLE) }
                    )
                }
                item {
                    ToolButton(
                        icon = painterResource(id = R.drawable.ic_tool_triangle),
                        label = "Triangle",
                        isSelected = state.selectedTool == Tool.TRIANGLE,
                        onClick = { onToolSelected(Tool.TRIANGLE) }
                    )
                }
                item {
                    ToolButton(
                        icon = painterResource(id = R.drawable.ic_tool_lasso),
                        label = "Lasso",
                        isSelected = state.selectedTool == Tool.LASSO,
                        onClick = { onToolSelected(Tool.LASSO) }
                    )
                }
                item {
                    ToolButton(
                        icon = painterResource(id = R.drawable.ic_tool_crop),
                        label = "Move",
                        isSelected = state.selectedTool == Tool.MOVE,
                        onClick = { onToolSelected(Tool.MOVE) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(state.selectedColor)
                        .border(2.dp, Color.White, CircleShape)
                        .clickable { onColorPickerToggle() }
                )

                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = "Size: ${state.strokeWidth.toInt()}px",
                        color = Color.White,
                        fontSize = 10.sp
                    )
                    Slider(
                        value = state.strokeWidth,
                        onValueChange = { },
                        valueRange = 1f..100f,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = state.selectedColor
                        )
                    )
                }

                IconButton(
                    onClick = onLayersToggle,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_tool_layer),
                        contentDescription = "Layers",
                        tint = if (state.showLayers) Color(0xFF4CAF50) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onFiltersToggle,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_tool_blur),
                        contentDescription = "Filters",
                        tint = if (state.showFilters) Color(0xFF4CAF50) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onToolSettingsToggle,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.Tune,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ToolButton(
    icon: androidx.compose.ui.graphics.painter.Painter,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .then(
                if (isSelected) {
                    Modifier.background(Color.White.copy(alpha = 0.2f))
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Icon(
            painter = icon,
            contentDescription = label,
            tint = if (isSelected) Color(0xFF4CAF50) else Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (isSelected) Color(0xFF4CAF50) else Color.White,
            fontSize = 8.sp
        )
    }
}

@Composable
fun ColorPickerPanel(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        color = Color(0xFF1A1A2E).copy(alpha = 0.95f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Color Picker",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(DEFAULT_COLORS) { color ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (selectedColor == color) 3.dp else 1.dp,
                                color = if (selectedColor == color) Color.White else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable { onColorSelected(color) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TOOL_COLORS) { color ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (selectedColor == color) 3.dp else 1.dp,
                                color = if (selectedColor == color) Color.White else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable { onColorSelected(color) }
                    )
                }
            }
        }
    }
}

@Composable
fun ToolSettingsPanel(
    state: DrawingState,
    onStrokeWidthChanged: (Float) -> Unit,
    onAlphaChanged: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        color = Color(0xFF1A1A2E).copy(alpha = 0.95f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tool Settings",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Stroke Width: ${state.strokeWidth.toInt()}px",
                color = Color.White,
                fontSize = 14.sp
            )
            Slider(
                value = state.strokeWidth,
                onValueChange = onStrokeWidthChanged,
                valueRange = 1f..100f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = state.selectedColor
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Opacity: ${(state.alpha * 100).toInt()}%",
                color = Color.White,
                fontSize = 14.sp
            )
            Slider(
                value = state.alpha,
                onValueChange = onAlphaChanged,
                valueRange = 0.1f..1f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = state.selectedColor
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Preview",
                color = Color.White,
                fontSize = 14.sp
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = (state.strokeWidth * 4).dp, height = state.strokeWidth.dp)
                        .clip(RoundedCornerShape(state.strokeWidth.dp / 2))
                        .background(state.selectedColor.copy(alpha = state.alpha))
                )
            }
        }
    }
}

@Composable
fun LayersPanel(
    state: DrawingState,
    onAddLayer: () -> Unit,
    onRemoveLayer: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onSetActiveLayer: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        color = Color(0xFF1A1A2E).copy(alpha = 0.95f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Layers",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(onClick = onAddLayer) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add Layer",
                            tint = Color(0xFF4CAF50)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            state.layers.reversed().forEach { layer ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (layer.id == state.activeLayerId) {
                                Color(0xFF4CAF50).copy(alpha = 0.3f)
                            } else {
                                Color.Transparent
                            }
                        )
                        .clickable { onSetActiveLayer(layer.id) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onToggleVisibility(layer.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (layer.isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = "Toggle Visibility",
                            tint = if (layer.isVisible) Color.White else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = layer.name,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )

                    if (state.layers.size > 1) {
                        IconButton(
                            onClick = { onRemoveLayer(layer.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete Layer",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FiltersPanel(
    state: DrawingState,
    onFilterChanged: (FilterType, Float) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        color = Color(0xFF1A1A2E).copy(alpha = 0.95f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .navigationBarsPadding()
                .heightIn(max = 400.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filters",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            FILTER_PRESETS.forEach { filter ->
                val currentValue = state.filters.find { it.type == filter.type }?.intensity ?: 0f

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = filter.type.name.replace("_", " "),
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Slider(
                        value = currentValue,
                        onValueChange = { onFilterChanged(filter.type, it) },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFF4CAF50)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryPanel(
    state: DrawingState,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        color = Color(0xFF1A1A2E).copy(alpha = 0.95f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "History",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Total actions: ${state.undoStack.size}",
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            state.undoStack.takeLast(10).forEachIndexed { index, action ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        when (action) {
                            is DrawingAction.AddPath -> Icons.Filled.Brush
                            is DrawingAction.AddShape -> Icons.Filled.Rectangle
                            is DrawingAction.AddText -> Icons.Filled.TextFields
                            is DrawingAction.AddImage -> Icons.Filled.Image
                            is DrawingAction.ClearCanvas -> Icons.Filled.Delete
                            is DrawingAction.AddLayer -> Icons.Filled.Layers
                            is DrawingAction.RemoveLayer -> Icons.Filled.Delete
                            is DrawingAction.ToggleLayerVisibility -> Icons.Filled.Visibility
                            else -> Icons.Filled.Edit
                        },
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (action) {
                            is DrawingAction.AddPath -> "Drew with ${action.path.tool.name}"
                            is DrawingAction.AddShape -> "Added ${action.shape.shape.name}"
                            is DrawingAction.AddText -> "Added text"
                            is DrawingAction.AddImage -> "Added image"
                            is DrawingAction.ClearCanvas -> "Cleared canvas"
                            is DrawingAction.AddLayer -> "Added layer"
                            is DrawingAction.RemoveLayer -> "Removed layer"
                            is DrawingAction.ToggleLayerVisibility -> "Toggled layer"
                            else -> "Action"
                        },
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorPanel(
    state: DrawingState,
    onTextChanged: (String) -> Unit,
    onFontSizeChanged: (Float) -> Unit,
    onFontFamilyChanged: (String) -> Unit,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        color = Color(0xFF1A1A2E).copy(alpha = 0.95f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Text Editor",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.currentText,
                onValueChange = onTextChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Enter text") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF4CAF50),
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.textBold,
                    onClick = onToggleBold,
                    label = { Text("Bold") }
                )
                FilterChip(
                    selected = state.textItalic,
                    onClick = onToggleItalic,
                    label = { Text("Italic") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Font Size: ${state.textFontSize.toInt()}sp",
                color = Color.White,
                fontSize = 14.sp
            )
            Slider(
                value = state.textFontSize,
                onValueChange = onFontSizeChanged,
                valueRange = 8f..72f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFF4CAF50)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Font Family",
                color = Color.White,
                fontSize = 14.sp
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(FONT_FAMILIES) { family ->
                    FilterChip(
                        selected = state.textFontFamily == family,
                        onClick = { onFontFamilyChanged(family) },
                        label = { Text(family) }
                    )
                }
            }
        }
    }
}
