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
        // وضع واحد
        const val KEY_SINGLE_VIDEO = "single_video"
        const val KEY_SINGLE_TARGET = "single_target" // "lock" or "home"
        // وضع مزدوج
        const val KEY_VIDEO_LOCK = "video_lock"
        const val KEY_VIDEO_HOME = "video_home"
        // للتوافق مع الكود القديم
        const val PREF_VIDEO_URI = "single_video"
    }

    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {

        private var playerLock: MediaPlayer? = null
        private var playerHome: MediaPlayer? = null
        private var readyLock = false
        private var readyHome = false
        private var durLock = 0
        private var durHome = 0

        private var isLocked = true
        private var isDual = false
        private var singleTarget = "home" // "lock" or "home"
        private var surface: SurfaceHolder? = null
        private val handler = Handler(Looper.getMainLooper())

        private val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_OFF    -> onScreenOff()
                    Intent.ACTION_SCREEN_ON     -> onScreenOn()
                    Intent.ACTION_USER_PRESENT  -> onUnlocked()
                    ACTION_RELOAD               -> reload()
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
            releasePlayers()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            holder.setSizeFromLayout()
            surface = holder
            setupPlayers(holder)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            releasePlayers()
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
            stopAll()
            seekToStart(playerLock, readyLock)
            seekToStart(playerHome, readyHome)
        }

        private fun onScreenOn() {
            if (!isLocked) return
            if (isDual) {
                // وضع مزدوج — فيديو Lock يشتغل فور إضاءة الشاشة
                play(playerLock, readyLock, durLock)
            } else if (singleTarget == "lock") {
                // وضع واحد على Lock
                play(playerLock, readyLock, durLock)
            }
        }

        private fun onUnlocked() {
            if (!isLocked) return
            isLocked = false
            if (isDual) {
                // وضع مزدوج — أوقف Lock وشغّل Home
                stopPlayer(playerLock)
                play(playerHome, readyHome, durHome)
            } else if (singleTarget == "home") {
                // وضع واحد على Home
                play(playerLock, readyLock, durLock)
            }
        }

        // ======= إعداد =======

        private fun setupPlayers(holder: SurfaceHolder) {
            val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
            isDual = prefs.getBoolean(KEY_DUAL_MODE, false)
            singleTarget = prefs.getString(KEY_SINGLE_TARGET, "home") ?: "home"

            releasePlayers()

            if (isDual) {
                // وضع مزدوج
                val uriLock = prefs.getString(KEY_VIDEO_LOCK, null)
                val uriHome = prefs.getString(KEY_VIDEO_HOME, null)
                if (uriLock != null) playerLock = build(Uri.parse(uriLock), holder, true)
                if (uriHome != null) playerHome = build(Uri.parse(uriHome), holder, false)
            } else {
                // وضع واحد
                val uri = prefs.getString(KEY_SINGLE_VIDEO, null)
                if (uri != null) playerLock = build(Uri.parse(uri), holder, true)
            }
        }

        private fun build(uri: Uri, holder: SurfaceHolder, isLock: Boolean): MediaPlayer? {
            return try {
                MediaPlayer().apply {
                    setSurface(holder.surface)
                    setDataSource(applicationContext, uri)
                    isLooping = false
                    setOnPreparedListener { mp ->
                        if (isLock) { durLock = mp.duration; readyLock = true }
                        else { durHome = mp.duration; readyHome = true }
                        mp.seekTo(0, MediaPlayer.SEEK_CLOSEST)
                        mp.start()
                        handler.postDelayed({ mp.pause() }, 80)
                    }
                    setOnCompletionListener { mp -> mp.pause() }
                    setOnErrorListener { _, _, _ ->
                        if (isLock) readyLock = false else readyHome = false
                        false
                    }
                    prepareAsync()
                }
            } catch (e: Exception) { null }
        }

        // ======= تحكم =======

        private fun play(player: MediaPlayer?, ready: Boolean, duration: Int) {
            handler.post {
                if (player == null || !ready) return@post
                try {
                    player.seekTo(0, MediaPlayer.SEEK_CLOSEST)
                    player.start()
                    val stop = (duration - 100).coerceAtLeast(0).toLong()
                    handler.postDelayed({
                        try { if (player.isPlaying) player.pause() } catch (e: Exception) {}
                    }, stop)
                } catch (e: Exception) {}
            }
        }

        private fun stopPlayer(p: MediaPlayer?) {
            try { p?.pause() } catch (e: Exception) {}
        }

        private fun seekToStart(p: MediaPlayer?, ready: Boolean) {
            handler.post {
                if (!ready) return@post
                try { p?.seekTo(0, MediaPlayer.SEEK_CLOSEST) } catch (e: Exception) {}
            }
        }

        private fun stopAll() {
            handler.removeCallbacksAndMessages(null)
            stopPlayer(playerLock)
            stopPlayer(playerHome)
        }

        private fun reload() {
            surface?.let { setupPlayers(it) }
            notifyColorsChanged()
        }

        private fun releasePlayers() {
            handler.removeCallbacksAndMessages(null)
            listOf(playerLock, playerHome).forEach {
                try { it?.stop(); it?.reset(); it?.release() } catch (e: Exception) {}
            }
            playerLock = null; playerHome = null
            readyLock = false; readyHome = false
            durLock = 0; durHome = 0
        }
    }
}
