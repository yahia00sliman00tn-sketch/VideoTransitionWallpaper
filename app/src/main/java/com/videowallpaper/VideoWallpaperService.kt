package com.videowallpaper

import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.Surface
import android.view.SurfaceHolder

class VideoWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return VideoEngine()
    }

    inner class VideoEngine : Engine() {

        private var mediaPlayer: MediaPlayer? = null
        private var isPlaying = false
        private var isLocked = true
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
            if (visible && isLocked) {
                // الهاتف فُتح — شغّل الفيديو مرة واحدة
                isLocked = false
                playVideo()
            } else if (!visible) {
                // الهاتف أُقفل — أعد للفريم الأول
                isLocked = true
                resetToFirstFrame()
            }
        }

        private fun setupMediaPlayer(holder: SurfaceHolder) {
            val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
            val uriString = prefs.getString(MainActivity.PREF_VIDEO_URI, null) ?: return

            try {
                val uri = Uri.parse(uriString)
                mediaPlayer = MediaPlayer().apply {
                    val surface = Surface(createSurfaceTexture(holder))
                    setSurface(surface)
                    setDataSource(applicationContext, uri)
                    isLooping = false
                    prepare()
                    // اعرض الفريم الأول فوراً
                    seekTo(0)
                    setOnCompletionListener {
                        // الفيديو انتهى — ثبّت على آخر فريم
                        isPlaying = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun createSurfaceTexture(holder: SurfaceHolder): SurfaceTexture {
            // نستخدم SurfaceHolder مباشرة
            return SurfaceTexture(0)
        }

        private fun playVideo() {
            handler.post {
                try {
                    mediaPlayer?.let { player ->
                        if (!isPlaying) {
                            player.seekTo(0)
                            player.start()
                            isPlaying = true
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        private fun resetToFirstFrame() {
            handler.post {
                try {
                    mediaPlayer?.let { player ->
                        if (isPlaying) {
                            player.pause()
                            isPlaying = false
                        }
                        player.seekTo(0)
                    }
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
