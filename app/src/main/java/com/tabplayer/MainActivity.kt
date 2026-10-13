package com.tabplayer

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.SliderDefaults

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
            trackIdToLoad = selectedVideoId,
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
                CircularProgressIndicator(color = Color(0xFFF59E0B))
            }
        }

        errorText?.let { Text(it, color = Color.Red) }

        LazyColumn(Modifier.fillMaxSize()) {
            items(results) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSelect(item.track_id) }
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(item.title, fontSize = 16.sp)
                        item.artist?.let { Text(it, fontSize = 12.sp, color = Color.Gray) }
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
fun PlayerScreen(trackIdToLoad: String?, onOpenSearch: () -> Unit) {
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

    LaunchedEffect(trackIdToLoad) {
        if (trackIdToLoad != null) {
            loading = true
            errorText = null
            try {
                track = ApiClient.transcribeByTrackId(trackIdToLoad)
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
    ) { uri: Uri? ->
        uri?.let {
            player.load(it)
            loading = true
            errorText = null
            scope.launch {
                try {
                    val newTrack = ApiClient.transcribeFile(context, it)
                    track = newTrack
                } catch (e: Exception) {
                    errorText = e.message ?: "Ошибка загрузки"
                } finally {
                    loading = false
                }
            }
        }
    }

        Spacer(Modifier.height(12.dp))
            Slider(
                value = pos.toFloat(),
                onValueChange = { player.seekTo(it.toLong()) },
                valueRange = 0f..dur.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFF59E0B),
                    activeTrackColor = Color(0xFFF59E0B),
                    inactiveTrackColor = Color(0xFF2A2E36)
                )

        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = { picker.launch(arrayOf("audio/*")) },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color(0xFF1E2128),
                    contentColor = Color(0xFF9CA3AF)
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Файл")
            }
            FilledIconButton(
                onClick = { if (playing) player.pause() else player.play() },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color(0xFFF59E0B),
                    contentColor = Color(0xFF13151A)
                ),
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Играть",
                    modifier = Modifier.size(32.dp)
                )
            }
            FilledIconButton(
                onClick = onOpenSearch,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color(0xFF1E2128),
                    contentColor = Color(0xFFF59E0B)
                )
            ) {
                Icon(Icons.Default.Search, contentDescription = "Поиск")
            }
        }

    }

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun PlayerScreenPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        val sampleTrack = TabTrack(
            title = "Смех — Я бегу туда же",
            bpm = 160,
            notes = listOf(
                TabNote(0, 3, 4, 180),
                TabNote(180, 3, 4, 180),
                TabNote(360, 3, 5, 180),
                TabNote(540, 3, 5, 180),
                TabNote(720, 4, 3, 180),
                TabNote(900, 4, 3, 180)
            )
        )
        Column(Modifier.fillMaxSize().background(Color(0xFF13151A)).padding(all = 16.dp)) {
            Text(
                sampleTrack.title,
                fontSize = 24.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = Color(0xFFFFFFFF)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Строй: ${sampleTrack.tuning.joinToString(" ")}  •  BPM: ${sampleTrack.bpm}",
                fontSize = 12.sp,
                color = Color(0xFF888888)
            )
            Spacer(Modifier.height(12.dp))
            TabCanvas(sampleTrack, 500L, Modifier.weight(1f))
            Spacer(Modifier.height(12.dp))
            Slider(
                value = 0.3f,
                onValueChange = {},
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFF59E0B),
                    activeTrackColor = Color(0xFFF59E0B),
                    inactiveTrackColor = Color(0xFF2A2E36)
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = {},
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFF1E2128),
                        contentColor = Color(0xFF9CA3AF)
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Файл")
                }
                FilledIconButton(
                    onClick = {},
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFFF59E0B),
                        contentColor = Color(0xFF13151A)
                    ),
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Играть", modifier = Modifier.size(32.dp))
                }
                FilledIconButton(
                    onClick = {},
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFF1E2128),
                        contentColor = Color(0xFFF59E0B)
                    )
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Поиск")
                }
            }
        }
    }
}
