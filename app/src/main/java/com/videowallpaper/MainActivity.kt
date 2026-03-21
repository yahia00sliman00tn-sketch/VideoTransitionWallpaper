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

    private lateinit var tvStatus: TextView
    private lateinit var tvColorHex: TextView
    private lateinit var btnSelectVideo: Button
    private lateinit var btnSetWallpaper: Button
    private lateinit var btnPickColor: Button
    private lateinit var btnReset: Button
    private lateinit var colorPreview: View
    private var selectedColor = Color.parseColor("#6200EE")

    companion object {
        const val REQUEST_VIDEO = 1001
        const val PREF_VIDEO_URI = "video_uri"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvColorHex = findViewById(R.id.tvColorHex)
        btnSelectVideo = findViewById(R.id.btnSelectVideo)
        btnSetWallpaper = findViewById(R.id.btnSetWallpaper)
        btnPickColor = findViewById(R.id.btnPickColor)
        btnReset = findViewById(R.id.btnReset)
        colorPreview = findViewById(R.id.colorPreview)

        val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
        selectedColor = prefs.getInt("accent_color", Color.parseColor("#6200EE"))
        updateColorUI()

        val savedUri = prefs.getString(PREF_VIDEO_URI, null)
        if (savedUri != null) {
            tvStatus.text = getString(R.string.video_selected)
            btnSetWallpaper.isEnabled = true
        }

        btnSelectVideo.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "video/*"
            }
            startActivityForResult(intent, REQUEST_VIDEO)
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
                    // مسح كل البيانات
                    val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
                    prefs.edit().clear().apply()
                    selectedColor = Color.parseColor("#6200EE")
                    updateColorUI()
                    tvStatus.text = getString(R.string.no_video)
                    btnSetWallpaper.isEnabled = false
                    try {
                        val wm = WallpaperManager.getInstance(this)
                        wm.clear()
                    } catch(e: Exception) {}
                    Toast.makeText(this,
                        getString(R.string.reset_done),
                        Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }

    private fun showColorWheelDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_color_wheel, null)

        val colorWheel = dialogView.findViewById<ColorWheelView>(R.id.colorWheel)
        val sbBrightness = dialogView.findViewById<SeekBar>(R.id.sbBrightness)
        val tvHexCode = dialogView.findViewById<TextView>(R.id.tvHexCode)
        val previewBar = dialogView.findViewById<View>(R.id.previewBar)

        var currentColor = selectedColor

        // تهيئة
        val hsv = FloatArray(3)
        Color.colorToHSV(selectedColor, hsv)
        sbBrightness.progress = (hsv[2] * 100).toInt()

        fun updatePreview(color: Int) {
            currentColor = color
            previewBar.setBackgroundColor(color)
            tvHexCode.text = "#%06X".format(0xFFFFFF and color)
        }

        updatePreview(selectedColor)

        colorWheel.onColorChanged = { color ->
            updatePreview(color)
        }

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
        if (requestCode == REQUEST_VIDEO && resultCode == RESULT_OK) {
            val uri: Uri? = data?.data
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
                prefs.edit().putString(PREF_VIDEO_URI, uri.toString()).apply()
                tvStatus.text = getString(R.string.video_selected)
                btnSetWallpaper.isEnabled = true
                updateColorUI()
            }
        }
    }
}
