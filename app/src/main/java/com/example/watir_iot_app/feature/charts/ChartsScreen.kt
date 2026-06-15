package com.example.watir_iot_app.feature.charts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.watir_iot_app.viewmodel.WatirViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
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
        Text("Wykres Telemetryczny")
        val chartModel = prepareChartModel(historyData)
        if (historyData.isNotEmpty() && chartModel != null) {
            val tempColor = androidx.compose.ui.graphics.Color(0xFFE91E63)
            val humidityColor = androidx.compose.ui.graphics.Color(0xFF2196F3)
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
                        label = com.patrykandpatrick.vico.compose.component.textComponent(
                            color = androidx.compose.ui.graphics.Color.Black
                        ),
                        labelText = "Temperatura (°C)"
                    ),
                    com.patrykandpatrick.vico.compose.legend.legendItem(
                        icon = com.patrykandpatrick.vico.compose.component.shapeComponent(
                            com.patrykandpatrick.vico.core.component.shape.Shapes.pillShape,
                            humidityColor
                        ),
                        label = com.patrykandpatrick.vico.compose.component.textComponent(
                            color = androidx.compose.ui.graphics.Color.Black
                        ),
                        labelText = "Wilgotność (%)"
                    ),
                    com.patrykandpatrick.vico.compose.legend.legendItem(
                        icon = com.patrykandpatrick.vico.compose.component.shapeComponent(
                            com.patrykandpatrick.vico.core.component.shape.Shapes.pillShape,
                            androidx.compose.ui.graphics.Color.Blue
                        ),
                        label = com.patrykandpatrick.vico.compose.component.textComponent(
                            color = androidx.compose.ui.graphics.Color.Black
                        ),
                        labelText = "Podlanie"
                    )
                ),
                iconSize = 8.dp,
                iconPadding = 8.dp,
                spacing = 4.dp,
                padding = com.patrykandpatrick.vico.compose.dimensions.dimensionsOf(16.dp)
            )
            val lineSpecs = mutableListOf(
                com.patrykandpatrick.vico.compose.chart.line.lineSpec(lineColor = tempColor),
                com.patrykandpatrick.vico.compose.chart.line.lineSpec(lineColor = humidityColor)
            )

            if (historyData.any { it.pump_active }) {
                lineSpecs.add(
                    com.patrykandpatrick.vico.compose.chart.line.lineSpec(
                        lineColor = androidx.compose.ui.graphics.Color.Transparent,
                        pointSize = 12.dp,
                        point = com.patrykandpatrick.vico.compose.component.shapeComponent(
                            shape = com.patrykandpatrick.vico.core.component.shape.Shapes.pillShape,
                            color = androidx.compose.ui.graphics.Color.Blue
                        )
                    )
                )
            }

           Chart(
                chart = lineChart(
                    lines = lineSpecs
                ),
                model = chartModel,
                startAxis = rememberStartAxis(
                    titleComponent = com.patrykandpatrick.vico.compose.component.textComponent(
                        color = androidx.compose.ui.graphics.Color.DarkGray,
                        padding = com.patrykandpatrick.vico.compose.dimensions.dimensionsOf(horizontal = 4.dp)
                    ),
                    title = "Wartość"
                ),
                bottomAxis = rememberBottomAxis(
                    valueFormatter = bottomAxisFormatter,
                    titleComponent = com.patrykandpatrick.vico.compose.component.textComponent(
                        color = androidx.compose.ui.graphics.Color.DarkGray
                    ),
                    title = "Oś czasu (Pomiary)"
                ),
                legend = legend,
                modifier = Modifier.weight(1f)
            )
        } else {
            Text("Brak danych do wyświetlenia. Spróbuj połączyć się z serwerem w Ustawieniach.")
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