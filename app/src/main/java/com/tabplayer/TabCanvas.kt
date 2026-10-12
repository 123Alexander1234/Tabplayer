package com.tabplayer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp

@Composable
fun TabCanvas(track: TabTrack, positionMs: Long, modifier: Modifier = Modifier) {
    val tm = rememberTextMeasurer()

    val noteStyle = TextStyle(
        color = Color(0xFFE5E7EB), fontSize = 14.sp,
        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
    )
    val highlightedNoteStyle = TextStyle(
        color = Color(0xFFFFFFFF), fontSize = 16.sp,
        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
    )
    val labelStyle = TextStyle(
        color = Color(0xFFB0B8C4), fontSize = 11.sp,
        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
    )

    val bgColor = Color(0xFF13151A)
    val stringColor = Color(0xFF9CA5B4)
    val stringGlow = Color(0x339CA5B4)
    val measureColor = Color(0xFFC5CCD6)
    val measureGlow = Color(0x33C5CCD6)
    val beadColor = Color(0xFFF59E0B)

    val bpm = track.bpm.coerceAtLeast(1)
    val timePerBeat = 60000f / bpm
    val beatsPerMeasure = 4
    val measuresPerRow = 2
    val beatsPerRow = beatsPerMeasure * measuresPerRow
    val timePerRow = beatsPerRow * timePerBeat
    val rowsVisible = 4

    Box(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxSize()) {
            val leftPad = 46f
            val rightPad = 16f
            val topPad = 4f
            val bottomPad = 4f
            val usableWidth = size.width - leftPad - rightPad
            val rowHeight = (size.height - topPad - bottomPad) / rowsVisible
            val stringGap = rowHeight / 7.5f
            val beatWidth = usableWidth / beatsPerRow
            val halfBeat = beatWidth * 0.5f

            val currentRow = (positionMs / timePerRow).toInt()
            val scrollOffset = (currentRow - 1).coerceAtLeast(0)
            val pxPerMs = usableWidth / timePerRow

            val playheadDisplayRow = currentRow - scrollOffset
            val playX: Float = if (playheadDisplayRow in 0 until rowsVisible) {
                val relTimeInRow = positionMs - currentRow * timePerRow
                leftPad + relTimeInRow * pxPerMs + halfBeat
            } else -1000f

            val playheadHalfWidth = 22f
            val highlightMin = playX - playheadHalfWidth
            val highlightMax = playX + playheadHalfWidth

            // 1. Струны и границы тактов
            for (displayRow in 0 until rowsVisible) {
                val rowTop = topPad + displayRow * rowHeight
                val firstStringY = rowTop + stringGap * 1.2f

                for (i in 0..3) {
                    val y = firstStringY + i * stringGap
                    drawLine(stringGlow, Offset(leftPad, y), Offset(size.width - rightPad, y), 4f)
                    drawLine(stringColor, Offset(leftPad, y), Offset(size.width - rightPad, y), 1.5f)
                    drawText(tm, track.tuning[3 - i], Offset(10f, y - 16f), labelStyle)
                }

                for (beat in 0..beatsPerRow step beatsPerMeasure) {
                    val x = leftPad + beat * beatWidth
                    val lineTop = firstStringY - 8f
                    val lineBottom = firstStringY + 3 * stringGap + 8f
                    drawLine(measureGlow, Offset(x, lineTop), Offset(x, lineBottom), 6f)
                    drawLine(measureColor, Offset(x, lineTop), Offset(x, lineBottom), 2f)
                }
            }

            // 2. Ноты и знаки длительности
            for (displayRow in 0 until rowsVisible) {
                val actualRow = scrollOffset + displayRow
                val rowTop = topPad + displayRow * rowHeight
                val rowBottomY = rowTop + rowHeight
                val firstStringY = rowTop + stringGap * 1.2f
                val rowStartMs = (actualRow * timePerRow).toLong()
                val rowEndMs = rowStartMs + timePerRow.toLong()

                val rowNotes = track.notes.filter { it.timeMs in rowStartMs until rowEndMs }
                val groups = rowNotes.groupBy { (it.timeMs / timePerBeat).toInt() }

                groups.forEach { (_, notesInGroup) ->
                    val sorted = notesInGroup.sortedBy { it.timeMs }
                    val minDur = sorted.minOf { it.durationMs / timePerBeat }
                    val flags = flagCountFor(minDur)
                    val needStem = minDur < 3.5f

                    // Цифры и точки
                    sorted.forEach { note ->
                        val relTime = note.timeMs - rowStartMs
                        val x = leftPad + relTime * pxPerMs + halfBeat
                        val y = firstStringY + (4 - note.string) * stringGap
                        val isHighlighted = displayRow == playheadDisplayRow &&
                                x >= highlightMin && x <= highlightMax

                        val style = if (isHighlighted) highlightedNoteStyle else noteStyle
                        val layout = tm.measure(AnnotatedString(note.fret.toString()), style)
                        val textW = layout.size.width.toFloat()
                        val textH = layout.size.height.toFloat()

                        drawRect(
                            color = bgColor,
                            topLeft = Offset(x - textW / 2f - 2f, y - textH / 2f - 1f),
                            size = Size(textW + 4f, textH + 2f)
                        )
                        drawText(layout, topLeft = Offset(x - textW / 2f, y - textH / 2f))

                        if (isDotted(note.durationMs / timePerBeat)) {
                            drawCircle(
                                color = if (isHighlighted) beadColor else Color(0xFFE5E7EB),
                                radius = 2f,
                                center = Offset(x + textW / 2f + 5f, y)
                            )
                        }
                    }

                    // Зона штилей (под всеми струнами)
                    if (needStem) {
                        drawNoteGroup(
                            notes = sorted,
                            rowStartMs = rowStartMs,
                            firstStringY = firstStringY,
                            stringGap = stringGap,
                            rowBottomY = rowBottomY,
                            leftPad = leftPad,
                            pxPerMs = pxPerMs,
                            halfBeat = halfBeat,
                            flags = flags,
                            isHighlighted = displayRow == playheadDisplayRow &&
                                    playX >= highlightMin && playX <= highlightMax,
                            beadColor = beadColor
                        )
                    }
                }
            }

            // 3. Курсор-медиатор
            if (playheadDisplayRow in 0 until rowsVisible) {
                val rowTop = topPad + playheadDisplayRow * rowHeight
                val rowBottom = rowTop + rowHeight
                val pw = playheadHalfWidth
                val topY = rowTop + 8f
                val arcBottom = topY + pw
                val pointY = rowBottom - 8f

                val path = Path().apply {
                    moveTo(playX, topY)
                    cubicTo(playX + pw * 0.55f, topY, playX + pw, topY + pw * 0.45f, playX + pw, arcBottom)
                    lineTo(playX + pw * 0.42f, pointY - 16f)
                    lineTo(playX, pointY)
                    lineTo(playX - pw * 0.42f, pointY - 16f)
                    lineTo(playX - pw, arcBottom)
                    cubicTo(playX - pw, topY + pw * 0.45f, playX - pw * 0.55f, topY, playX, topY)
                    close()
                }
                drawPath(path, beadColor.copy(alpha = 0.15f))
                drawPath(path, beadColor.copy(alpha = 0.22f))
                drawPath(path, beadColor, style = Stroke(width = 2.5f))
                drawCircle(beadColor, radius = 5f, center = Offset(playX, topY + 12f))
            }
        }
    }
}

private fun flagCountFor(durationBeats: Float): Int {
    val tol = 0.15f
    return when {
        kotlin.math.abs(durationBeats - 0.5f) < tol -> 1
        kotlin.math.abs(durationBeats - 0.75f) < tol -> 1
        kotlin.math.abs(durationBeats - 0.25f) < tol -> 2
        kotlin.math.abs(durationBeats - 0.375f) < tol -> 2
        kotlin.math.abs(durationBeats - 0.125f) < tol -> 3
        else -> 0
    }
}

private fun isDotted(durationBeats: Float): Boolean {
    val tol = 0.1f
    return kotlin.math.abs(durationBeats - 1.5f) < tol ||
            kotlin.math.abs(durationBeats - 3.0f) < tol ||
            kotlin.math.abs(durationBeats - 0.75f) < tol ||
            kotlin.math.abs(durationBeats - 0.375f) < tol
}

private fun DrawScope.drawNoteGroup(
    notes: List<TabNote>,
    rowStartMs: Long,
    firstStringY: Float,
    stringGap: Float,
    rowBottomY: Float,
    leftPad: Float,
    pxPerMs: Float,
    halfBeat: Float,
    flags: Int,
    isHighlighted: Boolean,
    beadColor: Color
) {
    val color = Color(0xFFE5E7EB)
    val stemW = 3f
    val beamW = stemW * 2f

    // Зона штилей: начинается с отступом = stringGap от нижней (E) струны
    val stemTopY = firstStringY + 3 * stringGap + stringGap
    val stemBottomY = rowBottomY - stringGap * 0.4f

    val noteXs = notes.map { note ->
        leftPad + (note.timeMs - rowStartMs) * pxPerMs + halfBeat
    }

    // Штили
    notes.forEach { note ->
        val x = leftPad + (note.timeMs - rowStartMs) * pxPerMs + halfBeat
        drawLine(
            color = color,
            start = Offset(x, stemTopY),
            end = Offset(x, stemBottomY),
            strokeWidth = stemW
        )
    }

    if (flags > 0) {
        if (notes.size == 1) {
            // Одиночная нота — флажок-крючок
            val x = noteXs.first()
            for (i in 0 until flags) {
                val fy = stemBottomY - i * 7f
                val path = Path().apply {
                    moveTo(x, fy)
                    cubicTo(x + 7f, fy + 4f, x + 9f, fy + 9f, x + 6f, fy + 14f)
                }
                drawPath(path, color, style = Stroke(width = stemW))
            }
        } else {
            // Группа нот — рёбра
            val minX = noteXs.min()
            val maxX = noteXs.max()
            for (i in 0 until flags) {
                val by = stemBottomY - i * 7f
                drawLine(
                    color = color,
                    start = Offset(minX, by),
                    end = Offset(maxX, by),
                    strokeWidth = beamW
                )
            }
        }
    }
}