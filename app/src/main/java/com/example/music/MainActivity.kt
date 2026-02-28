package com.example.music

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.music.ui.theme.MusicTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private var musicService: MusicService? = null
    private var isBound by mutableStateOf(false)

    private val songs = mutableStateListOf<Any>(R.raw.song1, R.raw.song2, R.raw.song3, R.raw.song4, R.raw.song5, R.raw.song6, R.raw.song7, R.raw.song8)
    private val songTitles = mutableStateListOf("Ordinary", "Woman", "NINAO", "Song 4", "Song 5", "Song 6", "Song 7", "Song 8")
    private val artistNames = mutableStateListOf("Alex Warren", "Doja Cat", "GIMS", "Artist 4", "Artist 5", "Artist 6", "Artist 7", "Artist 8")

    private var showFetchedTracks by mutableStateOf(false)
    private var fetchedTracks by mutableStateOf<List<Track>>(emptyList())
    private val downloadingTracks = mutableStateListOf<String>()

    private var currentSong by mutableStateOf<Any>(songs.first())
    private var isPlaying by mutableStateOf(false)
    private var currentPosition by mutableStateOf(0)
    private var songDuration by mutableStateOf(0)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val musicBinder = binder as MusicService.MusicBinder
            musicService = musicBinder.getService()
            isBound = true
            updateSongDuration()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        loadDownloadedSongs()

        Intent(this, MusicService::class.java).also { intent ->
            startService(intent)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

        setContent {
            LaunchedEffect(isPlaying) {
                if (isPlaying) {
                    while (true) {
                        currentPosition = musicService?.getCurrentPosition() ?: 0
                        delay(500L)
                    }
                }
            }

            MusicTheme {
                SpotifyMusicScreen(
                    songs = songs,
                    songTitles = songTitles,
                    artistNames = artistNames,
                    showFetchedTracks = showFetchedTracks,
                    fetchedTracks = fetchedTracks,
                    downloadingTracks = downloadingTracks,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    songDuration = songDuration,
                    onPlaySong = ::playSong,
                    onTogglePlayPause = ::togglePlayPause,
                    onNextSong = ::nextSong,
                    onPrevSong = ::prevSong,
                    onSeekTo = ::seekTo,
                    onFetchTracks = { fetchTracks() },
                    onDownloadTrack = { track -> downloadTrack(track) },
                    onHideTracks = { showFetchedTracks = false }
                )
            }
        }
    }

    private fun loadDownloadedSongs() {
        val musicDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        musicDir?.listFiles()?.forEach { file ->
            if (file.extension == "mp3") {
                songs.add(file.absolutePath)
                songTitles.add(file.nameWithoutExtension.replace("_", " "))
                artistNames.add("Downloaded")
            }
        }
    }

    private fun playSong(song: Any) {
        if (!isBound) return
        musicService?.playSong(song)
        currentSong = song
        isPlaying = true
        updateSongDuration()
    }

    private fun pauseSong() {
        if (!isBound) return
        musicService?.pauseSong()
        isPlaying = false
    }

    private fun togglePlayPause() {
        if (isPlaying) {
            pauseSong()
        } else {
            playSong(currentSong)
        }
    }

    private fun nextSong() {
        val nextIndex = (songs.indexOf(currentSong) + 1) % songs.size
        playSong(songs[nextIndex])
    }

    private fun prevSong() {
        val prevIndex = if (songs.indexOf(currentSong) - 1 < 0) {
            songs.size - 1
        } else {
            songs.indexOf(currentSong) - 1
        }
        playSong(songs[prevIndex])
    }

    private fun seekTo(position: Int) {
        if (!isBound) return
        musicService?.seekTo(position)
        currentPosition = position
    }

    private fun updateSongDuration() {
        lifecycleScope.launch {
            delay(100)
            if (!isBound) return@launch
            val duration = musicService?.getDuration() ?: 0
            if (duration > 0) {
                songDuration = duration
            }
        }
    }

    private fun fetchTracks() {
        lifecycleScope.launch {
            try {
                fetchedTracks = AudiusApi.retrofitService.getTrendingTracks().data
                showFetchedTracks = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun downloadTrack(track: Track) {
        if (downloadingTracks.contains(track.id)) return

        lifecycleScope.launch {
            try {
                downloadingTracks.add(track.id)
                withContext(Dispatchers.IO) {
                    val client = OkHttpClient()
                    val request = Request.Builder().url("https://api.audius.co/v1/tracks/${track.id}/stream").build()
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val musicDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                        val sanitizedTitle = track.title.replace(Regex("[^a-zA-Z0-9.-]"), "_")
                        val file = File(musicDir, "$sanitizedTitle.mp3")
                        FileOutputStream(file).use { fos ->
                            fos.write(response.body?.bytes())
                        }

                        withContext(Dispatchers.Main) {
                            songs.add(file.absolutePath)
                            songTitles.add(track.title)
                            artistNames.add(track.user.name)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                downloadingTracks.remove(track.id)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
        }
    }
}

@Composable
fun SpotifyMusicScreen(
    songs: List<Any>,
    songTitles: List<String>,
    artistNames: List<String>,
    showFetchedTracks: Boolean,
    fetchedTracks: List<Track>,
    downloadingTracks: List<String>,
    currentSong: Any,
    isPlaying: Boolean,
    currentPosition: Int,
    songDuration: Int,
    onPlaySong: (Any) -> Unit,
    onTogglePlayPause: () -> Unit,
    onNextSong: () -> Unit,
    onPrevSong: () -> Unit,
    onSeekTo: (Int) -> Unit,
    onFetchTracks: () -> Unit,
    onDownloadTrack: (Track) -> Unit,
    onHideTracks: () -> Unit
) {
    val currentSongIndex = songs.indexOf(currentSong)
    val currentSongTitle = songTitles.getOrElse(currentSongIndex) { "Unknown Title" }
    val currentArtistName = artistNames.getOrElse(currentSongIndex) { "Unknown Artist" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF121212), Color(0xFF1E1E1E))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(250.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.DarkGray)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = currentSongTitle, color = Color.White, fontSize = 24.sp)
            Text(text = currentArtistName, color = Color.LightGray, fontSize = 16.sp)

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onFetchTracks) {
                Text("Fetch Trending Tracks")
            }

            if (showFetchedTracks) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Trending Tracks", color = Color.White, fontSize = 20.sp)
                    IconButton(onClick = onHideTracks) {
                        Icon(Icons.Default.Close, contentDescription = "Hide Tracks", tint = Color.White)
                    }
                }

                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(fetchedTracks) { track ->
                        val isDownloading = downloadingTracks.contains(track.id)
                        val isDownloaded = songTitles.contains(track.title)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.title, color = Color.White)
                                Text(track.user.name, color = Color.LightGray, fontSize = 12.sp)
                            }
                            Box(modifier = Modifier.size(width = 120.dp, height = 48.dp), contentAlignment = Alignment.Center) {
                                when {
                                    isDownloading -> {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Cyan)
                                    }
                                    isDownloaded -> {
                                        Text("Downloaded", color = Color.Green)
                                    }
                                    else -> {
                                        Button(onClick = { onDownloadTrack(track) }) {
                                            Text("Download")
                                        }
                                    }
                                }
                            }
                        }
                        Divider(color = Color.Gray)
                    }
                }
            }

            Text("My Playlist", color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(top = 16.dp))
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(songs) { song ->
                    val isCurrent = song == currentSong
                    val index = songs.indexOf(song)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlaySong(song) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isCurrent) Color.Cyan else Color.DarkGray)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = songTitles.getOrElse(index) { "Unknown" },
                                color = if (isCurrent) Color.Cyan else Color.White
                            )
                            Text(
                                text = artistNames.getOrElse(index) { "Unknown" },
                                color = Color.LightGray, fontSize = 12.sp
                            )
                        }
                    }
                    Divider(color = Color.Gray)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { position -> onSeekTo(position.toInt()) },
                    valueRange = 0f..(songDuration.toFloat().coerceAtLeast(1f)),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Cyan,
                        activeTrackColor = Color.Cyan,
                        inactiveTrackColor = Color.Gray
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${currentPosition / 1000}s", color = Color.LightGray, fontSize = 12.sp)
                    Text("${songDuration / 1000}s", color = Color.LightGray, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPrevSong) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Prev", tint = Color.Cyan, modifier = Modifier.size(48.dp))
                    }
                    IconButton(onClick = onTogglePlayPause) {
                        Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = "Play/Pause", tint = Color.Cyan, modifier = Modifier.size(64.dp))
                    }
                    IconButton(onClick = onNextSong) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.Cyan, modifier = Modifier.size(48.dp))
                    }
                }
            }
        }
    }
}
