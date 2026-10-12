package com.tabplayer

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot() {
    var screen by remember { mutableStateOf("player") }
    var selectedVideoId by remember { mutableStateOf<String?>(null) }

    if (screen == "search") {
        SearchScreen(
            onBack = { screen = "player" },
            onSelect = { videoId ->
                selectedVideoId = videoId
                screen = "player"
            }
        )
    } else {
        PlayerScreen(
            videoIdToLoad = selectedVideoId,
            onOpenSearch = { screen = "search" }
        )
    }
}

@Composable
fun SearchScreen(onBack: () -> Unit, onSelect: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Назад") }
            Spacer(Modifier.width(8.dp))
            Text("Поиск трека", fontSize = 20.sp)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Название или исполнитель") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                if (query.length < 2) return@Button
                loading = true
                errorText = null
                scope.launch {
                    try {
                        results = ApiClient.search(query)
                        if (results.isEmpty()) errorText = "Ничего не найдено"
                    } catch (e: Exception) {
                        errorText = e.message
                    } finally {
                        loading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Найти") }

        Spacer(Modifier.height(12.dp))

        if (loading) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFF4081))
            }
        }

        errorText?.let { Text(it, color = Color.Red) }

        LazyColumn(Modifier.fillMaxSize()) {
            items(results) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSelect(item.video_id) }
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(item.title, fontSize = 16.sp)
                        item.uploader?.let { Text(it, fontSize = 12.sp, color = Color.Gray) }
                        item.duration?.let {
                            Text("${it / 60}:${(it % 60).toString().padStart(2, '0')}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerScreen(videoIdToLoad: String?, onOpenSearch: () -> Unit) {
    val context = LocalContext.current
    val player = remember { AudioPlayer(context) }

    var track by remember { mutableStateOf<TabTrack?>(null) }
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(1L) }
    var playing by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        track = TabRepository.load(context, "sample_tab.json")
    }

    LaunchedEffect(videoIdToLoad) {
        if (videoIdToLoad != null) {
            loading = true
            errorText = null
            try {
                track = ApiClient.transcribeByVideoId(videoIdToLoad)
            } catch (e: Exception) {
                errorText = e.message
            } finally {
                loading = false
            }
        }
    }

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
        Text(track?.title ?: "Нет трека", fontSize = 20.sp)
        track?.let {
            Text("Строй: ${it.tuning.joinToString(" ")}  •  BPM: ${it.bpm}", fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))

        Box(Modifier.weight(1f)) {
            track?.let { TabCanvas(it, pos, Modifier.fillMaxSize()) }
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFFFF4081))
                        Spacer(Modifier.height(12.dp))
                        Text("Разбираем трек...")
                    }
                }
            }
            errorText?.let { err ->
                Text(err, color = Color.Red, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }

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
            Button(onClick = { picker.launch(arrayOf("audio/*")) }) { Text("Файл") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onOpenSearch) { Text("Поиск") }
        }
    }
}
