package com.tabplayer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
        color = Color(0xFF9CA3AF), fontSize = 14.sp,
        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
    )
    val highlightedNoteStyle = TextStyle(
        color = Color(0xFFFFFFFF), fontSize = 17.sp,
        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
    )
    val labelStyle = TextStyle(
        color = Color(0xFF6B7280), fontSize = 11.sp,
        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
    )

    val bgColor = Color(0xFF13151A)
    val stringColor = Color(0xFF2A2E36)
    val measureColor = Color(0xFF424852)
    val beadColor = Color(0xFFF59E0B)

    val bpm = track.bpm.coerceAtLeast(1)
    val timePerBeat = 60000f / bpm
    val beatsPerMeasure = 4
    val measuresPerRow = 2
    val beatsPerRow = beatsPerMeasure * measuresPerRow
    val timePerRow = beatsPerRow * timePerBeat
    val rowsVisible = 4

    Box(modifier.fillMaxWidth().background(bgColor)) {
        Canvas(Modifier.fillMaxSize()) {
            val leftPad = 46f
            val rightPad = 16f
            val topPad = 4f
            val bottomPad = 4f
            val usableWidth = size.width - leftPad - rightPad
            val rowHeight = (size.height - topPad - bottomPad) / rowsVisible
            val stringGap = rowHeight / 5.8f
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

            // Строки: струны + границы тактов
            for (displayRow in 0 until rowsVisible) {
                val rowTop = topPad + displayRow * rowHeight
                val firstStringY = rowTop + stringGap * 1.2f

                for (i in 0..3) {
                    val y = firstStringY + i * stringGap
                    // Тень
                    drawLine(
                        color = Color(0x55000000),
                        start = Offset(leftPad, y + 1.5f),
                        end = Offset(size.width - rightPad, y + 1.5f),
                        strokeWidth = 2f
                    )
                    // Струна
                    drawLine(
                        color = stringColor,
                        start = Offset(leftPad, y),
                        end = Offset(size.width - rightPad, y),
                        strokeWidth = 1.5f
                    )
                    drawText(tm, track.tuning[3 - i], Offset(10f, y - 16f), labelStyle)
                }

                // Жирные границы тактов
                for (beat in 0..beatsPerRow step beatsPerMeasure) {
                    val x = leftPad + beat * beatWidth
                    val lineTop = firstStringY - 6f
                    val lineBottom = firstStringY + 3 * stringGap + 6f
                    drawLine(
                        color = measureColor,
                        start = Offset(x, lineTop),
                        end = Offset(x, lineBottom),
                        strokeWidth = 2f
                    )
                }
            }

            // Ноты — со смещением в середину доли и разрывом струны
            for (displayRow in 0 until rowsVisible) {
                val actualRow = scrollOffset + displayRow
                val rowTop = topPad + displayRow * rowHeight
                val firstStringY = rowTop + stringGap * 1.2f
                val rowStartMs = (actualRow * timePerRow).toLong()
                val rowEndMs = rowStartMs + timePerRow.toLong()

                track.notes.forEach { note ->
                    if (note.timeMs in rowStartMs until rowEndMs) {
                        val relTime = note.timeMs - rowStartMs
                        // Смещаем на пол-доли, чтобы ноты не стояли на границах
                        val x = leftPad + relTime * pxPerMs + halfBeat
                        val y = firstStringY + (4 - note.string) * stringGap
                        val isHighlighted = displayRow == playheadDisplayRow &&
                                x >= highlightMin && x <= highlightMax

                        val style = if (isHighlighted) highlightedNoteStyle else noteStyle
                        val layout = tm.measure(AnnotatedString(note.fret.toString()), style)
                        val textW = layout.size.width.toFloat()
                        val textH = layout.size.height.toFloat()

                        // Разрыв струны под нотой
                        drawRect(
                            color = bgColor,
                            topLeft = Offset(x - textW / 2f - 2f, y - textH / 2f - 1f),
                            size = Size(textW + 4f, textH + 2f)
                        )

                        // Цифра — точно по центру линии
                        drawText(
                            layout,
                            topLeft = Offset(x - textW / 2f, y - textH / 2f)
                        )
                    }
                }
            }

            // Курсор-медиатор
            if (playheadDisplayRow in 0 until rowsVisible) {
                val rowTop = topPad + playheadDisplayRow * rowHeight
                val rowBottom = rowTop + rowHeight
                val pw = playheadHalfWidth
                val topY = rowTop + 8f
                val arcBottom = topY + pw
                val pointY = rowBottom - 8f

                val path = Path().apply {
                    moveTo(playX, topY)
                    cubicTo(
                        playX + pw * 0.55f, topY,
                        playX + pw, topY + pw * 0.45f,
                        playX + pw, arcBottom
                    )
                    lineTo(playX + pw * 0.42f, pointY - 16f)
                    lineTo(playX, pointY)
                    lineTo(playX - pw * 0.42f, pointY - 16f)
                    lineTo(playX - pw, arcBottom)
                    cubicTo(
                        playX - pw, topY + pw * 0.45f,
                        playX - pw * 0.55f, topY,
                        playX, topY
                    )
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