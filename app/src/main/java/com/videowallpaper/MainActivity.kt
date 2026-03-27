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
    private var isDualMode = false

    companion object {
        const val REQUEST_VIDEO_LOCK = 1001
        const val REQUEST_VIDEO_UNLOCK = 1002
        const val PREF_VIDEO_URI = "video_uri"
        const val PREF_VIDEO_URI_LOCK = "video_uri_lock"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatusLock = findViewById(R.id.tvStatusLock)
        tvStatusUnlock = findViewById(R.id.tvStatusUnlock)
        tvColorHex = findViewById(R.id.tvColorHex)
        tvMode = findViewById(R.id.tvMode)
        btnSelectLock = findViewById(R.id.btnSelectLock)
        btnSelectUnlock = findViewById(R.id.btnSelectUnlock)
        btnSetWallpaper = findViewById(R.id.btnSetWallpaper)
        btnPickColor = findViewById(R.id.btnPickColor)
        btnReset = findViewById(R.id.btnReset)
        switchDualMode = findViewById(R.id.switchDualMode)
        cardUnlock = findViewById(R.id.cardUnlock)
        colorPreview = findViewById(R.id.colorPreview)

        val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
        selectedColor = prefs.getInt("accent_color", Color.parseColor("#6200EE"))
        isDualMode = prefs.getBoolean("dual_mode", false)
        updateColorUI()

        // استرجع الحالة
        if (prefs.getString(PREF_VIDEO_URI_LOCK, null) != null) {
            tvStatusLock.text = "تم الاختيار ✓"
            tvStatusLock.setTextColor(Color.parseColor("#1DB954"))
        }
        if (prefs.getString(PREF_VIDEO_URI, null) != null) {
            tvStatusUnlock.text = "تم الاختيار ✓"
            tvStatusUnlock.setTextColor(Color.parseColor("#1DB954"))
        }

        // تطبيق الوضع المحفوظ
        switchDualMode.isChecked = isDualMode
        updateDualMode(isDualMode)
        updateSetButton()

        switchDualMode.setOnCheckedChangeListener { _, isChecked ->
            isDualMode = isChecked
            prefs.edit().putBoolean("dual_mode", isDualMode).apply()
            updateDualMode(isDualMode)
            sendReload()
        }

        btnSelectLock.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "video/*"
            }
            startActivityForResult(intent, REQUEST_VIDEO_LOCK)
        }

        btnSelectUnlock.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "video/*"
            }
            startActivityForResult(intent, REQUEST_VIDEO_UNLOCK)
        }

        btnPickColor.setOnClickListener {
            showColorWheelDialog()
        }

        btnSetWallpaper.setOnClickListener {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@MainActivity, VideoWallpaperService::class.java)
                )
            }
            startActivity(intent)
        }

        btnReset.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.reset_title))
                .setMessage(getString(R.string.reset_message))
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    prefs.edit().clear().apply()
                    selectedColor = Color.parseColor("#6200EE")
                    isDualMode = false
                    switchDualMode.isChecked = false
                    updateDualMode(false)
                    updateColorUI()
                    tvStatusLock.text = getString(R.string.no_video)
                    tvStatusLock.setTextColor(Color.parseColor("#FF5555"))
                    tvStatusUnlock.text = getString(R.string.no_video)
                    tvStatusUnlock.setTextColor(Color.parseColor("#FF5555"))
                    updateSetButton()
                    sendReload()
                    Toast.makeText(this,
                        getString(R.string.reset_done),
                        Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }

    private fun updateDualMode(dual: Boolean) {
        cardUnlock.visibility = if (dual) View.VISIBLE else View.GONE
        tvMode.text = if (dual) "🎬🎬 فيديوين — مرحلتين" else "🎬 فيديو واحد"
    }

    private fun updateSetButton() {
        val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
        val hasLock = prefs.getString(PREF_VIDEO_URI_LOCK, null) != null
        val hasUnlock = prefs.getString(PREF_VIDEO_URI, null) != null
        btnSetWallpaper.isEnabled = hasLock || hasUnlock
    }

    private fun sendReload() {
        sendBroadcast(Intent(VideoWallpaperService.ACTION_RELOAD))
    }

    private fun showColorWheelDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_color_wheel, null)
        val colorWheel = dialogView.findViewById<ColorWheelView>(R.id.colorWheel)
        val sbBrightness = dialogView.findViewById<SeekBar>(R.id.sbBrightness)
        val tvHexCode = dialogView.findViewById<TextView>(R.id.tvHexCode)
        val previewBar = dialogView.findViewById<View>(R.id.previewBar)

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
                val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
                prefs.edit().putInt("accent_color", selectedColor).apply()
                updateColorUI()
                sendReload()
                Toast.makeText(this,
                    getString(R.string.color_saved),
                    Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun updateColorUI() {
        val hex = "#%06X".format(0xFFFFFF and selectedColor)
        tvColorHex.text = hex
        val circle = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(selectedColor)
            setStroke(4, 0x60FFFFFF)
        }
        colorPreview.background = circle
        val btnDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 42f
            setColor(selectedColor)
        }
        btnSetWallpaper.background = btnDrawable
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri: Uri = data?.data ?: return
        contentResolver.takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
        when (requestCode) {
            REQUEST_VIDEO_LOCK -> {
                prefs.edit().putString(PREF_VIDEO_URI_LOCK, uri.toString()).apply()
                tvStatusLock.text = "تم الاختيار ✓"
                tvStatusLock.setTextColor(Color.parseColor("#1DB954"))
                sendReload()
            }
            REQUEST_VIDEO_UNLOCK -> {
                prefs.edit().putString(PREF_VIDEO_URI, uri.toString()).apply()
                tvStatusUnlock.text = "تم الاختيار ✓"
                tvStatusUnlock.setTextColor(Color.parseColor("#1DB954"))
                sendReload()
            }
        }
        updateSetButton()
    }
}
