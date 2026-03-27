package com.videowallpaper

import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatusLock: TextView
    private lateinit var tvStatusUnlock: TextView
    private lateinit var tvColorHex: TextView
    private lateinit var tvMode: TextView
    private lateinit var btnSelectLock: Button
    private lateinit var btnSelectUnlock: Button
    private lateinit var btnSetWallpaper: Button
    private lateinit var btnPickColor: Button
    private lateinit var btnReset: Button
    private lateinit var switchDualMode: Switch
    private lateinit var cardUnlock: LinearLayout
    private lateinit var colorPreview: View
    private var selectedColor = Color.parseColor("#6200EE")

    companion object {
        const val REQUEST_VIDEO_LOCK = 1001
        const val REQUEST_VIDEO_UNLOCK = 1002
        const val PREF_VIDEO_URI = "video_uri_lock"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatusLock   = findViewById(R.id.tvStatusLock)
        tvStatusUnlock = findViewById(R.id.tvStatusUnlock)
        tvColorHex     = findViewById(R.id.tvColorHex)
        tvMode         = findViewById(R.id.tvMode)
        btnSelectLock  = findViewById(R.id.btnSelectLock)
        btnSelectUnlock= findViewById(R.id.btnSelectUnlock)
        btnSetWallpaper= findViewById(R.id.btnSetWallpaper)
        btnPickColor   = findViewById(R.id.btnPickColor)
        btnReset       = findViewById(R.id.btnReset)
        switchDualMode = findViewById(R.id.switchDualMode)
        cardUnlock     = findViewById(R.id.cardUnlock)
        colorPreview   = findViewById(R.id.colorPreview)

        val prefs = getSharedPreferences(VideoWallpaperService.PREF_NAME, MODE_PRIVATE)
        selectedColor = prefs.getInt(VideoWallpaperService.KEY_COLOR, Color.parseColor("#6200EE"))
        updateColorUI()

        // استرجع الحالة
        val isDual = prefs.getBoolean(VideoWallpaperService.KEY_DUAL_MODE, false)
        switchDualMode.isChecked = isDual
        updateDualMode(isDual)

        if (prefs.getString(VideoWallpaperService.KEY_VIDEO_LOCK, null) != null) {
            tvStatusLock.text = "تم الاختيار ✓"
            tvStatusLock.setTextColor(0xFF1DB954.toInt())
        }
        if (prefs.getString(VideoWallpaperService.KEY_VIDEO_UNLOCK, null) != null) {
            tvStatusUnlock.text = "تم الاختيار ✓"
            tvStatusUnlock.setTextColor(0xFF1DB954.toInt())
        }
        updateSetButton()

        switchDualMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(VideoWallpaperService.KEY_DUAL_MODE, isChecked).apply()
            updateDualMode(isChecked)
            sendReload()
        }

        btnSelectLock.setOnClickListener {
            startActivityForResult(videoPickerIntent(), REQUEST_VIDEO_LOCK)
        }

        btnSelectUnlock.setOnClickListener {
            startActivityForResult(videoPickerIntent(), REQUEST_VIDEO_UNLOCK)
        }

        btnPickColor.setOnClickListener { showColorWheelDialog() }

        btnSetWallpaper.setOnClickListener {
            startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@MainActivity, VideoWallpaperService::class.java)
                )
            })
        }

        btnReset.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.reset_title))
                .setMessage(getString(R.string.reset_message))
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    prefs.edit().clear().apply()
                    selectedColor = Color.parseColor("#6200EE")
                    switchDualMode.isChecked = false
                    updateDualMode(false)
                    updateColorUI()
                    tvStatusLock.text = getString(R.string.no_video)
                    tvStatusLock.setTextColor(0xFFFF5555.toInt())
                    tvStatusUnlock.text = getString(R.string.no_video)
                    tvStatusUnlock.setTextColor(0xFFFF5555.toInt())
                    updateSetButton()
                    sendReload()
                    Toast.makeText(this, getString(R.string.reset_done), Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }

    private fun videoPickerIntent() = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "video/*"
    }

    private fun updateDualMode(dual: Boolean) {
        cardUnlock.visibility = if (dual) View.VISIBLE else View.GONE
        tvMode.text = if (dual) "🎬🎬 مرحلتان — Vivo style" else "🎬 فيديو واحد"
    }

    private fun updateSetButton() {
        val prefs = getSharedPreferences(VideoWallpaperService.PREF_NAME, MODE_PRIVATE)
        btnSetWallpaper.isEnabled =
            prefs.getString(VideoWallpaperService.KEY_VIDEO_LOCK, null) != null
    }

    private fun sendReload() {
        sendBroadcast(Intent(VideoWallpaperService.ACTION_RELOAD))
    }

    private fun showColorWheelDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_color_wheel, null)
        val colorWheel  = dialogView.findViewById<ColorWheelView>(R.id.colorWheel)
        val sbBrightness= dialogView.findViewById<SeekBar>(R.id.sbBrightness)
        val tvHexCode   = dialogView.findViewById<TextView>(R.id.tvHexCode)
        val previewBar  = dialogView.findViewById<View>(R.id.previewBar)

        var currentColor = selectedColor
        val hsv = FloatArray(3)
        Color.colorToHSV(selectedColor, hsv)
        sbBrightness.progress = (hsv[2] * 100).toInt()

        fun updatePreview(color: Int) {
            currentColor = color
            previewBar.setBackgroundColor(color)
            tvHexCode.text = "#%06X".format(0xFFFFFF and color)
        }

        updatePreview(selectedColor)
        colorWheel.onColorChanged = { color -> updatePreview(color) }

        sbBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, v: Int, u: Boolean) {
                colorWheel.setBrightness(v / 100f)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle(getString(R.string.pick_color))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                selectedColor = currentColor
                val prefs = getSharedPreferences(VideoWallpaperService.PREF_NAME, MODE_PRIVATE)
                prefs.edit().putInt(VideoWallpaperService.KEY_COLOR, selectedColor).apply()
                updateColorUI()
                sendReload()
                Toast.makeText(this, getString(R.string.color_saved), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun updateColorUI() {
        tvColorHex.text = "#%06X".format(0xFFFFFF and selectedColor)
        colorPreview.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(selectedColor)
            setStroke(4, 0x60FFFFFF)
        }
        btnSetWallpaper.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 42f
            setColor(selectedColor)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val prefs = getSharedPreferences(VideoWallpaperService.PREF_NAME, MODE_PRIVATE)
        when (requestCode) {
            REQUEST_VIDEO_LOCK -> {
                prefs.edit().putString(VideoWallpaperService.KEY_VIDEO_LOCK, uri.toString()).apply()
                tvStatusLock.text = "تم الاختيار ✓"
                tvStatusLock.setTextColor(0xFF1DB954.toInt())
            }
            REQUEST_VIDEO_UNLOCK -> {
                prefs.edit().putString(VideoWallpaperService.KEY_VIDEO_UNLOCK, uri.toString()).apply()
                tvStatusUnlock.text = "تم الاختيار ✓"
                tvStatusUnlock.setTextColor(0xFF1DB954.toInt())
            }
        }
        updateSetButton()
        sendReload()
    }
}
