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
    }

    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {

        private var playerLock: MediaPlayer? = null   // فيديو 1 — Lock Screen
        private var playerUnlock: MediaPlayer? = null // فيديو 2 — Unlock
        private var isReadyLock = false
        private var isReadyUnlock = false
        private var durationLock = 0
        private var durationUnlock = 0
        private var isLocked = true
        private var currentSurface: SurfaceHolder? = null
        private val handler = Handler(Looper.getMainLooper())

        private val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        isLocked = true
                        showFirstFrameLock()
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        if (isLocked) {
                            // شاشة القفل ظهرت — شغّل فيديو 1
                            playLockVideo()
                        }
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        if (isLocked) {
                            isLocked = false
                            // فُتح القفل — شغّل فيديو 2
                            playUnlockVideo()
                        }
                    }
                    ACTION_RELOAD -> {
                        currentSurface?.let {
                            setupPlayers(it)
                        }
                        notifyColorsChanged()
                    }
                }
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)
            setTouchEventsEnabled(false)
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(ACTION_RELOAD)
            }
            registerReceiver(receiver, filter)
        }

        override fun onDestroy() {
            super.onDestroy()
            try { unregisterReceiver(receiver) } catch (e: Exception) {}
            releasePlayers()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            holder.setSizeFromLayout()
            currentSurface = holder
            setupPlayers(holder)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            releasePlayers()
            currentSurface = null
        }

        override fun onVisibilityChanged(visible: Boolean) {}

        override fun onComputeColors(): WallpaperColors {
            val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
            val colorInt = prefs.getInt("accent_color", Color.parseColor("#6200EE"))
            return WallpaperColors(Color.valueOf(colorInt), null, null)
        }

        private fun setupPlayers(holder: SurfaceHolder) {
            val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
            val uriLock = prefs.getString("video_uri_lock", null)
            val uriUnlock = prefs.getString(MainActivity.PREF_VIDEO_URI, null)

            releasePlayers()

            // إعداد فيديو 1 (Lock)
            if (uriLock != null) {
                setupPlayer(
                    uri = Uri.parse(uriLock),
                    holder = holder,
                    isLockPlayer = true
                )
            }

            // إعداد فيديو 2 (Unlock)
            if (uriUnlock != null) {
                setupPlayer(
                    uri = Uri.parse(uriUnlock),
                    holder = holder,
                    isLockPlayer = false
                )
            }
        }

        private fun setupPlayer(uri: Uri, holder: SurfaceHolder, isLockPlayer: Boolean) {
            try {
                val player = MediaPlayer().apply {
                    setSurface(holder.surface)
                    setDataSource(applicationContext, uri)
                    isLooping = false
                    setOnPreparedListener { mp ->
                        if (isLockPlayer) {
                            durationLock = mp.duration
                            isReadyLock = true
                        } else {
                            durationUnlock = mp.duration
                            isReadyUnlock = true
                        }
                        mp.seekTo(0, MediaPlayer.SEEK_CLOSEST)
                        mp.start()
                        handler.postDelayed({ mp.pause() }, 80)
                    }
                    setOnCompletionListener { mp ->
                        mp.pause()
                    }
                    setOnErrorListener { _, _, _ ->
                        if (isLockPlayer) isReadyLock = false
                        else isReadyUnlock = false
                        false
                    }
                    prepareAsync()
                }
                if (isLockPlayer) playerLock = player
                else playerUnlock = player
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun playLockVideo() {
            handler.removeCallbacksAndMessages(null)
            handler.post {
                try {
                    val player = playerLock ?: return@post
                    if (!isReadyLock) return@post
                    player.seekTo(0, MediaPlayer.SEEK_CLOSEST)
                    player.start()
                    val stopAt = (durationLock - 100).coerceAtLeast(0).toLong()
                    handler.postDelayed({
                        try { if (player.isPlaying) player.pause() } catch (e: Exception) {}
                    }, stopAt)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        private fun playUnlockVideo() {
            handler.removeCallbacksAndMessages(null)
            handler.post {
                try {
                    // أوقف فيديو 1 أولاً
                    try { playerLock?.pause() } catch (e: Exception) {}

                    val player = playerUnlock ?: return@post
                    if (!isReadyUnlock) return@post
                    player.seekTo(0, MediaPlayer.SEEK_CLOSEST)
                    player.start()
                    val stopAt = (durationUnlock - 100).coerceAtLeast(0).toLong()
                    handler.postDelayed({
                        try { if (player.isPlaying) player.pause() } catch (e: Exception) {}
                    }, stopAt)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        private fun showFirstFrameLock() {
            handler.removeCallbacksAndMessages(null)
            handler.post {
                try {
                    playerLock?.let { if (isReadyLock) { it.pause(); it.seekTo(0, MediaPlayer.SEEK_CLOSEST) } }
                    playerUnlock?.let { if (isReadyUnlock) { it.pause(); it.seekTo(0, MediaPlayer.SEEK_CLOSEST) } }
                } catch (e: Exception) {}
            }
        }

        private fun releasePlayers() {
            handler.removeCallbacksAndMessages(null)
            listOf(playerLock, playerUnlock).forEach { player ->
                try { player?.stop(); player?.reset(); player?.release() } catch (e: Exception) {}
            }
            playerLock = null
            playerUnlock = null
            isReadyLock = false
            isReadyUnlock = false
            durationLock = 0
            durationUnlock = 0
        }
    }
}
