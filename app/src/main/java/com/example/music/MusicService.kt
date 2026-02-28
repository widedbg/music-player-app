package com.example.music

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MusicService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var currentSong: Any? = null
    private val channelId = "music_channel"

    private val binder = MusicBinder()

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun playSong(song: Any) {
        if (currentSong == song && mediaPlayer?.isPlaying == true) return

        mediaPlayer?.stop()
        mediaPlayer?.release()

        mediaPlayer = when (song) {
            is Int -> MediaPlayer.create(this, song)
            is String -> {
                try {
                    MediaPlayer().apply {
                        setDataSource(song)
                        prepare()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            else -> return
        }

        mediaPlayer?.apply {
            isLooping = false
            start()
            setOnCompletionListener { /* Handle completion, e.g., play next */ }
        }
        currentSong = song

        startForeground(1, buildNotification())
    }

    fun pauseSong() {
        mediaPlayer?.pause()
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
    }

    fun getDuration(): Int = mediaPlayer?.duration ?: 0
    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0
    fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Music Player")
            .setContentText("Playing music")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "Music Playback",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
