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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


@Composable
fun VirtualJoystick(
    onMove: (xPercent: Float, yPercent: Float) -> Unit,
    onStop: () -> Unit
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    val maxRadius = 150f

    Canvas(modifier =
        Modifier
            .size(300.dp)
            .pointerInput(Unit){
                detectDragGestures(
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                    }
                ){ change, dragAmount->

                    change.consume()
                    val newOffset = thumbOffset + dragAmount
                    val distance = newOffset.getDistance()

                    if(distance <= maxRadius){
                        thumbOffset = newOffset
                    }
                    else{
                        thumbOffset = newOffset / distance * maxRadius
                    }

                    // Obliczamy wychylenie od -1.0 do 1.0
                    val xPercent = thumbOffset.x / maxRadius
                    val yPercent = thumbOffset.y / maxRadius

                    onMove(xPercent, yPercent)
                }
            }) {
        drawCircle(
            color = Color.LightGray,
            radius = maxRadius
        )
        drawCircle(
            color = Color.Blue,
            radius = 50f,
            center = center + thumbOffset
        )
    }
}
