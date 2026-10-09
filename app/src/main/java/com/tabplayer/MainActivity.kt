package com.tabplayer

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                PlayerScreen()
            }
        }
    }
}

@Composable
fun PlayerScreen() {
    val context = LocalContext.current
    val player = remember { AudioPlayer(context) }
    val track = remember { TabRepository.load(context, "sample_tab.json") }

    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(1L) }
    var playing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            pos = player.currentPosition()
            dur = player.duration().coerceAtLeast(1L)
            playing = player.isPlaying()
            delay(33)
        }
    }

    DisposableEffect(Unit) { onDispose { player.release() } }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { player.load(it) } }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(track.title, fontSize = 20.sp)
        Text("Строй: ${track.tuning.joinToString(" ")}  •  BPM: ${track.bpm}", fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        TabCanvas(track, pos, Modifier.weight(1f))
        Spacer(Modifier.height(12.dp))
        Slider(
            value = pos.toFloat(),
            onValueChange = { player.seekTo(it.toLong()) },
            valueRange = 0f..dur.toFloat()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${pos / 1000}s / ${dur / 1000}s", fontSize = 12.sp)
            Spacer(Modifier.width(12.dp))
            Button(onClick = { if (playing) player.pause() else player.play() }) {
                Text(if (playing) "Пауза" else "Играть")
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { picker.launch(arrayOf("audio/*")) }) {
                Text("Трек")
            }
        }
    }
}
