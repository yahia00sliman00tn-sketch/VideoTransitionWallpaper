package com.videowallpaper

import android.app.WallpaperColors
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class VideoWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return VideoEngine()
    }

    inner class VideoEngine : Engine() {

        private var mediaPlayer: MediaPlayer? = null
        private var isReady = false
        private val handler = Handler(Looper.getMainLooper())

        private val unlockReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_USER_PRESENT -> playVideo()
                    Intent.ACTION_SCREEN_OFF -> showFirstFrame()
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(false)
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            registerReceiver(unlockReceiver, filter)
        }

        override fun onDestroy() {
            super.onDestroy()
            unregisterReceiver(unlockReceiver)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            setupMediaPlayer(holder)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            releasePlayer()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            // BroadcastReceiver يتحكم بكل شيء
        }

        override fun onComputeColors(): WallpaperColors {
            val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
            val colorInt = prefs.getInt("accent_color", Color.parseColor("#6200EE"))
            return WallpaperColors(Color.valueOf(colorInt), null, null)
        }

        private fun setupMediaPlayer(holder: SurfaceHolder) {
            val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
            val uriString = prefs.getString(MainActivity.PREF_VIDEO_URI, null) ?: return
            try {
                val uri = Uri.parse(uriString)
                val player = MediaPlayer()
                player.setSurface(holder.surface)
                player.setDataSource(applicationContext, uri)
                player.isLooping = false
                player.setOnPreparedListener { mp ->
                    isReady = true
                    mp.seekTo(0)
                    mp.start()
                    mp.pause()
                }
                player.setOnCompletionListener { mp ->
                    mp.seekTo(mp.duration)
                }
                player.prepareAsync()
                mediaPlayer = player
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun playVideo() {
            handler.post {
                try {
                    val player = mediaPlayer ?: return@post
                    if (!isReady) return@post
                    player.seekTo(0)
                    player.start()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        private fun showFirstFrame() {
            handler.post {
                try {
                    val player = mediaPlayer ?: return@post
                    if (!isReady) return@post
                    player.pause()
                    player.seekTo(0)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        private fun releasePlayer() {
            mediaPlayer?.release()
            mediaPlayer = null
            isReady = false
        }
    }
}
