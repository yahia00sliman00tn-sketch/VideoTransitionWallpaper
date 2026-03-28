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

    // وضع واحد
    private lateinit var cardSingle: LinearLayout
    private lateinit var tvStatusSingle: TextView
    private lateinit var btnSelectSingle: Button
    private lateinit var radioLock: RadioButton
    private lateinit var radioHome: RadioButton

    // وضع مزدوج
    private lateinit var cardDual: LinearLayout
    private lateinit var tvStatusLock: TextView
    private lateinit var tvStatusHome: TextView
    private lateinit var btnSelectLock: Button
    private lateinit var btnSelectHome: Button

    // مشترك
    private lateinit var tvMode: TextView
    private lateinit var tvColorHex: TextView
    private lateinit var btnSetWallpaper: Button
    private lateinit var btnPickColor: Button
    private lateinit var btnReset: Button
    private lateinit var switchDualMode: Switch
    private lateinit var colorPreview: View
    private var selectedColor = Color.parseColor("#6200EE")
    private var isDual = false

    companion object {
        const val REQ_SINGLE = 1001
        const val REQ_LOCK   = 1002
        const val REQ_HOME   = 1003
        // للتوافق
        const val PREF_VIDEO_URI = VideoWallpaperService.KEY_SINGLE_VIDEO
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // وضع واحد
        cardSingle      = findViewById(R.id.cardSingle)
        tvStatusSingle  = findViewById(R.id.tvStatusSingle)
        btnSelectSingle = findViewById(R.id.btnSelectSingle)
        radioLock       = findViewById(R.id.radioLock)
        radioHome       = findViewById(R.id.radioHome)

        // وضع مزدوج
        cardDual        = findViewById(R.id.cardDual)
        tvStatusLock    = findViewById(R.id.tvStatusLock)
        tvStatusHome    = findViewById(R.id.tvStatusHome)
        btnSelectLock   = findViewById(R.id.btnSelectLock)
        btnSelectHome   = findViewById(R.id.btnSelectHome)

        // مشترك
        tvMode          = findViewById(R.id.tvMode)
        tvColorHex      = findViewById(R.id.tvColorHex)
        btnSetWallpaper = findViewById(R.id.btnSetWallpaper)
        btnPickColor    = findViewById(R.id.btnPickColor)
        btnReset        = findViewById(R.id.btnReset)
        switchDualMode  = findViewById(R.id.switchDualMode)
        colorPreview    = findViewById(R.id.colorPreview)

        val prefs = getSharedPreferences(VideoWallpaperService.PREF_NAME, MODE_PRIVATE)
        selectedColor = prefs.getInt(VideoWallpaperService.KEY_COLOR, Color.parseColor("#6200EE"))
        isDual = prefs.getBoolean(VideoWallpaperService.KEY_DUAL_MODE, false)
        val target = prefs.getString(VideoWallpaperService.KEY_SINGLE_TARGET, "home") ?: "home"

        updateColorUI()
        switchDualMode.isChecked = isDual
        updateMode(isDual)

        if (target == "lock") radioLock.isChecked = true
        else radioHome.isChecked = true

        // استرجع الحالة
        if (prefs.getString(VideoWallpaperService.KEY_SINGLE_VIDEO, null) != null) {
            tvStatusSingle.text = "تم الاختيار ✓"
            tvStatusSingle.setTextColor(0xFF1DB954.toInt())
        }
        if (prefs.getString(VideoWallpaperService.KEY_VIDEO_LOCK, null) != null) {
            tvStatusLock.text = "تم الاختيار ✓"
            tvStatusLock.setTextColor(0xFF1DB954.toInt())
        }
        if (prefs.getString(VideoWallpaperService.KEY_VIDEO_HOME, null) != null) {
            tvStatusHome.text = "تم الاختيار ✓"
            tvStatusHome.setTextColor(0xFF1DB954.toInt())
        }
        updateSetButton()

        // Switch
        switchDualMode.setOnCheckedChangeListener { _, isChecked ->
            isDual = isChecked
            prefs.edit().putBoolean(VideoWallpaperService.KEY_DUAL_MODE, isDual).apply()
            updateMode(isDual)
            sendReload()
        }

        // Radio buttons
        radioLock.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                prefs.edit().putString(VideoWallpaperService.KEY_SINGLE_TARGET, "lock").apply()
                sendReload()
            }
        }
        radioHome.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                prefs.edit().putString(VideoWallpaperService.KEY_SINGLE_TARGET, "home").apply()
                sendReload()
            }
        }

        // أزرار الاختيار
        btnSelectSingle.setOnClickListener {
            startActivityForResult(videoPicker(), REQ_SINGLE)
        }
        btnSelectLock.setOnClickListener {
            startActivityForResult(videoPicker(), REQ_LOCK)
        }
        btnSelectHome.setOnClickListener {
            startActivityForResult(videoPicker(), REQ_HOME)
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
                    isDual = false
                    switchDualMode.isChecked = false
                    radioHome.isChecked = true
                    updateMode(false)
                    updateColorUI()
                    tvStatusSingle.text = getString(R.string.no_video)
                    tvStatusSingle.setTextColor(0xFFFF5555.toInt())
                    tvStatusLock.text = getString(R.string.no_video)
                    tvStatusLock.setTextColor(0xFFFF5555.toInt())
                    tvStatusHome.text = getString(R.string.no_video)
                    tvStatusHome.setTextColor(0xFFFF5555.toInt())
                    updateSetButton()
                    sendReload()
                    Toast.makeText(this, getString(R.string.reset_done), Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }

    private fun updateMode(dual: Boolean) {
        cardSingle.visibility = if (dual) View.GONE else View.VISIBLE
        cardDual.visibility   = if (dual) View.VISIBLE else View.GONE
        tvMode.text = if (dual) "🎬🎬 وضع مزدوج — Vivo Style" else "🎬 فيديو واحد"
    }

    private fun updateSetButton() {
        val prefs = getSharedPreferences(VideoWallpaperService.PREF_NAME, MODE_PRIVATE)
        btnSetWallpaper.isEnabled = if (isDual) {
            prefs.getString(VideoWallpaperService.KEY_VIDEO_LOCK, null) != null ||
            prefs.getString(VideoWallpaperService.KEY_VIDEO_HOME, null) != null
        } else {
            prefs.getString(VideoWallpaperService.KEY_SINGLE_VIDEO, null) != null
        }
    }

    private fun sendReload() {
        sendBroadcast(Intent(VideoWallpaperService.ACTION_RELOAD))
    }

    private fun videoPicker() = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "video/*"
    }

    private fun showColorWheelDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_color_wheel, null)
        val colorWheel   = dialogView.findViewById<ColorWheelView>(R.id.colorWheel)
        val sbBrightness = dialogView.findViewById<SeekBar>(R.id.sbBrightness)
        val tvHexCode    = dialogView.findViewById<TextView>(R.id.tvHexCode)
        val previewBar   = dialogView.findViewById<View>(R.id.previewBar)

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
            REQ_SINGLE -> {
                prefs.edit().putString(VideoWallpaperService.KEY_SINGLE_VIDEO, uri.toString()).apply()
                tvStatusSingle.text = "تم الاختيار ✓"
                tvStatusSingle.setTextColor(0xFF1DB954.toInt())
            }
            REQ_LOCK -> {
                prefs.edit().putString(VideoWallpaperService.KEY_VIDEO_LOCK, uri.toString()).apply()
                tvStatusLock.text = "تم الاختيار ✓"
                tvStatusLock.setTextColor(0xFF1DB954.toInt())
            }
            REQ_HOME -> {
                prefs.edit().putString(VideoWallpaperService.KEY_VIDEO_HOME, uri.toString()).apply()
                tvStatusHome.text = "تم الاختيار ✓"
                tvStatusHome.setTextColor(0xFF1DB954.toInt())
            }
        }
        updateSetButton()
        sendReload()
    }
}
