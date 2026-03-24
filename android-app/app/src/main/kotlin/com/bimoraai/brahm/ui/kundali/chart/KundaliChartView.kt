package com.bimoraai.brahm.ui.kundali.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimoraai.brahm.core.theme.*

// North Indian diamond-style Kundali chart rendered with pure Compose Canvas
@Composable
fun KundaliChartView(
    grahas: Map<Int, List<String>>,   // house number (1-12) -> list of planet abbreviations
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .background(BrahmCard)
    ) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.5.dp.toPx()
        val borderColor = BrahmBorder
        val lineColor = BrahmGold.copy(alpha = 0.4f)
        val textColor = BrahmForeground
        val planetColor = BrahmSaffron

        // Outer rectangle
        drawRect(color = borderColor, style = Stroke(strokeWidth))

        // Inner diamond (connecting midpoints of sides)
        val cx = w / 2f; val cy = h / 2f
        val mid = w / 2f

        // 4 diagonal lines forming the inner diamond
        drawLine(lineColor, Offset(0f, 0f), Offset(w, h), strokeWidth)
        drawLine(lineColor, Offset(w, 0f), Offset(0f, h), strokeWidth)
        drawLine(lineColor, Offset(mid, 0f), Offset(w, mid), strokeWidth)
        drawLine(lineColor, Offset(w, mid), Offset(mid, h), strokeWidth)
        drawLine(lineColor, Offset(mid, h), Offset(0f, mid), strokeWidth)
        drawLine(lineColor, Offset(0f, mid), Offset(mid, 0f), strokeWidth)

        // House centers (North Indian layout)
        val houseCenter = northIndianHouseCenters(w, h)

        // Draw planet abbreviations in each house
        grahas.forEach { (house, planets) ->
            val center = houseCenter[house] ?: return@forEach
            val text = planets.joinToString(" ")
            if (text.isNotBlank()) {
                val measured = textMeasurer.measure(
                    text,
                    style = TextStyle(color = planetColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = text,
                    topLeft = Offset(
                        center.x - measured.size.width / 2f,
                        center.y - measured.size.height / 2f,
                    ),
                    style = TextStyle(color = planetColor, fontSize = 10.sp, fontWeight = FontWeight.Medium),
                )
            }
        }
    }
}

// House centers for North Indian (diamond) chart layout
private fun northIndianHouseCenters(w: Float, h: Float): Map<Int, Offset> {
    val q = w / 4f
    val r = h / 4f
    return mapOf(
        1  to Offset(2 * q, 2 * r),         // center (Lagna)
        2  to Offset(3 * q, 1 * r),
        3  to Offset(4 * q, 0 * r),         // top-right corner
        4  to Offset(3 * q, 2 * r),
        5  to Offset(4 * q, 4 * r),         // bottom-right corner
        6  to Offset(3 * q, 3 * r),
        7  to Offset(2 * q, 4 * r),         // bottom center
        8  to Offset(1 * q, 3 * r),
        9  to Offset(0 * q, 4 * r),         // bottom-left corner
        10 to Offset(1 * q, 2 * r),
        11 to Offset(0 * q, 0 * r),         // top-left corner
        12 to Offset(1 * q, 1 * r),
    )
}
