package com.videowallpaper

import android.app.WallpaperColors
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.media.MediaCodec
import android.media.MediaFormat
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
        private var surfaceHolder: SurfaceHolder? = null
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
            // HDR/4K: نطلب أعلى جودة ممكنة للسطح
            holder.setSizeFromLayout()
            surfaceHolder = holder
            setupMediaPlayer(holder)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceHolder = holder
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            releasePlayer()
            surfaceHolder = null
        }

        override fun onVisibilityChanged(visible: Boolean) {
            // BroadcastReceiver يتحكم — لا شيء هنا
        }

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
                    // HDR/4K: Surface مباشرة بدون تحويل
                    setSurface(holder.surface)
                    setDataSource(applicationContext, uri)
                    isLooping = false

                    // أداء أعلى — تجهيز مسبق كامل
                    setOnPreparedListener { mp ->
                        isReady = true
                        // اعرض الفريم الأول فوراً بدون تشغيل
                        mp.seekTo(0, MediaPlayer.SEEK_CLOSEST)
                        mp.start()
                        // توقف فوري بعد microsecond لعرض الفريم
                        handler.postDelayed({ mp.pause() }, 50)
                    }

                    setOnCompletionListener { mp ->
                        // ثبّت على الفريم الأخير تماماً
                        mp.seekTo(mp.duration, MediaPlayer.SEEK_CLOSEST)
                    }

                    setOnErrorListener { _, _, _ ->
                        isReady = false
                        false
                    }

                    // تشغيل async لتجنب تأخير الـ UI thread
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
                    // بدون تأخير — seekTo ثم start فوراً
                    player.seekTo(0, MediaPlayer.SEEK_CLOSEST)
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
                    player.seekTo(0, MediaPlayer.SEEK_CLOSEST)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        private fun releasePlayer() {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.reset()
                mediaPlayer?.release()
            } catch (e: Exception) {}
            mediaPlayer = null
            isReady = false
        }
    }
}
