package com.videowallpaper

import android.media.MediaPlayer
import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.os.Handler
import android.os.Looper
import android.app.KeyguardManager

class VideoWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return VideoEngine()
    }

    inner class VideoEngine : Engine() {

        private var mediaPlayer: MediaPlayer? = null
        private var wasLocked = true
        private var isReady = false
        private val handler = Handler(Looper.getMainLooper())

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(false)
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
            super.onVisibilityChanged(visible)
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            val isLocked = keyguardManager.isKeyguardLocked
            if (!visible && isLocked) {
                wasLocked = true
                showFirstFrame()
            } else if (visible && wasLocked && !isLocked) {
                wasLocked = false
                playVideo()
            }
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
