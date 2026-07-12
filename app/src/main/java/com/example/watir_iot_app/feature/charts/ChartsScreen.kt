package com.example.watir_iot_app.feature.charts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watir_iot_app.R
import com.example.watir_iot_app.viewmodel.WatirViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.compose.component.textComponent
import com.patrykandpatrick.vico.compose.dimensions.dimensionsOf
import com.patrykandpatrick.vico.core.chart.layout.HorizontalLayout
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.example.watir_iot_app.data.model.TelemetryDbRow
import com.patrykandpatrick.vico.core.entry.ChartEntryModel
import com.patrykandpatrick.vico.core.entry.FloatEntry

@Composable
fun ChartsScreen(viewModel: WatirViewModel) {
    val historyData = viewModel.telemetryHistory.value.data.reversed()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.charts_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp),
            color = MaterialTheme.colorScheme.onBackground
        )
        
        val chartModel = prepareChartModel(historyData)
        if (historyData.isNotEmpty() && chartModel != null) {
            val tempColor = Color(0xFFE91E63)
            val humidityColor = Color(0xFF2196F3)
            val bottomAxisFormatter = com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter<com.patrykandpatrick.vico.core.axis.AxisPosition.Horizontal.Bottom> { value, _ ->
                value.toInt().toString()
            }

            val legend = com.patrykandpatrick.vico.compose.legend.verticalLegend(
                items = listOf(
                    com.patrykandpatrick.vico.compose.legend.legendItem(
                        icon = com.patrykandpatrick.vico.compose.component.shapeComponent(
                            com.patrykandpatrick.vico.core.component.shape.Shapes.pillShape,
                            tempColor
                        ),
                        label = textComponent(color = MaterialTheme.colorScheme.onSurface),
                        labelText = stringResource(R.string.charts_temp_label)
                    ),
                    com.patrykandpatrick.vico.compose.legend.legendItem(
                        icon = com.patrykandpatrick.vico.compose.component.shapeComponent(
                            com.patrykandpatrick.vico.core.component.shape.Shapes.pillShape,
                            humidityColor
                        ),
                        label = textComponent(color = MaterialTheme.colorScheme.onSurface),
                        labelText = stringResource(R.string.charts_hum_label)
                    ),
                    com.patrykandpatrick.vico.compose.legend.legendItem(
                        icon = com.patrykandpatrick.vico.compose.component.shapeComponent(
                            com.patrykandpatrick.vico.core.component.shape.Shapes.pillShape,
                            Color.Blue
                        ),
                        label = textComponent(color = MaterialTheme.colorScheme.onSurface),
                        labelText = stringResource(R.string.charts_watering_label)
                    )
                ),
                iconSize = 8.dp,
                iconPadding = 8.dp,
                spacing = 4.dp,
                padding = dimensionsOf(16.dp)
            )

            val lineSpecs = mutableListOf(
                com.patrykandpatrick.vico.compose.chart.line.lineSpec(lineColor = tempColor),
                com.patrykandpatrick.vico.compose.chart.line.lineSpec(lineColor = humidityColor)
            )

            if (historyData.any { it.pump_active }) {
                lineSpecs.add(
                    com.patrykandpatrick.vico.compose.chart.line.lineSpec(
                        lineColor = Color.Transparent,
                        pointSize = 12.dp,
                        point = com.patrykandpatrick.vico.compose.component.shapeComponent(
                            shape = com.patrykandpatrick.vico.core.component.shape.Shapes.pillShape,
                            color = Color.Blue
                        )
                    )
                )
            }

            // Tworzymy wykres i ustawiamy pointPosition na Start, aby przylegał do osi Y
            val chart = lineChart(lines = lineSpecs)
            Chart(
                chart = chart,
                model = chartModel,
                startAxis = rememberStartAxis(
                    label = textComponent(
                        color = MaterialTheme.colorScheme.onSurface,
                        textSize = 10.sp
                    ),
                    axis = lineComponent(color = MaterialTheme.colorScheme.outline),
                    guideline = lineComponent(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        thickness = 1.dp
                    ),
                    titleComponent = textComponent(
                        color = MaterialTheme.colorScheme.onSurface,
                        padding = dimensionsOf(horizontal = 4.dp)
                    ),
                    title = stringResource(R.string.charts_value_axis)
                ),
                bottomAxis = rememberBottomAxis(
                    label = textComponent(
                        color = MaterialTheme.colorScheme.onSurface,
                        textSize = 10.sp
                    ),
                    axis = lineComponent(color = MaterialTheme.colorScheme.outline),
                    guideline = lineComponent(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        thickness = 1.dp
                    ),
                    valueFormatter = bottomAxisFormatter,
                    titleComponent = textComponent(
                        color = MaterialTheme.colorScheme.onSurface,
                        padding = dimensionsOf(vertical = 4.dp)
                    ),
                    title = stringResource(R.string.charts_time_axis)
                ),
                legend = legend,
                modifier = Modifier.weight(1f),
                horizontalLayout = HorizontalLayout.FullWidth()
            )
        } else {
            Text(
                text = stringResource(R.string.charts_no_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

private fun prepareChartModel(data: List<TelemetryDbRow>): ChartEntryModel? {
    if (data.isEmpty()) return null

    val temperatureEntries = data.mapIndexed { index, row ->
        FloatEntry(x = index.toFloat(), y = row.temp)
    }

    val humidityEntries = data.mapIndexed { index, row ->
        FloatEntry(x = index.toFloat(), y = row.humidity.toFloat())
    }
    val pumpEntries = data.mapIndexedNotNull { index, row ->
        if (row.pump_active) {
            FloatEntry(x = index.toFloat(), y = row.humidity.toFloat())
        } else null
    }

    return if (pumpEntries.isNotEmpty()) {
        entryModelOf(temperatureEntries, humidityEntries, pumpEntries)
    } else {
        entryModelOf(temperatureEntries, humidityEntries)
    }
}
