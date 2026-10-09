package com.tabplayer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp

@Composable
fun TabCanvas(track: TabTrack, positionMs: Long, modifier: Modifier = Modifier) {
    val tm = rememberTextMeasurer()
    val noteStyle = TextStyle(color = Color.White, fontSize = 18.sp, fontFamily = FontFamily.Monospace)
    val labelStyle = TextStyle(color = Color(0xFF66FF99), fontSize = 12.sp, fontFamily = FontFamily.Monospace)

    Box(modifier.fillMaxWidth().background(Color(0xFF101010))) {
        Canvas(Modifier.fillMaxSize()) {
            val step = size.height / 5f
            val startY = step

            for (i in 0..3) {
                val y = startY + i * step
                drawLine(Color.Gray, Offset(60f, y), Offset(size.width, y), strokeWidth = 2f)
                drawText(tm, track.tuning[3 - i], Offset(12f, y - 22f), labelStyle)
            }

            val windowStart = positionMs - 500
            val windowEnd = positionMs + 1500
            val pxPerMs = (size.width - 60f) / 2000f

            track.notes.forEach { note ->
                if (note.timeMs in windowStart..windowEnd) {
                    val x = 60f + (note.timeMs - windowStart) * pxPerMs
                    val y = startY + (4 - note.string) * step
                    drawText(tm, note.fret.toString(), Offset(x, y - 22f), noteStyle)
                }
            }

            val playX = 60f + 500f * pxPerMs
            drawLine(Color(0xFFFF4081), Offset(playX, 0f), Offset(playX, size.height), strokeWidth = 4f)
        }
    }
}
