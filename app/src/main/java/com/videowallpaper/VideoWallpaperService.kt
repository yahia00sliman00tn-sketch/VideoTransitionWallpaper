package com.videowallpaper

import android.media.MediaPlayer
import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.view.Surface
import android.view.SurfaceHolder
import android.os.Handler
import android.os.Looper

class VideoWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return VideoEngine()
    }

    inner class VideoEngine : Engine() {

        private var mediaPlayer: MediaPlayer? = null
        private var isVisible = false
        private var wasLocked = true
        private val handler = Handler(Looper.getMainLooper())
        private var surfaceHolder: SurfaceHolder? = null

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(false)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            this.surfaceHolder = holder
            setupMediaPlayer(holder)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            releasePlayer()
            this.surfaceHolder = null
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isVisible = visible
            if (visible && wasLocked) {
                wasLocked = false
                playVideo()
            } else if (!visible) {
                wasLocked = true
                resetToFirstFrame()
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
                player.setOnCompletionListener { mp ->
                    mp.seekTo(0)
                    mp.pause()
                }
                player.prepare()
                player.seekTo(0)
                mediaPlayer = player
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun playVideo() {
            handler.post {
                try {
                    val player = mediaPlayer ?: return@post
                    player.seekTo(0)
                    player.start()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        private fun resetToFirstFrame() {
            handler.post {
                try {
                    val player = mediaPlayer ?: return@post
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
        }
    }
}
