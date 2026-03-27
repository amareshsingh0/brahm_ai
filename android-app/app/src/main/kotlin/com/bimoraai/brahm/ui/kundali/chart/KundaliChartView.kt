package com.bimoraai.brahm.ui.kundali.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimoraai.brahm.core.theme.*

// ─── Constants ────────────────────────────────────────────────────────────────

val PLANET_COLORS = mapOf(
    "Su"  to Color(0xFFF59E0B), "Surya"   to Color(0xFFF59E0B), "Sun"     to Color(0xFFF59E0B),
    "Mo"  to Color(0xFF93C5FD), "Chandra" to Color(0xFF93C5FD), "Moon"    to Color(0xFF93C5FD),
    "Ma"  to Color(0xFFEF4444), "Mangal"  to Color(0xFFEF4444), "Mars"    to Color(0xFFEF4444),
    "Bu"  to Color(0xFF22C55E), "Budh"    to Color(0xFF22C55E), "Mercury" to Color(0xFF22C55E),
    "Gu"  to Color(0xFFEAB308), "Guru"    to Color(0xFFEAB308), "Jupiter" to Color(0xFFEAB308),
    "Sk"  to Color(0xFFA855F7), "Shukra"  to Color(0xFFA855F7), "Venus"   to Color(0xFFA855F7),
    "Sa"  to Color(0xFF64748B), "Shani"   to Color(0xFF64748B), "Saturn"  to Color(0xFF64748B),
    "Ra"  to Color(0xFF6366F1), "Rahu"    to Color(0xFF6366F1),
    "Ke"  to Color(0xFFF97316), "Ketu"    to Color(0xFFF97316),
    "Lag" to Color(0xFFB07A00),
)

val PLANET_SYMBOLS = mapOf(
    "Su" to "☉", "Mo" to "☽", "Ma" to "♂", "Bu" to "☿",
    "Gu" to "♃", "Sk" to "♀", "Sa" to "♄", "Ra" to "☊", "Ke" to "☋",
    "Lag" to "Asc",
)

val RASHI_SHORT = listOf("Ari","Tau","Gem","Can","Leo","Vir","Lib","Sco","Sag","Cap","Aqu","Pis")
val RASHI_LONG  = listOf(
    "Mesha","Vrishabha","Mithuna","Karka","Simha","Kanya",
    "Tula","Vrischika","Dhanu","Makara","Kumbha","Meena",
)

fun rashiIdxByName(name: String): Int {
    val clean = name.trim()
    RASHI_LONG.forEachIndexed  { i, r -> if (r.equals(clean, true)) return i }
    RASHI_SHORT.forEachIndexed { i, r -> if (r.equals(clean, true)) return i }
    listOf("aries","taurus","gemini","cancer","leo","virgo",
           "libra","scorpio","sagittarius","capricorn","aquarius","pisces")
        .forEachIndexed { i, r -> if (r.equals(clean, true)) return i }
    return -1
}

// ─── South Indian fixed cell positions (rashi index 0=Ari → 11=Pis) ──────────
// Grid is 4×4; cells (1,1),(1,2),(2,1),(2,2) are empty center
// (row, col): Pis=0,0  Ari=0,1  Tau=0,2  Gem=0,3
//             Aqu=1,0                     Can=1,3
//             Cap=2,0                     Leo=2,3
//             Sag=3,0  Sco=3,1  Lib=3,2  Vir=3,3
// (row, col) for each rashi index 0=Ari … 11=Pis in the 4×4 South Indian grid
private val SOUTH_RASHI_POS: List<Pair<Int,Int>> = listOf(
    0 to 1, // Ari  (0)
    0 to 2, // Tau  (1)
    0 to 3, // Gem  (2)
    1 to 3, // Can  (3)
    2 to 3, // Leo  (4)
    3 to 3, // Vir  (5)
    3 to 2, // Lib  (6)
    3 to 1, // Sco  (7)
    3 to 0, // Sag  (8)
    2 to 0, // Cap  (9)
    1 to 0, // Aqu  (10)
    0 to 0, // Pis  (11)
)

// ─── KundaliChartView ─────────────────────────────────────────────────────────

/**
 * Renders a Vedic birth chart.
 *
 * @param grahas       house number (1-12) → list of planet abbreviations
 * @param lagnaRashi   rashi name for house 1 (used to label all 12 houses)
 * @param chartStyle   "North" | "South" | "East" | "West"
 */
@Composable
fun KundaliChartView(
    grahas: Map<Int, List<String>>,
    lagnaRashi: String = "",
    chartStyle: String = "North",
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val lagnaIdx = rashiIdxByName(lagnaRashi)

    // house → rashi short name
    val houseRashis: Map<Int, String> = if (lagnaIdx >= 0) {
        (1..12).associateWith { h -> RASHI_SHORT[(lagnaIdx + h - 1) % 12] }
    } else emptyMap()

    when (chartStyle) {
        "South" -> SouthIndianChart(
            grahas       = grahas,
            lagnaIdx     = lagnaIdx,
            houseRashis  = houseRashis,
            textMeasurer = textMeasurer,
            modifier     = modifier,
        )
        "East", "West" -> EastIndianChart(
            grahas       = grahas,
            houseRashis  = houseRashis,
            textMeasurer = textMeasurer,
            modifier     = modifier,
        )
        else -> NorthIndianChart(
            grahas       = grahas,
            houseRashis  = houseRashis,
            textMeasurer = textMeasurer,
            modifier     = modifier,
        )
    }
}

// ─── North Indian Diamond Chart ───────────────────────────────────────────────

@Composable
private fun NorthIndianChart(
    grahas: Map<Int, List<String>>,
    houseRashis: Map<Int, String>,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    modifier: Modifier,
) {
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(BrahmCard)
    ) {
        val w = size.width; val h = size.height
        val sw = 1.2.dp.toPx()
        val lineColor = BrahmGold.copy(alpha = 0.35f)
        val mid = w / 2f

        // Outer border + diamond lines
        drawRect(color = BrahmBorder, style = Stroke(sw))
        drawLine(lineColor, Offset(0f, 0f),    Offset(w, h),    sw)
        drawLine(lineColor, Offset(w, 0f),     Offset(0f, h),   sw)
        drawLine(lineColor, Offset(mid, 0f),   Offset(w, mid),  sw)
        drawLine(lineColor, Offset(w, mid),    Offset(mid, h),  sw)
        drawLine(lineColor, Offset(mid, h),    Offset(0f, mid), sw)
        drawLine(lineColor, Offset(0f, mid),   Offset(mid, 0f), sw)

        // Correct centers — corner houses moved inside their triangles
        val q = w / 4f; val r = h / 4f
        val centers = mapOf(
            1  to Offset(2f   * q, 2f   * r),
            2  to Offset(3f   * q, 1f   * r),
            3  to Offset(3.4f * q, 0.5f * r),
            4  to Offset(3f   * q, 2f   * r),
            5  to Offset(3.4f * q, 3.5f * r),
            6  to Offset(3f   * q, 3f   * r),
            7  to Offset(2f   * q, 3.4f * r),
            8  to Offset(1f   * q, 3f   * r),
            9  to Offset(0.6f * q, 3.5f * r),
            10 to Offset(1f   * q, 2f   * r),
            11 to Offset(0.6f * q, 0.5f * r),
            12 to Offset(1f   * q, 1f   * r),
        )

        for (house in 1..12) {
            val center = centers[house] ?: continue
            drawHouseContent(
                textMeasurer = textMeasurer,
                house        = house,
                center       = center,
                rashi        = houseRashis[house] ?: "",
                planets      = grahas[house] ?: emptyList(),
                cellW        = q * 1.8f,
                cellH        = r * 1.6f,
            )
        }
    }
}

// ─── South Indian Chart ───────────────────────────────────────────────────────

@Composable
private fun SouthIndianChart(
    grahas: Map<Int, List<String>>,
    lagnaIdx: Int,
    houseRashis: Map<Int, String>,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    modifier: Modifier,
) {
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(BrahmCard)
    ) {
        val cellW = size.width / 4f
        val cellH = size.height / 4f
        val sw = 1.dp.toPx()

        // Draw 12 cells (skip center 2×2)
        for (row in 0..3) {
            for (col in 0..3) {
                if (row in 1..2 && col in 1..2) continue  // empty center

                // Draw cell border
                drawRect(
                    color  = BrahmBorder,
                    topLeft = Offset(col * cellW, row * cellH),
                    size    = Size(cellW, cellH),
                    style   = Stroke(sw),
                )

                // Find which rashi this cell belongs to
                val rashiIdx = SOUTH_RASHI_POS.indexOfFirst { (r, c) -> r == row && c == col }
                if (rashiIdx < 0) continue

                // Find which house this rashi is (lagna = house 1)
                val house = if (lagnaIdx >= 0) {
                    ((rashiIdx - lagnaIdx + 12) % 12) + 1
                } else rashiIdx + 1

                val center = Offset(col * cellW + cellW / 2f, row * cellH + cellH / 2f)
                val rashi  = RASHI_SHORT[rashiIdx]

                // Lagna cell: colored top border
                if (house == 1) {
                    drawRect(
                        color   = BrahmGold.copy(alpha = 0.3f),
                        topLeft = Offset(col * cellW, row * cellH),
                        size    = Size(cellW, 3.dp.toPx()),
                    )
                }

                drawHouseContent(
                    textMeasurer = textMeasurer,
                    house        = house,
                    center       = center,
                    rashi        = rashi,
                    planets      = grahas[house] ?: emptyList(),
                    cellW        = cellW,
                    cellH        = cellH,
                )
            }
        }

        // Center 2×2: draw outer border only
        drawRect(
            color   = BrahmBorder,
            topLeft = Offset(cellW, cellH),
            size    = Size(cellW * 2, cellH * 2),
            style   = Stroke(sw),
        )
    }
}

// ─── East Indian Chart (Bengali style) ───────────────────────────────────────
// Houses fixed in 4×4 grid (center 2×2 empty). H1 top-left, clockwise.
// Row,Col → House: (0,0)=H1 (0,1)=H2 (0,2)=H3 (0,3)=H4
//                   (1,3)=H5                  (1,0)=H12
//                   (2,3)=H6                  (2,0)=H11
//                   (3,3)=H7 (3,2)=H8 (3,1)=H9 (3,0)=H10

private val EAST_HOUSE_POS: List<Pair<Int,Int>> = listOf(
    0 to 0, // H1
    0 to 1, // H2
    0 to 2, // H3
    0 to 3, // H4
    1 to 3, // H5
    2 to 3, // H6
    3 to 3, // H7
    3 to 2, // H8
    3 to 1, // H9
    3 to 0, // H10
    2 to 0, // H11
    1 to 0, // H12
)

@Composable
private fun EastIndianChart(
    grahas: Map<Int, List<String>>,
    houseRashis: Map<Int, String>,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    modifier: Modifier,
) {
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(BrahmCard)
    ) {
        val cellW = size.width / 4f
        val cellH = size.height / 4f
        val sw = 1.dp.toPx()

        for (house in 1..12) {
            val (row, col) = EAST_HOUSE_POS[house - 1]
            // Cell border
            drawRect(
                color   = BrahmBorder,
                topLeft = Offset(col * cellW, row * cellH),
                size    = Size(cellW, cellH),
                style   = Stroke(sw),
            )
            // H1 highlight (lagna)
            if (house == 1) {
                drawRect(
                    color   = BrahmGold.copy(alpha = 0.25f),
                    topLeft = Offset(col * cellW, row * cellH),
                    size    = Size(cellW, 3.dp.toPx()),
                )
            }
            val center = Offset(col * cellW + cellW / 2f, row * cellH + cellH / 2f)
            drawHouseContent(
                textMeasurer = textMeasurer,
                house        = house,
                center       = center,
                rashi        = houseRashis[house] ?: "",
                planets      = grahas[house] ?: emptyList(),
                cellW        = cellW,
                cellH        = cellH,
            )
        }
        // Center 2×2 border
        drawRect(
            color   = BrahmBorder,
            topLeft = Offset(cellW, cellH),
            size    = Size(cellW * 2, cellH * 2),
            style   = Stroke(sw),
        )
    }
}

// ─── Shared house content renderer ───────────────────────────────────────────

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHouseContent(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    house: Int,
    center: Offset,
    rashi: String,
    planets: List<String>,
    cellW: Float,
    cellH: Float,
) {
    val numSp  = 7.sp.toPx()
    val rashiSp = 7.sp.toPx()
    val planetSp = 8.5.sp.toPx()

    // House number — top-left of cell
    val numText = "$house"
    val numM = textMeasurer.measure(numText, TextStyle(fontSize = 7.sp))
    drawText(
        textMeasurer = textMeasurer,
        text         = numText,
        topLeft      = Offset(center.x - cellW * 0.4f, center.y - cellH * 0.42f),
        style        = TextStyle(
            color      = BrahmMutedForeground.copy(alpha = 0.5f),
            fontSize   = 7.sp,
        ),
    )

    // Rashi name — centered near top of cell
    if (rashi.isNotBlank()) {
        val rm = textMeasurer.measure(rashi, TextStyle(fontSize = 7.sp))
        drawText(
            textMeasurer = textMeasurer,
            text         = rashi,
            topLeft      = Offset(center.x - rm.size.width / 2f, center.y - cellH * 0.35f),
            style        = TextStyle(
                color    = BrahmMutedForeground.copy(alpha = 0.4f),
                fontSize = 7.sp,
            ),
        )
    }

    // Planets — stacked vertically, colored
    if (planets.isEmpty()) return
    val lineH   = planetSp * 1.45f
    val topY    = center.y - cellH * 0.08f  // slightly below center (rashi takes top portion)
    planets.forEachIndexed { i, abbr ->
        val color   = PLANET_COLORS[abbr] ?: Color(0xFFD4540A)
        val symbol  = PLANET_SYMBOLS[abbr]
        val display = when {
            symbol == null || symbol == "Asc" -> abbr
            else -> "$symbol $abbr"
        }
        val pm = textMeasurer.measure(display, TextStyle(fontSize = 8.5.sp))
        val y  = topY + i * lineH - (planets.size * lineH / 2f)
        drawText(
            textMeasurer = textMeasurer,
            text         = display,
            topLeft      = Offset(center.x - pm.size.width / 2f, y),
            style        = TextStyle(
                color      = color,
                fontSize   = 8.5.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}
