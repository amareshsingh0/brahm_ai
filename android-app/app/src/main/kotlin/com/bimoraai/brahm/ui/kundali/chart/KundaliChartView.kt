package com.bimoraai.brahm.ui.kundali.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimoraai.brahm.core.theme.*
import kotlin.math.*

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
// \uFE0E forces text presentation (not emoji) for astrological zodiac symbols
val ZODIAC_SYMBOLS = listOf(
    "♈\uFE0E","♉\uFE0E","♊\uFE0E","♋\uFE0E","♌\uFE0E","♍\uFE0E",
    "♎\uFE0E","♏\uFE0E","♐\uFE0E","♑\uFE0E","♒\uFE0E","♓\uFE0E",
)
// Full English rashi names (index-matched with RASHI_SHORT)
val RASHI_FULL_EN = listOf(
    "Aries","Taurus","Gemini","Cancer","Leo","Virgo",
    "Libra","Scorpio","Sagittarius","Capricorn","Aquarius","Pisces",
)

// Planet abbreviation → full English name
val PLANET_FULL = mapOf(
    "Su" to "Sun", "Mo" to "Moon", "Ma" to "Mars", "Bu" to "Mercury",
    "Gu" to "Jupiter", "Sk" to "Venus", "Sa" to "Saturn",
    "Ra" to "Rahu", "Ke" to "Ketu", "Lag" to "Lagna",
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

// ─── South Indian: rashi index → (row, col) in 4×4 grid ─────────────────────
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

// ─── North Indian: house → (row, col) in 4×4 perimeter grid ─────────────────
// H1 at top-center, houses go counter-clockwise (H2 left of H1, H12 right of H1)
private val NORTH_HOUSE_POS = listOf(
    0 to 2, // H1  top row col 2
    0 to 1, // H2  top row col 1
    0 to 0, // H3  top-left corner
    1 to 0, // H4  left col
    2 to 0, // H5  left col
    3 to 0, // H6  bottom-left corner
    3 to 1, // H7  bottom row
    3 to 2, // H8  bottom row
    3 to 3, // H9  bottom-right corner
    2 to 3, // H10 right col
    1 to 3, // H11 right col
    0 to 3, // H12 top-right corner
)

// ─── East Indian: house → relative (x, y) as fractions of canvas width ──────
// Diamond extends to canvas edges. Labels placed in center of each triangular section.
// H1 = top, H4 = right-bottom, H7 = bottom, H10 = left (matching website screenshot)
private val EAST_HOUSE_REL: List<Pair<Float,Float>> = listOf(
    0.500f to 0.115f, // H1  top section (outside diamond, just below top tip)
    0.640f to 0.275f, // H2  upper-right inner
    0.880f to 0.500f, // H3  right section (outside diamond)
    0.720f to 0.640f, // H4  lower-right inner
    0.630f to 0.760f, // H5  lower-right inner-2
    0.500f to 0.720f, // H6  lower-center inner
    0.500f to 0.885f, // H7  bottom section (outside diamond)
    0.370f to 0.760f, // H8  lower-left inner-2
    0.280f to 0.640f, // H9  lower-left inner
    0.120f to 0.500f, // H10 left section (outside diamond)
    0.360f to 0.275f, // H11 upper-left inner
    0.500f to 0.300f, // H12 upper-center inner
)

// ─── KundaliChartView ─────────────────────────────────────────────────────────

@Composable
fun KundaliChartView(
    grahas: Map<Int, List<String>>,
    lagnaRashi: String = "",
    chartStyle: String = "North",
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val lagnaIdx = rashiIdxByName(lagnaRashi)

    // Full English names for all chart styles
    val houseRashis: Map<Int, String> = if (lagnaIdx >= 0) {
        (1..12).associateWith { h -> RASHI_FULL_EN[(lagnaIdx + h - 1) % 12] }
    } else emptyMap()
    // Use full English name for center display (lagnaRashi may be Sanskrit e.g. "Dhanu")
    val lagnaDisplay = if (lagnaIdx >= 0) RASHI_FULL_EN[lagnaIdx] else lagnaRashi

    when (chartStyle) {
        "South" -> SouthIndianChart(
            grahas       = grahas,
            lagnaIdx     = lagnaIdx,
            houseRashis  = houseRashis,
            textMeasurer = textMeasurer,
            modifier     = modifier,
        )
        "East" -> EastIndianChart(
            grahas       = grahas,
            lagnaRashi   = lagnaDisplay,
            houseRashis  = houseRashis,
            textMeasurer = textMeasurer,
            modifier     = modifier,
        )
        "West" -> WestIndianChart(
            grahas       = grahas,
            lagnaRashi   = lagnaDisplay,
            lagnaIdx     = lagnaIdx,
            houseRashis  = houseRashis,
            textMeasurer = textMeasurer,
            modifier     = modifier,
        )
        else -> NorthIndianChart(
            grahas       = grahas,
            lagnaRashi   = lagnaDisplay,
            houseRashis  = houseRashis,
            textMeasurer = textMeasurer,
            modifier     = modifier,
        )
    }
}

// ─── North Indian Chart (4×4 square grid, perimeter houses) ──────────────────

@Composable
private fun NorthIndianChart(
    grahas: Map<Int, List<String>>,
    lagnaRashi: String,
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
        val W = size.width
        val cellW = W / 4f
        val cellH = W / 4f
        val sw = 1.dp.toPx()
        val lineColor = BrahmGold.copy(alpha = 0.35f)

        // Draw 12 perimeter cells
        for (house in 1..12) {
            val (row, col) = NORTH_HOUSE_POS[house - 1]
            val x = col * cellW
            val y = row * cellH

            // H1 (lagna) highlight
            if (house == 1) {
                drawRect(
                    color   = BrahmGold.copy(alpha = 0.07f),
                    topLeft = Offset(x, y),
                    size    = Size(cellW, cellH),
                )
            }

            // Cell border
            drawRect(
                color   = BrahmBorder,
                topLeft = Offset(x, y),
                size    = Size(cellW, cellH),
                style   = Stroke(sw),
            )

            drawNorthHouseContent(
                textMeasurer = textMeasurer,
                house        = house,
                cellX        = x,
                cellY        = y,
                rashi        = houseRashis[house] ?: "",
                planets      = grahas[house] ?: emptyList(),
                cellW        = cellW,
                cellH        = cellH,
                isLagna      = house == 1,
            )
        }

        // Center 2×2 box with X lines
        val cx1 = cellW; val cy1 = cellH
        val cx2 = cellW * 3f; val cy2 = cellH * 3f
        drawRect(
            color   = BrahmGold.copy(alpha = 0.05f),
            topLeft = Offset(cx1, cy1),
            size    = Size(cellW * 2f, cellH * 2f),
        )
        drawRect(
            color   = BrahmBorder,
            topLeft = Offset(cx1, cy1),
            size    = Size(cellW * 2f, cellH * 2f),
            style   = Stroke(sw),
        )
        // Diagonal X
        drawLine(lineColor, Offset(cx1, cy1), Offset(cx2, cy2), sw)
        drawLine(lineColor, Offset(cx2, cy1), Offset(cx1, cy2), sw)

        // Center label
        val C = W / 2f
        if (lagnaRashi.isNotBlank()) {
            val rm = textMeasurer.measure(lagnaRashi, TextStyle(fontSize = 7.sp, fontWeight = FontWeight.Bold))
            drawText(
                textMeasurer = textMeasurer,
                text         = lagnaRashi,
                topLeft      = Offset(C - rm.size.width / 2f, C - rm.size.height - 0.5.dp.toPx()),
                style        = TextStyle(color = BrahmGold, fontSize = 7.sp, fontWeight = FontWeight.Bold),
            )
        }
        val lm = textMeasurer.measure("Lagna", TextStyle(fontSize = 6.sp))
        drawText(
            textMeasurer = textMeasurer,
            text         = "Lagna",
            topLeft      = Offset(C - lm.size.width / 2f, C + 1.dp.toPx()),
            style        = TextStyle(color = BrahmMutedForeground.copy(alpha = 0.6f), fontSize = 6.sp),
        )
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

        for (row in 0..3) {
            for (col in 0..3) {
                if (row in 1..2 && col in 1..2) continue

                drawRect(
                    color   = BrahmBorder,
                    topLeft = Offset(col * cellW, row * cellH),
                    size    = Size(cellW, cellH),
                    style   = Stroke(sw),
                )

                val rashiIdx = SOUTH_RASHI_POS.indexOfFirst { (r, c) -> r == row && c == col }
                if (rashiIdx < 0) continue

                val house = if (lagnaIdx >= 0) ((rashiIdx - lagnaIdx + 12) % 12) + 1 else rashiIdx + 1

                // Lagna: small gold triangle in top-left corner
                if (house == 1) {
                    val x = col * cellW
                    val y = row * cellH
                    val triSize = 14.dp.toPx()
                    val triPath = Path().apply {
                        moveTo(x + 2f, y + 2f)
                        lineTo(x + triSize, y + 2f)
                        lineTo(x + 2f, y + triSize)
                        close()
                    }
                    drawPath(triPath, color = BrahmGold.copy(alpha = 0.8f), style = Fill)
                }

                val center = Offset(col * cellW + cellW / 2f, row * cellH + cellH / 2f)
                drawSouthHouseContent(
                    textMeasurer = textMeasurer,
                    house        = house,
                    center       = center,
                    rashi        = RASHI_FULL_EN[rashiIdx],
                    planets      = grahas[house] ?: emptyList(),
                    cellW        = cellW,
                    cellH        = cellH,
                    isLagna      = house == 1,
                )
            }
        }

        // Center 2×2 outer border
        drawRect(
            color   = BrahmBorder,
            topLeft = Offset(cellW, cellH),
            size    = Size(cellW * 2, cellH * 2),
            style   = Stroke(sw),
        )

        // Center "Brahm AI / Kundali" label
        val cx = size.width / 2f; val cy = size.height / 2f
        val t1 = "Brahm AI"
        val m1 = textMeasurer.measure(t1, TextStyle(fontSize = 8.sp))
        drawText(
            textMeasurer = textMeasurer,
            text         = t1,
            topLeft      = Offset(cx - m1.size.width / 2f, cy - m1.size.height - 1.dp.toPx()),
            style        = TextStyle(color = BrahmGold.copy(alpha = 0.6f), fontSize = 8.sp, fontWeight = FontWeight.SemiBold),
        )
        val t2 = "Kundali"
        val m2 = textMeasurer.measure(t2, TextStyle(fontSize = 7.sp))
        drawText(
            textMeasurer = textMeasurer,
            text         = t2,
            topLeft      = Offset(cx - m2.size.width / 2f, cy + 1.dp.toPx()),
            style        = TextStyle(color = BrahmMutedForeground.copy(alpha = 0.5f), fontSize = 7.sp),
        )
    }
}

// ─── East Indian Chart (Diamond / Bengali style) ─────────────────────────────

@Composable
private fun EastIndianChart(
    grahas: Map<Int, List<String>>,
    lagnaRashi: String,
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
        val W = size.width
        val C = W / 2f
        val sw = 1.dp.toPx()
        val lineColor = BrahmGold.copy(alpha = 0.35f)

        // Outer diamond touches canvas edges (tip at each edge midpoint)
        val outerPath = Path().apply {
            moveTo(C, 0f)          // top
            lineTo(W, C)           // right
            lineTo(C, W)           // bottom
            lineTo(0f, C)          // left
            close()
        }
        drawPath(outerPath, color = BrahmBorder, style = Stroke(sw))

        // Inner diamond (center area highlight)
        val innerR = W * 0.165f
        val innerPath = Path().apply {
            moveTo(C, C - innerR)
            lineTo(C + innerR, C)
            lineTo(C, C + innerR)
            lineTo(C - innerR, C)
            close()
        }
        drawPath(innerPath, color = BrahmGold.copy(alpha = 0.06f), style = Fill)
        drawPath(innerPath, color = BrahmBorder, style = Stroke(sw))

        // 4 lines from canvas corners through center (creating 12 sections)
        drawLine(lineColor, Offset(C, 0f), Offset(C, W), sw)            // vertical
        drawLine(lineColor, Offset(0f, C), Offset(W, C), sw)            // horizontal
        drawLine(lineColor, Offset(0f, 0f), Offset(W, W), sw)           // diagonal ↘
        drawLine(lineColor, Offset(W, 0f), Offset(0f, W), sw)           // diagonal ↙

        // House labels: house number + full rashi name + planets
        for (house in 1..12) {
            val (rx, ry) = EAST_HOUSE_REL[house - 1]
            val cx = W * rx
            val cy = W * ry
            val rashi = houseRashis[house] ?: ""
            val planets = grahas[house] ?: emptyList()
            val isLagna = house == 1

            // House number
            val numText = if (isLagna) "$house" else "$house"
            val nm = textMeasurer.measure(numText, TextStyle(fontSize = 7.sp))
            drawText(
                textMeasurer = textMeasurer,
                text         = numText,
                topLeft      = Offset(cx - nm.size.width / 2f, cy - nm.size.height - 0.5.dp.toPx()),
                style        = TextStyle(
                    color      = if (isLagna) BrahmGold else BrahmMutedForeground.copy(alpha = 0.6f),
                    fontSize   = 7.sp,
                    fontWeight = if (isLagna) FontWeight.Bold else FontWeight.Normal,
                ),
            )

            // Full rashi name + ▲ for lagna
            if (rashi.isNotBlank()) {
                val label = if (isLagna) "$rashi ▲" else rashi
                val rm = textMeasurer.measure(label, TextStyle(fontSize = 7.sp))
                drawText(
                    textMeasurer = textMeasurer,
                    text         = label,
                    topLeft      = Offset(cx - rm.size.width / 2f, cy + 0.5.dp.toPx()),
                    style        = TextStyle(
                        color      = if (isLagna) BrahmGold else BrahmMutedForeground.copy(alpha = 0.55f),
                        fontSize   = 7.sp,
                        fontWeight = if (isLagna) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                )
            }

            // Planets stacked below rashi name
            if (planets.isNotEmpty()) {
                val lineH = 9.sp.toPx() * 1.35f
                val topY  = cy + 9.dp.toPx()
                planets.forEachIndexed { i, abbr ->
                    val color = PLANET_COLORS[abbr] ?: Color(0xFFD4540A)
                    val sym   = PLANET_SYMBOLS[abbr]
                    val name  = PLANET_FULL[abbr] ?: abbr
                    val rowY  = topY + i * lineH
                    // Skip rows that overflow canvas bounds (prevents maxHeight crash)
                    if (rowY < 0f || rowY + lineH > W) return@forEachIndexed
                    if (sym != null && sym != "Asc") {
                        val sm  = textMeasurer.measure(sym, TextStyle(fontSize = 9.sp, fontFamily = FontFamily.Serif))
                        val tnm = textMeasurer.measure(name, TextStyle(fontSize = 7.sp))
                        val totalW = sm.size.width + 2.dp.toPx() + tnm.size.width
                        val sx = cx - totalW / 2f
                        drawText(textMeasurer, sym, Offset(sx, rowY),
                            TextStyle(color = color, fontSize = 9.sp, fontFamily = FontFamily.Serif))
                        drawText(textMeasurer, name, Offset(sx + sm.size.width + 2.dp.toPx(), rowY + (sm.size.height - tnm.size.height) / 2f),
                            TextStyle(color = color, fontSize = 7.sp, fontWeight = FontWeight.SemiBold))
                    } else {
                        val pm = textMeasurer.measure(name, TextStyle(fontSize = 7.5.sp))
                        drawText(textMeasurer, name, Offset(cx - pm.size.width / 2f, rowY),
                            TextStyle(color = color, fontSize = 7.5.sp, fontWeight = FontWeight.SemiBold))
                    }
                }
            }
        }

        // Center label: "East / RashiName Lagna"
        val em = textMeasurer.measure("East", TextStyle(fontSize = 7.sp))
        drawText(
            textMeasurer = textMeasurer,
            text         = "East",
            topLeft      = Offset(C - em.size.width / 2f, C - em.size.height - 1.dp.toPx()),
            style        = TextStyle(color = BrahmGold.copy(alpha = 0.5f), fontSize = 7.sp),
        )
        if (lagnaRashi.isNotBlank()) {
            val lbl = "$lagnaRashi Lagna"
            val rm  = textMeasurer.measure(lbl, TextStyle(fontSize = 6.sp))
            drawText(
                textMeasurer = textMeasurer,
                text         = lbl,
                topLeft      = Offset(C - rm.size.width / 2f, C + 1.dp.toPx()),
                style        = TextStyle(color = BrahmMutedForeground.copy(alpha = 0.5f), fontSize = 6.sp),
            )
        }
    }
}

// ─── West Indian Chart (Circular / Western Wheel) ────────────────────────────

@Composable
private fun WestIndianChart(
    grahas: Map<Int, List<String>>,
    lagnaRashi: String,
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
        val W  = size.width
        val C  = W / 2f
        // Ring boundaries (from outside in):
        // Canvas edge → R1: zodiac symbols (outermost ring)
        // R1 → R2: house number + rashi name
        // R2 → R3: planets
        // R3 center: lagna info
        val R1 = C * 0.82f  // inner boundary of zodiac ring
        val R2 = C * 0.62f  // inner boundary of rashi ring
        val R3 = C * 0.28f  // center area boundary
        val sw = 1.dp.toPx()
        val lineColor = BrahmGold.copy(alpha = 0.30f)

        // Filled center circle
        drawCircle(color = BrahmGold.copy(alpha = 0.06f), radius = R3, center = Offset(C, C))

        // 3 concentric circles
        drawCircle(color = BrahmBorder, radius = R1, center = Offset(C, C), style = Stroke(sw))
        drawCircle(color = BrahmBorder, radius = R2, center = Offset(C, C), style = Stroke(sw))
        drawCircle(color = BrahmBorder, radius = R3, center = Offset(C, C), style = Stroke(sw))

        // 12 radial divider lines from R3 to canvas edge
        for (i in 0..11) {
            val angle = PI - (i * PI / 6.0)
            val x1 = C + (R3 * cos(angle)).toFloat()
            val y1 = C - (R3 * sin(angle)).toFloat()
            // Extend to canvas edge
            val x2 = C + (C * cos(angle)).toFloat()
            val y2 = C - (C * sin(angle)).toFloat()
            drawLine(lineColor, Offset(x1, y1), Offset(x2, y2), sw)
        }

        val badgeSize = 14.dp.toPx()
        val badgeHalf = badgeSize / 2f
        val badgeRound = 3.dp.toPx()

        // House sectors: zodiac badge (outside R1), house+rashi (R1-R2), planets (R2-R3)
        for (i in 0..11) {
            val houseId  = i + 1
            val midAngle = PI - ((i + 0.5) * PI / 6.0)
            val isLagna  = houseId == 1

            // ── Zodiac icon badge OUTSIDE R1 ────────────────────────────────────
            val symR = (R1 + C) / 2f
            val symX = (C + symR * cos(midAngle)).toFloat()
            val symY = (C - symR * sin(midAngle)).toFloat()
            val zodiacIdx = if (lagnaIdx >= 0) (lagnaIdx + i) % 12 else i
            val zodSym = ZODIAC_SYMBOLS[zodiacIdx]
            // Small rounded square badge (app theme gold)
            drawRoundRect(
                color        = BrahmGold.copy(alpha = if (isLagna) 0.25f else 0.12f),
                topLeft      = Offset(symX - badgeHalf, symY - badgeHalf),
                size         = Size(badgeSize, badgeSize),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(badgeRound),
            )
            drawRoundRect(
                color        = BrahmGold.copy(alpha = if (isLagna) 0.8f else 0.4f),
                topLeft      = Offset(symX - badgeHalf, symY - badgeHalf),
                size         = Size(badgeSize, badgeSize),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(badgeRound),
                style        = Stroke(0.8.dp.toPx()),
            )
            // Serif font forces text/symbol rendering (not emoji) — same as SkyScreen
            val zm = textMeasurer.measure(zodSym, TextStyle(fontSize = 8.sp, fontFamily = FontFamily.Serif))
            drawText(
                textMeasurer = textMeasurer,
                text         = zodSym,
                topLeft      = Offset(symX - zm.size.width / 2f, symY - zm.size.height / 2f),
                style        = TextStyle(
                    color      = BrahmGold.copy(alpha = if (isLagna) 1f else 0.75f),
                    fontSize   = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                ),
            )

            // ── House number + 3-letter rashi (R1–R2) ───────────────────────────
            val midR   = (R1 + R2) / 2f
            val labelX = (C + midR * cos(midAngle)).toFloat()
            val labelY = (C - midR * sin(midAngle)).toFloat()

            val nm = textMeasurer.measure("$houseId", TextStyle(fontSize = 7.sp))
            drawText(
                textMeasurer = textMeasurer,
                text         = "$houseId",
                topLeft      = Offset(labelX - nm.size.width / 2f, labelY - nm.size.height - 0.5.dp.toPx()),
                style        = TextStyle(
                    color      = if (isLagna) BrahmGold else BrahmMutedForeground.copy(alpha = 0.7f),
                    fontSize   = 7.sp,
                    fontWeight = if (isLagna) FontWeight.Bold else FontWeight.Normal,
                ),
            )
            val rashiFull = houseRashis[houseId] ?: RASHI_FULL_EN[zodiacIdx]
            val rm = textMeasurer.measure(rashiFull, TextStyle(fontSize = 6.sp))
            drawText(
                textMeasurer = textMeasurer,
                text         = rashiFull,
                topLeft      = Offset(labelX - rm.size.width / 2f, labelY + 0.5.dp.toPx()),
                style        = TextStyle(
                    color      = if (isLagna) BrahmGold else BrahmGold.copy(alpha = 0.75f),
                    fontSize   = 6.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )

            // ── Planets between R2 and R3 (symbol + abbrev, serif for symbol) ──────
            val planets = grahas[houseId] ?: emptyList()
            if (planets.isNotEmpty()) {
                val baseR     = R3 + (R2 - R3) * 0.55f
                val lineH     = 8.sp.toPx() * 1.35f
                val perpAngle = midAngle + PI / 2.0
                val cols      = if (planets.size <= 2) planets.size else 2
                val tOff      = if (planets.size <= 1) 0f else (R2 - R3) * 0.22f
                planets.forEachIndexed { pi, abbr ->
                    val col    = pi % cols
                    val row    = pi / cols
                    val colOff = (col - (cols - 1) / 2f) * tOff
                    val rowOff = (row - ((ceil(planets.size.toDouble() / cols) - 1) / 2f)) * lineH
                    val pr     = baseR - rowOff
                    val px     = (C + pr * cos(midAngle) + colOff * cos(perpAngle)).toFloat()
                    val py     = (C - pr * sin(midAngle) - colOff * sin(perpAngle)).toFloat()
                    val color  = PLANET_COLORS[abbr] ?: Color(0xFFD4540A)
                    // Show astrological symbol if available, else 2-letter abbrev
                    val sym   = PLANET_SYMBOLS[abbr]
                    val label = if (sym != null && sym != "Asc") sym else abbr.take(2)
                    val ff    = if (sym != null && sym != "Asc") FontFamily.Serif else null
                    val pm    = textMeasurer.measure(label, TextStyle(fontSize = 7.5.sp, fontFamily = ff))
                    drawText(
                        textMeasurer = textMeasurer,
                        text         = label,
                        topLeft      = Offset(px - pm.size.width / 2f, py - pm.size.height / 2f),
                        style        = TextStyle(color = color, fontSize = 7.5.sp, fontWeight = FontWeight.SemiBold, fontFamily = ff),
                    )
                }
            }
        }

        // Center: lagna rashi + "Lagna"
        if (lagnaRashi.isNotBlank()) {
            val rm = textMeasurer.measure(lagnaRashi.take(7), TextStyle(fontSize = 8.sp))
            drawText(
                textMeasurer = textMeasurer,
                text         = lagnaRashi.take(7),
                topLeft      = Offset(C - rm.size.width / 2f, C - rm.size.height - 0.5.dp.toPx()),
                style        = TextStyle(color = BrahmGold, fontSize = 8.sp, fontWeight = FontWeight.Bold),
            )
            val lm = textMeasurer.measure("Lagna", TextStyle(fontSize = 6.5.sp))
            drawText(
                textMeasurer = textMeasurer,
                text         = "Lagna",
                topLeft      = Offset(C - lm.size.width / 2f, C + 1.dp.toPx()),
                style        = TextStyle(color = BrahmMutedForeground.copy(alpha = 0.7f), fontSize = 6.5.sp),
            )
        }
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
    // House number — top-left of cell
    drawText(
        textMeasurer = textMeasurer,
        text         = "$house",
        topLeft      = Offset(center.x - cellW * 0.4f, center.y - cellH * 0.42f),
        style        = TextStyle(color = BrahmMutedForeground.copy(alpha = 0.5f), fontSize = 7.sp),
    )

    // Rashi name — centered near top
    if (rashi.isNotBlank()) {
        val rm = textMeasurer.measure(rashi, TextStyle(fontSize = 7.sp))
        drawText(
            textMeasurer = textMeasurer,
            text         = rashi,
            topLeft      = Offset(center.x - rm.size.width / 2f, center.y - cellH * 0.35f),
            style        = TextStyle(color = BrahmMutedForeground.copy(alpha = 0.4f), fontSize = 7.sp),
        )
    }

    // Planets — stacked vertically, colored
    if (planets.isEmpty()) return
    val planetSp = 8.5f.sp.toPx()
    val lineH    = planetSp * 1.45f
    val topY     = center.y - cellH * 0.08f
    planets.forEachIndexed { i, abbr ->
        val color   = PLANET_COLORS[abbr] ?: Color(0xFFD4540A)
        val symbol  = PLANET_SYMBOLS[abbr]
        val display = if (symbol == null || symbol == "Asc") abbr else "$symbol $abbr"
        val pm = textMeasurer.measure(display, TextStyle(fontSize = 8.5.sp))
        val y  = topY + i * lineH - (planets.size * lineH / 2f)
        drawText(
            textMeasurer = textMeasurer,
            text         = display,
            topLeft      = Offset(center.x - pm.size.width / 2f, y),
            style        = TextStyle(color = color, fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}

// North Indian: house# top-left, rashi RIGHT-ALIGNED top-right, planets centered
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNorthHouseContent(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    house: Int,
    cellX: Float,
    cellY: Float,
    rashi: String,
    planets: List<String>,
    cellW: Float,
    cellH: Float,
    isLagna: Boolean = false,
) {
    val pad = 3.dp.toPx()

    // House number — top-left
    drawText(
        textMeasurer = textMeasurer,
        text         = "$house",
        topLeft      = Offset(cellX + pad, cellY + pad),
        style        = TextStyle(
            color      = if (isLagna) BrahmGold else BrahmMutedForeground.copy(alpha = 0.55f),
            fontSize   = 7.sp,
            fontWeight = if (isLagna) FontWeight.Bold else FontWeight.Normal,
        ),
    )

    // Rashi name — right-aligned near top
    if (rashi.isNotBlank()) {
        val rm = textMeasurer.measure(rashi, TextStyle(fontSize = 7.sp))
        drawText(
            textMeasurer = textMeasurer,
            text         = rashi,
            topLeft      = Offset(cellX + cellW - rm.size.width - pad, cellY + pad),
            style        = TextStyle(
                color      = if (isLagna) BrahmGold else BrahmMutedForeground.copy(alpha = 0.45f),
                fontSize   = 7.sp,
                fontWeight = if (isLagna) FontWeight.SemiBold else FontWeight.Normal,
            ),
        )
        // "Lag" below rashi for lagna cell
        if (isLagna) {
            val lm = textMeasurer.measure("Lag", TextStyle(fontSize = 6.sp))
            drawText(
                textMeasurer = textMeasurer,
                text         = "Lag",
                topLeft      = Offset(cellX + cellW - lm.size.width - pad, cellY + pad + rm.size.height + 1.dp.toPx()),
                style        = TextStyle(color = BrahmGold.copy(alpha = 0.7f), fontSize = 6.sp),
            )
        }
    }

    // Planets: symbol + full name centered in cell
    if (planets.isEmpty()) return
    val cx     = cellX + cellW / 2f
    val startY = cellY + cellH * 0.38f
    val lineH  = (9.sp.toPx() + 7.sp.toPx()) * 0.82f
    val totalH = planets.size * lineH
    val topY   = startY + (cellH * 0.52f - totalH) / 2f

    planets.forEachIndexed { i, abbr ->
        val color = PLANET_COLORS[abbr] ?: Color(0xFFD4540A)
        val sym   = PLANET_SYMBOLS[abbr]
        val name  = PLANET_FULL[abbr] ?: abbr
        val rowY  = topY + i * lineH
        if (sym != null && sym != "Asc") {
            val sm  = textMeasurer.measure(sym, TextStyle(fontSize = 9.sp, fontFamily = FontFamily.Serif))
            val tnm = textMeasurer.measure(name, TextStyle(fontSize = 7.sp))
            val totalW = sm.size.width + 2.dp.toPx() + tnm.size.width
            val sx = cx - totalW / 2f
            drawText(textMeasurer, sym, Offset(sx, rowY),
                TextStyle(color = color, fontSize = 9.sp, fontFamily = FontFamily.Serif))
            drawText(textMeasurer, name, Offset(sx + sm.size.width + 2.dp.toPx(), rowY + (sm.size.height - tnm.size.height) / 2f),
                TextStyle(color = color, fontSize = 7.sp, fontWeight = FontWeight.SemiBold))
        } else {
            val pm = textMeasurer.measure(name, TextStyle(fontSize = 7.5.sp))
            drawText(textMeasurer, name, Offset(cx - pm.size.width / 2f, rowY),
                TextStyle(color = color, fontSize = 7.5.sp, fontWeight = FontWeight.SemiBold))
        }
    }
}

// South Indian: house# small muted top-left, full rashi bold gold, planets with symbol+name
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSouthHouseContent(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    house: Int,
    center: Offset,
    rashi: String,
    planets: List<String>,
    cellW: Float,
    cellH: Float,
    isLagna: Boolean = false,
) {
    val pad  = 4.dp.toPx()
    val topY = center.y - cellH * 0.42f
    val leftX = center.x - cellW * 0.44f

    // House number — small, muted, top-left
    val nm = textMeasurer.measure("$house", TextStyle(fontSize = 6.5.sp))
    drawText(
        textMeasurer = textMeasurer,
        text         = "$house",
        topLeft      = Offset(leftX, topY),
        style        = TextStyle(color = BrahmMutedForeground.copy(alpha = 0.5f), fontSize = 6.5.sp),
    )

    // Full rashi name — bold gold, after house number
    if (rashi.isNotBlank()) {
        val rm = textMeasurer.measure(rashi, TextStyle(fontSize = 7.sp, fontWeight = FontWeight.Bold))
        drawText(
            textMeasurer = textMeasurer,
            text         = rashi,
            topLeft      = Offset(leftX + nm.size.width + pad, topY),
            style        = TextStyle(
                color      = BrahmGold,
                fontSize   = 7.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }

    // Planets — symbol (serif) + full name, centered, stacked
    if (planets.isEmpty()) return
    val symSp  = 9.sp.toPx()
    val nameSp = 7.sp.toPx()
    val lineH  = (symSp + nameSp) * 0.85f
    val startY = center.y - cellH * 0.08f - (planets.size * lineH / 2f)
    planets.forEachIndexed { i, abbr ->
        val color  = PLANET_COLORS[abbr] ?: Color(0xFFD4540A)
        val sym    = PLANET_SYMBOLS[abbr]
        val name   = PLANET_FULL[abbr] ?: abbr
        val rowY   = startY + i * lineH

        if (sym != null && sym != "Asc") {
            // Symbol centered, then name next to it
            val sm = textMeasurer.measure(sym, TextStyle(fontSize = 9.sp, fontFamily = FontFamily.Serif))
            val tnm = textMeasurer.measure(name, TextStyle(fontSize = 7.sp))
            val totalW = sm.size.width + 2.dp.toPx() + tnm.size.width
            val startX = center.x - totalW / 2f
            drawText(
                textMeasurer = textMeasurer,
                text         = sym,
                topLeft      = Offset(startX, rowY),
                style        = TextStyle(color = color, fontSize = 9.sp, fontFamily = FontFamily.Serif),
            )
            drawText(
                textMeasurer = textMeasurer,
                text         = name,
                topLeft      = Offset(startX + sm.size.width + 2.dp.toPx(), rowY + (sm.size.height - tnm.size.height) / 2f),
                style        = TextStyle(color = color, fontSize = 7.sp, fontWeight = FontWeight.SemiBold),
            )
        } else {
            val pm = textMeasurer.measure(name, TextStyle(fontSize = 7.5.sp))
            drawText(
                textMeasurer = textMeasurer,
                text         = name,
                topLeft      = Offset(center.x - pm.size.width / 2f, rowY),
                style        = TextStyle(color = color, fontSize = 7.5.sp, fontWeight = FontWeight.SemiBold),
            )
        }
    }
}
