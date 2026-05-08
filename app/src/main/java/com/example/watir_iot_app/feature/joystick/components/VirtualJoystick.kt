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

@Preview
@Composable fun VirtualJoystick(){
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }
    Canvas(modifier =
        Modifier
            .size(300.dp)
            .pointerInput(Unit){
                detectDragGestures(
                    onDragEnd = { thumbOffset = Offset.Zero}
                ){
                    change, dragAmount->
                    change.consume()
                    val newOffset = thumbOffset + dragAmount
                    val distance = newOffset.getDistance()
                    val maxRadius = 150f
                    if(distance <= maxRadius){
                        thumbOffset = newOffset
                    }
                    else{
                        thumbOffset = newOffset / distance * maxRadius
                    }
                }
            }) {
        drawCircle(
            color = Color.LightGray,
            radius = 150f
        )
        drawCircle(
            color = Color.Blue,
            radius = 50f,
            center = center + thumbOffset
        )
    }
}
