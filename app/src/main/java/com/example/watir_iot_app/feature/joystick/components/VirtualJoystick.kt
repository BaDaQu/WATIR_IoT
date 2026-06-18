package com.example.watir_iot_app.feature.joystick.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp

@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    onMove: (xPercent: Float, yPercent: Float) -> Unit,
    onStop: () -> Unit
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    var maxRadius by remember { mutableStateOf(1f) }
    val watirNavy = Color(0xFF102A43)
    val watirBlue = Color(0xFF2EB4E6)

    Canvas(
        modifier = modifier
            .size(240.dp)
            .onSizeChanged { size ->
                maxRadius = (size.width.coerceAtMost(size.height) / 2f)
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        onStop()
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val newOffset = thumbOffset + dragAmount
                    val distance = newOffset.getDistance()
                    if (distance <= maxRadius) {
                        thumbOffset = newOffset
                    } else {
                        thumbOffset = newOffset / distance * maxRadius
                    }
                    val xPercent = thumbOffset.x / maxRadius
                    val yPercent = thumbOffset.y / maxRadius

                    onMove(xPercent, yPercent)
                }
            }
    ) {
        drawCircle(
            color = watirNavy.copy(alpha = 0.05f),
            radius = maxRadius
        )
        drawCircle(
            color = watirNavy.copy(alpha = 0.2f),
            radius = maxRadius,
            style = Stroke(width = 4f)
        )
        drawCircle(
            color = watirBlue,
            radius = maxRadius * 0.35f,
            center = center + thumbOffset
        )
    }
}