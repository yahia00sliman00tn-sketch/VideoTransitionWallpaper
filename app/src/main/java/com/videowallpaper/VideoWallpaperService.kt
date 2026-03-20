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

    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {

        private var mediaPlayer: MediaPlayer? = null
        private var isReady = false
        private var videoDuration = 0
        private val handler = Handler(Looper.getMainLooper())

        private val unlockReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_USER_PRESENT -> playVideo()
                    Intent.ACTION_SCREEN_OFF   -> showFirstFrame()
                }
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            setTouchEventsEnabled(false)
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            registerReceiver(unlockReceiver, filter)
        }

        override fun onDestroy() {
            super.onDestroy()
            try { unregisterReceiver(unlockReceiver) } catch (e: Exception) {}
            releasePlayer()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            holder.setSizeFromLayout()
            setupMediaPlayer(holder)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            releasePlayer()
        }

        override fun onVisibilityChanged(visible: Boolean) {}

        override fun onComputeColors(): WallpaperColors {
            val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
            val colorInt = prefs.getInt("accent_color", Color.parseColor("#6200EE"))
            return WallpaperColors(Color.valueOf(colorInt), null, null)
        }

        private fun setupMediaPlayer(holder: SurfaceHolder) {
            val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
            val uriString = prefs.getString(MainActivity.PREF_VIDEO_URI, null) ?: return
            releasePlayer()
            try {
                val uri = Uri.parse(uriString)
                val player = MediaPlayer().apply {
                    setSurface(holder.surface)
                    setDataSource(applicationContext, uri)
                    isLooping = false

                    setOnPreparedListener { mp ->
                        videoDuration = mp.duration
                        isReady = true
                        // اعرض الفريم الأول فوراً
                        mp.seekTo(0, MediaPlayer.SEEK_CLOSEST)
                        mp.start()
                        handler.postDelayed({ mp.pause() }, 80)
                    }

                    setOnCompletionListener { mp ->
                        // الحل الحقيقي: pause قبل نهاية الفيديو بـ 100ms
                        // بدل seekTo(duration) الذي يسبب الرجوع
                        mp.pause()
                    }

                    setOnErrorListener { _, _, _ ->
                        isReady = false
                        false
                    }
                    prepareAsync()
                }
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
                    player.seekTo(0, MediaPlayer.SEEK_CLOSEST)
                    player.start()

                    // نوقف الفيديو قبل نهايته بـ 100ms لتجنب الرجوع
                    val stopAt = (videoDuration - 100).coerceAtLeast(0).toLong()
                    handler.postDelayed({
                        try {
                            if (player.isPlaying) {
                                player.pause()
                            }
                        } catch (e: Exception) {}
                    }, stopAt)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        private fun showFirstFrame() {
            handler.removeCallbacksAndMessages(null)
            handler.post {
                try {
                    val player = mediaPlayer ?: return@post
                    if (!isReady) return@post
                    player.pause()
                    player.seekTo(0, MediaPlayer.SEEK_CLOSEST)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        private fun releasePlayer() {
            handler.removeCallbacksAndMessages(null)
            try {
                mediaPlayer?.stop()
                mediaPlayer?.reset()
                mediaPlayer?.release()
            } catch (e: Exception) {}
            mediaPlayer = null
            isReady = false
            videoDuration = 0
        }
    }
}
