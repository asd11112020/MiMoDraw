package com.mimo.draw

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FILTER_CN = mapOf(
    "BLUR" to "模糊",
    "SHARPEN" to "锐化",
    "BRIGHTNESS" to "亮度",
    "CONTRAST" to "对比度",
    "SATURATION" to "饱和度",
    "HUE_ROTATE" to "色相旋转",
    "INVERT" to "反色",
    "GRAYSCALE" to "灰度",
    "SEPIA" to "复古",
    "VIGNETTE" to "暗角",
    "NOISE" to "噪点",
    "PIXELATE" to "像素化",
    "EMBOSS" to "浮雕"
)

private val FONT_CN = mapOf(
    "Default" to "默认",
    "Serif" to "衬线体",
    "Sans-Serif" to "无衬线",
    "Monospace" to "等宽",
    "Cursive" to "手写体"
)

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
                text = "妙笔生花",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onToggleGrid, modifier = Modifier.size(36.dp)) {
                    Icon(painterResource(id = R.drawable.ic_tool_grid), contentDescription = "网格",
                        tint = if (state.showGrid) Color(0xFF4CAF50) else Color.White, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onToggleSymmetry, modifier = Modifier.size(36.dp)) {
                    Icon(painterResource(id = R.drawable.ic_tool_symmetry), contentDescription = "对称",
                        tint = if (state.isSymmetryEnabled) Color(0xFF4CAF50) else Color.White, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onUndo, enabled = state.undoStack.isNotEmpty(), modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Undo, contentDescription = "撤销",
                        tint = if (state.undoStack.isNotEmpty()) Color.White else Color.Gray, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onRedo, enabled = state.redoStack.isNotEmpty(), modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Redo, contentDescription = "重做",
                        tint = if (state.redoStack.isNotEmpty()) Color.White else Color.Gray, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "清空", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onSave, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Save, contentDescription = "保存", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
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
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 4.dp)
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item { ToolButton(painterResource(id = R.drawable.ic_tool_pen), "钢笔", state.selectedTool == Tool.PEN) { onToolSelected(Tool.PEN) } }
                item { ToolButton(painterResource(id = R.drawable.ic_tool_brush), "毛笔", state.selectedTool == Tool.BRUSH) { onToolSelected(Tool.BRUSH) } }
                item { ToolButton(painterResource(id = R.drawable.ic_tool_calligraphy), "书法", state.selectedTool == Tool.CALLIGRAPHY) { onToolSelected(Tool.CALLIGRAPHY) } }
                item { ToolButton(painterResource(id = R.drawable.ic_tool_spray), "喷枪", state.selectedTool == Tool.SPRAY) { onToolSelected(Tool.SPRAY) } }
                item { ToolButton(painterResource(id = R.drawable.ic_tool_eraser), "橡皮", state.selectedTool == Tool.ERASER) { onToolSelected(Tool.ERASER) } }
                item { ToolButton(painterResource(id = R.drawable.ic_tool_blur), "模糊", state.selectedTool == Tool.BLUR) { onToolSelected(Tool.BLUR) } }
                item { ToolButton(painterResource(id = R.drawable.ic_tool_eyedropper), "取色", state.selectedTool == Tool.EYEDROPPER) { onToolSelected(Tool.EYEDROPPER) } }
                item { ToolButton(painterResource(id = R.drawable.ic_tool_fill), "填充", state.selectedTool == Tool.FILL) { onToolSelected(Tool.FILL) } }
                item { ToolButton(painterResource(id = R.drawable.ic_tool_text), "文字", state.selectedTool == Tool.TEXT) { onToolSelected(Tool.TEXT) } }
                item { Divider(modifier = Modifier.height(48.dp).width(1.dp).padding(vertical = 8.dp), color = Color.Gray) }
                item { ToolButton(painterResource(id = R.drawable.ic_tool_line), "直线", state.selectedTool == Tool.LINE) { onToolSelected(Tool.LINE) } }
                item { ToolButton(painterResource(id = R.drawable.ic_tool_rectangle), "矩形", state.selectedTool == Tool.RECTANGLE) { onToolSelected(Tool.RECTANGLE) } }
                item { ToolButton(painterResource(id = R.drawable.ic_tool_circle), "圆形", state.selectedTool == Tool.CIRCLE) { onToolSelected(Tool.CIRCLE) } }
                item { ToolButton(painterResource(id = R.drawable.ic_tool_triangle), "三角", state.selectedTool == Tool.TRIANGLE) { onToolSelected(Tool.TRIANGLE) } }
                item { ToolButton(painterResource(id = R.drawable.ic_tool_lasso), "套索", state.selectedTool == Tool.LASSO) { onToolSelected(Tool.LASSO) } }
                item { ToolButton(painterResource(id = R.drawable.ic_tool_crop), "移动", state.selectedTool == Tool.MOVE) { onToolSelected(Tool.MOVE) } }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(state.selectedColor)
                        .border(2.dp, Color.White, CircleShape).clickable { onColorPickerToggle() }
                )

                Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(text = "粗细: ${state.strokeWidth.toInt()}px", color = Color.White, fontSize = 10.sp)
                    Slider(
                        value = state.strokeWidth, onValueChange = {}, valueRange = 1f..100f,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = state.selectedColor)
                    )
                }

                IconButton(onClick = onLayersToggle, modifier = Modifier.size(36.dp)) {
                    Icon(painterResource(id = R.drawable.ic_tool_layer), contentDescription = "图层",
                        tint = if (state.showLayers) Color(0xFF4CAF50) else Color.White, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onFiltersToggle, modifier = Modifier.size(36.dp)) {
                    Icon(painterResource(id = R.drawable.ic_tool_blur), contentDescription = "滤镜",
                        tint = if (state.showFilters) Color(0xFF4CAF50) else Color.White, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onToolSettingsToggle, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Tune, contentDescription = "设置", tint = Color.White, modifier = Modifier.size(20.dp))
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
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onClick() }
            .then(if (isSelected) Modifier.background(Color.White.copy(alpha = 0.2f)) else Modifier)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Icon(painter = icon, contentDescription = label,
            tint = if (isSelected) Color(0xFF4CAF50) else Color.White, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, color = if (isSelected) Color(0xFF4CAF50) else Color.White, fontSize = 8.sp)
    }
}

@Composable
fun ColorPickerPanel(selectedColor: Color, onColorSelected: (Color) -> Unit, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        color = Color(0xFF1A1A2E).copy(alpha = 0.95f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("调色板", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "关闭", tint = Color.White) }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DEFAULT_COLORS) { color ->
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(color)
                        .border(if (selectedColor == color) 3.dp else 1.dp,
                            if (selectedColor == color) Color.White else Color.Gray, CircleShape)
                        .clickable { onColorSelected(color) })
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TOOL_COLORS) { color ->
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color)
                        .border(if (selectedColor == color) 3.dp else 1.dp,
                            if (selectedColor == color) Color.White else Color.Gray, CircleShape)
                        .clickable { onColorSelected(color) })
                }
            }
        }
    }
}

@Composable
fun ToolSettingsPanel(state: DrawingState, onStrokeWidthChanged: (Float) -> Unit, onAlphaChanged: (Float) -> Unit, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        color = Color(0xFF1A1A2E).copy(alpha = 0.95f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("工具设置", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "关闭", tint = Color.White) }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("笔刷粗细: ${state.strokeWidth.toInt()}px", color = Color.White, fontSize = 14.sp)
            Slider(value = state.strokeWidth, onValueChange = onStrokeWidthChanged, valueRange = 1f..100f,
                modifier = Modifier.fillMaxWidth(), colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = state.selectedColor))
            Spacer(modifier = Modifier.height(12.dp))
            Text("不透明度: ${(state.alpha * 100).toInt()}%", color = Color.White, fontSize = 14.sp)
            Slider(value = state.alpha, onValueChange = onAlphaChanged, valueRange = 0.1f..1f,
                modifier = Modifier.fillMaxWidth(), colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = state.selectedColor))
            Spacer(modifier = Modifier.height(12.dp))
            Text("预览", color = Color.White, fontSize = 14.sp)
            Box(modifier = Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(8.dp))
                .background(Color.White).padding(16.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(width = (state.strokeWidth * 4).dp, height = state.strokeWidth.dp)
                    .clip(RoundedCornerShape(state.strokeWidth.dp / 2)).background(state.selectedColor.copy(alpha = state.alpha)))
            }
        }
    }
}

@Composable
fun LayersPanel(state: DrawingState, onAddLayer: () -> Unit, onRemoveLayer: (String) -> Unit,
    onToggleVisibility: (String) -> Unit, onSetActiveLayer: (String) -> Unit, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        color = Color(0xFF1A1A2E).copy(alpha = 0.95f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("图层", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = onAddLayer) { Icon(Icons.Filled.Add, contentDescription = "新建图层", tint = Color(0xFF4CAF50)) }
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "关闭", tint = Color.White) }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            state.layers.reversed().forEach { layer ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(if (layer.id == state.activeLayerId) Color(0xFF4CAF50).copy(alpha = 0.3f) else Color.Transparent)
                        .clickable { onSetActiveLayer(layer.id) }.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onToggleVisibility(layer.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(if (layer.isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = "切换可见性", tint = if (layer.isVisible) Color.White else Color.Gray, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = layer.name, color = Color.White, modifier = Modifier.weight(1f))
                    if (state.layers.size > 1) {
                        IconButton(onClick = { onRemoveLayer(layer.id) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Delete, contentDescription = "删除图层", tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FiltersPanel(state: DrawingState, onFilterChanged: (FilterType, Float) -> Unit, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        color = Color(0xFF1A1A2E).copy(alpha = 0.95f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).navigationBarsPadding().heightIn(max = 400.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("滤镜", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "关闭", tint = Color.White) }
            }
            Spacer(modifier = Modifier.height(12.dp))
            FILTER_PRESETS.forEach { filter ->
                val currentValue = state.filters.find { it.type == filter.type }?.intensity ?: 0f
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(FILTER_CN[filter.type.name] ?: filter.type.name, color = Color.White, fontSize = 12.sp)
                    Slider(value = currentValue, onValueChange = { onFilterChanged(filter.type, it) }, valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth(), colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFF4CAF50)))
                }
            }
        }
    }
}

@Composable
fun HistoryPanel(state: DrawingState, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        color = Color(0xFF1A1A2E).copy(alpha = 0.95f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("历史记录", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "关闭", tint = Color.White) }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("共 ${state.undoStack.size} 步操作", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            state.undoStack.takeLast(10).forEach { action ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
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
                        contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (action) {
                            is DrawingAction.AddPath -> "用${action.path.tool.name}绘制"
                            is DrawingAction.AddShape -> "添加${action.shape.shape.name}"
                            is DrawingAction.AddText -> "添加文字"
                            is DrawingAction.AddImage -> "添加图片"
                            is DrawingAction.ClearCanvas -> "清空画布"
                            is DrawingAction.AddLayer -> "新建图层"
                            is DrawingAction.RemoveLayer -> "删除图层"
                            is DrawingAction.ToggleLayerVisibility -> "切换图层可见性"
                            else -> "操作"
                        },
                        color = Color.White, fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorPanel(state: DrawingState, onTextChanged: (String) -> Unit, onFontSizeChanged: (Float) -> Unit,
    onFontFamilyChanged: (String) -> Unit, onToggleBold: () -> Unit, onToggleItalic: () -> Unit, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        color = Color(0xFF1A1A2E).copy(alpha = 0.95f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("文字编辑", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "关闭", tint = Color.White) }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.currentText, onValueChange = onTextChanged, modifier = Modifier.fillMaxWidth(),
                label = { Text("输入文字") },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF4CAF50), unfocusedBorderColor = Color.Gray)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.textBold, onClick = onToggleBold, label = { Text("粗体") })
                FilterChip(selected = state.textItalic, onClick = onToggleItalic, label = { Text("斜体") })
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("字号: ${state.textFontSize.toInt()}sp", color = Color.White, fontSize = 14.sp)
            Slider(value = state.textFontSize, onValueChange = onFontSizeChanged, valueRange = 8f..72f,
                modifier = Modifier.fillMaxWidth(), colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color(0xFF4CAF50)))
            Spacer(modifier = Modifier.height(12.dp))
            Text("字体", color = Color.White, fontSize = 14.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FONT_FAMILIES) { family ->
                    FilterChip(selected = state.textFontFamily == family, onClick = { onFontFamilyChanged(family) },
                        label = { Text(FONT_CN[family] ?: family) })
                }
            }
        }
    }
}
