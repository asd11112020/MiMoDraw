package com.mimo.draw.ui

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mimo.draw.DrawingState
import com.mimo.draw.Layer

@Composable
fun LayerThumbnails(
    state: DrawingState,
    layerBitmaps: Map<String, Bitmap>,
    onLayerSelected: (String) -> Unit,
    onLayerVisibilityToggle: (String) -> Unit,
    onLayerDelete: (String) -> Unit,
    onAddLayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A2E))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("图层", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onAddLayer, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "新建", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(state.layers.reversed()) { index, layer ->
                LayerThumbnailItem(
                    layer = layer,
                    thumbnail = layerBitmaps[layer.id],
                    isActive = layer.id == state.activeLayerId,
                    onClick = { onLayerSelected(layer.id) },
                    onVisibilityToggle = { onLayerVisibilityToggle(layer.id) },
                    onDelete = { onLayerDelete(layer.id) },
                    canDelete = state.layers.size > 1
                )
            }
        }
    }
}

@Composable
fun LayerThumbnailItem(
    layer: Layer,
    thumbnail: Bitmap?,
    isActive: Boolean,
    onClick: () -> Unit,
    onVisibilityToggle: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    val borderWidth = if (isActive) 2.dp else 1.dp
    val borderColor = if (isActive) Color(0xFF4CAF50) else Color(0xFF444444)
    val bgColor = if (isActive) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color(0xFF2A2A3E)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp, 42.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFF333333), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail.asImageBitmap(),
                    contentDescription = layer.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Filled.Layers,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = layer.name,
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = onVisibilityToggle,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                if (layer.isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                contentDescription = "可见性",
                tint = if (layer.isVisible) Color.White else Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }

        if (canDelete) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "删除",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

fun createLayerThumbnail(bitmap: Bitmap?, width: Int = 280, height: Int = 210): Bitmap {
    val thumbnail = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(thumbnail)
    canvas.drawColor(android.graphics.Color.WHITE)

    if (bitmap != null) {
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        val scale = minOf(width.toFloat() / bitmap.width, height.toFloat() / bitmap.height)
        val scaledWidth = (bitmap.width * scale).toInt()
        val scaledHeight = (bitmap.height * scale).toInt()
        val left = (width - scaledWidth) / 2
        val top = (height - scaledHeight) / 2
        canvas.drawBitmap(bitmap, null, RectF(left.toFloat(), top.toFloat(), (left + scaledWidth).toFloat(), (top + scaledHeight).toFloat()), paint)
    }

    return thumbnail
}
