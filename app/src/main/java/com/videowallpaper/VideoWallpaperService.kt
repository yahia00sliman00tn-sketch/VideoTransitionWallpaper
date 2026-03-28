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

    companion object {
        const val ACTION_RELOAD = "com.videowallpaper.RELOAD"
        const val PREF_NAME = "wallpaper_prefs"
        const val KEY_COLOR = "accent_color"
        const val KEY_DUAL_MODE = "dual_mode"
        const val KEY_SINGLE_VIDEO = "single_video"
        const val KEY_SINGLE_TARGET = "single_target"
        const val KEY_VIDEO_LOCK = "video_lock"
        const val KEY_VIDEO_HOME = "video_home"
        const val PREF_VIDEO_URI = "single_video"
    }

    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {

        private var activePlayer: MediaPlayer? = null
        private var isLocked = true
        private var isDual = false
        private var singleTarget = "home"
        private var surface: SurfaceHolder? = null
        private val handler = Handler(Looper.getMainLooper())

        // URIs
        private var uriSingle: Uri? = null
        private var uriLock: Uri? = null
        private var uriHome: Uri? = null

        private val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_OFF   -> onScreenOff()
                    Intent.ACTION_SCREEN_ON    -> onScreenOn()
                    Intent.ACTION_USER_PRESENT -> onUnlocked()
                    ACTION_RELOAD              -> reload()
                }
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            setTouchEventsEnabled(false)
            registerReceiver(receiver, IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(ACTION_RELOAD)
            })
        }

        override fun onDestroy() {
            super.onDestroy()
            try { unregisterReceiver(receiver) } catch (e: Exception) {}
            releaseActive()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            holder.setSizeFromLayout()
            surface = holder
            loadUris()
            // اعرض الفريم الأول
            showFirstFrame()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            releaseActive()
            surface = null
        }

        override fun onVisibilityChanged(visible: Boolean) {}

        override fun onComputeColors(): WallpaperColors {
            val c = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                .getInt(KEY_COLOR, Color.parseColor("#6200EE"))
            return WallpaperColors(Color.valueOf(c), null, null)
        }

        // ======= أحداث =======

        private fun onScreenOff() {
            isLocked = true
            handler.removeCallbacksAndMessages(null)
            releaseActive()
            showFirstFrame()
        }

        private fun onScreenOn() {
            if (!isLocked) return
            if (isDual) {
                // وضع مزدوج — شغّل Lock video
                playUri(uriLock, thenHold = true)
            } else if (singleTarget == "lock") {
                // وضع واحد على Lock
                playUri(uriSingle, thenHold = true)
            }
        }

        private fun onUnlocked() {
            if (!isLocked) return
            isLocked = false
            handler.removeCallbacksAndMessages(null)
            if (isDual) {
                // وضع مزدوج — شغّل Home video
                releaseActive()
                playUri(uriHome, thenHold = true)
            } else if (singleTarget == "home") {
                // وضع واحد على Home
                releaseActive()
                playUri(uriSingle, thenHold = true)
            }
        }

        // ======= تشغيل =======

        private fun showFirstFrame() {
            val holder = surface ?: return
            val uri = when {
                isDual -> uriLock ?: uriHome
                else   -> uriSingle
            } ?: return

            handler.post {
                try {
                    val player = MediaPlayer().apply {
                        setSurface(holder.surface)
                        setDataSource(applicationContext, uri)
                        setOnPreparedListener { mp ->
                            mp.seekTo(0, MediaPlayer.SEEK_CLOSEST)
                            mp.start()
                            handler.postDelayed({ mp.pause() }, 80)
                        }
                        setOnErrorListener { _, _, _ -> false }
                        prepareAsync()
                    }
                    activePlayer = player
                } catch (e: Exception) {}
            }
        }

        private fun playUri(uri: Uri?, thenHold: Boolean) {
            val holder = surface ?: return
            if (uri == null) return

            handler.post {
                try {
                    releaseActive()
                    val player = MediaPlayer().apply {
                        setSurface(holder.surface)
                        setDataSource(applicationContext, uri)
                        isLooping = false
                        setOnPreparedListener { mp ->
                            mp.seekTo(0, MediaPlayer.SEEK_CLOSEST)
                            mp.start()
                        }
                        setOnCompletionListener { mp ->
                            // ثبّت على الفريم الأخير
                            mp.pause()
                        }
                        setOnErrorListener { _, _, _ -> false }
                        prepareAsync()
                    }
                    activePlayer = player
                } catch (e: Exception) {}
            }
        }

        private fun releaseActive() {
            try {
                activePlayer?.stop()
                activePlayer?.reset()
                activePlayer?.release()
            } catch (e: Exception) {}
            activePlayer = null
        }

        private fun loadUris() {
            val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
            isDual = prefs.getBoolean(KEY_DUAL_MODE, false)
            singleTarget = prefs.getString(KEY_SINGLE_TARGET, "home") ?: "home"
            uriSingle = prefs.getString(KEY_SINGLE_VIDEO, null)?.let { Uri.parse(it) }
            uriLock   = prefs.getString(KEY_VIDEO_LOCK, null)?.let { Uri.parse(it) }
            uriHome   = prefs.getString(KEY_VIDEO_HOME, null)?.let { Uri.parse(it) }
        }

        private fun reload() {
            loadUris()
            releaseActive()
            showFirstFrame()
            notifyColorsChanged()
        }
    }
}
