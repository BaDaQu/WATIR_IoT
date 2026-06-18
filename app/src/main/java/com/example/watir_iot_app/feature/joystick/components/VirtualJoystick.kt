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
    var maxRadius by remember { mutableStateOf(1f) } // Obliczany dynamicznie!

    // Kolory z logo WATIR
    val watirNavy = Color(0xFF102A43)
    val watirBlue = Color(0xFF2EB4E6)

    Canvas(
        modifier = modifier
            .size(240.dp) // Domyślny rozmiar (zostanie nadpisany, jeśli podano inny z zewnątrz)
            .onSizeChanged { size ->
                // Super ważna poprawka: dynamiczny promień dostosowany do każdego ekranu!
                maxRadius = (size.width.coerceAtMost(size.height) / 2f)
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        onStop() // UWAGA: Naprawiony błąd! Teraz silniki na pewno się zatrzymają.
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val newOffset = thumbOffset + dragAmount
                    val distance = newOffset.getDistance()

                    // Trygonometria: nie pozwalamy kropce wyjść poza koło
                    if (distance <= maxRadius) {
                        thumbOffset = newOffset
                    } else {
                        thumbOffset = newOffset / distance * maxRadius
                    }

                    // Obliczamy wychylenie od -1.0 do 1.0
                    val xPercent = thumbOffset.x / maxRadius
                    val yPercent = thumbOffset.y / maxRadius

                    onMove(xPercent, yPercent)
                }
            }
    ) {
        // 1. Baza joysticka (delikatne granatowe tło)
        drawCircle(
            color = watirNavy.copy(alpha = 0.05f),
            radius = maxRadius
        )
        // 2. Obwódka bazy (wyraźniejsza)
        drawCircle(
            color = watirNavy.copy(alpha = 0.2f),
            radius = maxRadius,
            style = Stroke(width = 4f)
        )
        // 3. "Grzybek" (piękny błękitny kciuk do sterowania)
        drawCircle(
            color = watirBlue,
            radius = maxRadius * 0.35f, // Grzybek zajmuje zawsze proporcjonalnie 35% bazy
            center = center + thumbOffset
        )
    }
}