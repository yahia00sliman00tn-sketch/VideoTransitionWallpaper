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
        // مفاتيح الإعدادات
        const val PREF_NAME = "wallpaper_prefs"
        const val KEY_VIDEO_LOCK = "video_uri_lock"
        const val KEY_VIDEO_UNLOCK = "video_uri_unlock"
        const val KEY_DUAL_MODE = "dual_mode"
        const val KEY_COLOR = "accent_color"
    }

    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {

        // المشغلان
        private var playerA: MediaPlayer? = null  // فيديو Lock Screen
        private var playerB: MediaPlayer? = null  // فيديو Unlock
        private var readyA = false
        private var readyB = false
        private var durA = 0
        private var durB = 0

        // الحالة
        private var isLocked = true
        private var isDualMode = false
        private var holder: SurfaceHolder? = null
        private val handler = Handler(Looper.getMainLooper())

        private val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_OFF -> onScreenOff()
                    Intent.ACTION_SCREEN_ON  -> onScreenOn()
                    Intent.ACTION_USER_PRESENT -> onUnlocked()
                    ACTION_RELOAD -> reload()
                }
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            setTouchEventsEnabled(false)
            loadSettings()
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
            this.holder = holder
            setupPlayers(holder)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            releasePlayers()
            this.holder = null
        }

        override fun onVisibilityChanged(visible: Boolean) {}

        override fun onComputeColors(): WallpaperColors {
            val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
            val color = prefs.getInt(KEY_COLOR, Color.parseColor("#6200EE"))
            return WallpaperColors(Color.valueOf(color), null, null)
        }

        // ======= أحداث الشاشة =======

        private fun onScreenOff() {
            isLocked = true
            stopAll()
            seekAllToStart()
        }

        private fun onScreenOn() {
            if (isDualMode && isLocked) {
                // وضع مزدوج — شغّل فيديو A على شاشة القفل
                play(playerA, readyA, durA)
            }
        }

        private fun onUnlocked() {
            if (!isLocked) return
            isLocked = false
            if (isDualMode) {
                // وضع مزدوج — أوقف A وشغّل B
                stopPlayer(playerA)
                play(playerB, readyB, durB)
            } else {
                // وضع واحد — شغّل A عند الفتح
                play(playerA, readyA, durA)
            }
        }

        // ======= إعداد المشغلات =======

        private fun loadSettings() {
            val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
            isDualMode = prefs.getBoolean(KEY_DUAL_MODE, false)
        }

        private fun setupPlayers(holder: SurfaceHolder) {
            val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
            isDualMode = prefs.getBoolean(KEY_DUAL_MODE, false)

            releasePlayers()

            val uriA = prefs.getString(KEY_VIDEO_LOCK, null)
                ?: prefs.getString(MainActivity.PREF_VIDEO_URI, null)
            val uriB = prefs.getString(KEY_VIDEO_UNLOCK, null)

            if (uriA != null) {
                playerA = buildPlayer(Uri.parse(uriA), holder, isA = true)
            }
            if (isDualMode && uriB != null) {
                playerB = buildPlayer(Uri.parse(uriB), holder, isA = false)
            }
        }

        private fun buildPlayer(uri: Uri, holder: SurfaceHolder, isA: Boolean): MediaPlayer? {
            return try {
                MediaPlayer().apply {
                    setSurface(holder.surface)
                    setDataSource(applicationContext, uri)
                    isLooping = false
                    setOnPreparedListener { mp ->
                        if (isA) { durA = mp.duration; readyA = true }
                        else { durB = mp.duration; readyB = true }
                        mp.seekTo(0, MediaPlayer.SEEK_CLOSEST)
                        mp.start()
                        handler.postDelayed({ mp.pause() }, 80)
                    }
                    setOnCompletionListener { mp ->
                        mp.pause()
                    }
                    setOnErrorListener { _, _, _ ->
                        if (isA) readyA = false else readyB = false
                        false
                    }
                    prepareAsync()
                }
            } catch (e: Exception) { null }
        }

        // ======= التشغيل =======

        private fun play(player: MediaPlayer?, ready: Boolean, duration: Int) {
            handler.post {
                if (player == null || !ready) return@post
                try {
                    player.seekTo(0, MediaPlayer.SEEK_CLOSEST)
                    player.start()
                    val stopAt = (duration - 100).coerceAtLeast(0).toLong()
                    handler.postDelayed({
                        try { if (player.isPlaying) player.pause() } catch (e: Exception) {}
                    }, stopAt)
                } catch (e: Exception) {}
            }
        }

        private fun stopPlayer(player: MediaPlayer?) {
            try { player?.pause() } catch (e: Exception) {}
        }

        private fun stopAll() {
            handler.removeCallbacksAndMessages(null)
            stopPlayer(playerA)
            stopPlayer(playerB)
        }

        private fun seekAllToStart() {
            handler.post {
                try {
                    if (readyA) playerA?.seekTo(0, MediaPlayer.SEEK_CLOSEST)
                    if (readyB) playerB?.seekTo(0, MediaPlayer.SEEK_CLOSEST)
                } catch (e: Exception) {}
            }
        }

        private fun reload() {
            holder?.let { setupPlayers(it) }
            notifyColorsChanged()
        }

        private fun releasePlayers() {
            handler.removeCallbacksAndMessages(null)
            listOf(playerA, playerB).forEach {
                try { it?.stop(); it?.reset(); it?.release() } catch (e: Exception) {}
            }
            playerA = null; playerB = null
            readyA = false; readyB = false
            durA = 0; durB = 0
        }
    }
}
