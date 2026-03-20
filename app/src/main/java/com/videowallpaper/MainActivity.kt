package com.videowallpaper

import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnSelectVideo: Button
    private lateinit var btnSetWallpaper: Button
    private lateinit var btnPickColor: Button
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
        btnSelectVideo = findViewById(R.id.btnSelectVideo)
        btnSetWallpaper = findViewById(R.id.btnSetWallpaper)
        btnPickColor = findViewById(R.id.btnPickColor)
        colorPreview = findViewById(R.id.colorPreview)

        val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
        selectedColor = prefs.getInt("accent_color", Color.parseColor("#6200EE"))
        updateColorUI()

        val savedUri = prefs.getString(PREF_VIDEO_URI, null)
        if (savedUri != null) {
            tvStatus.text = "تم اختيار الفيديو ✓"
            tvStatus.setTextColor(selectedColor)
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
            showColorPickerDialog()
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
    }

    private fun showColorPickerDialog() {
        val colors = listOf(
            "#6200EE" to "بنفسجي",
            "#03DAC5" to "فيروزي",
            "#FF6D00" to "برتقالي",
            "#E91E63" to "وردي",
            "#00BCD4" to "سماوي",
            "#4CAF50" to "أخضر",
            "#F44336" to "أحمر",
            "#FF9800" to "ذهبي",
            "#9C27B0" to "بنفسجي غامق",
            "#FFFFFF" to "أبيض",
            "#000000" to "أسود"
        )

        val names = colors.map { it.second }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("اختر لون Material You")
            .setItems(names) { _, index ->
                selectedColor = Color.parseColor(colors[index].first)
                val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
                prefs.edit().putInt("accent_color", selectedColor).apply()
                updateColorUI()
                Toast.makeText(this, "تم حفظ اللون ✓", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun updateColorUI() {
        colorPreview.setBackgroundColor(selectedColor)
        btnSetWallpaper.setBackgroundColor(selectedColor)
        btnPickColor.setBackgroundColor(selectedColor)
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
                tvStatus.text = "تم اختيار الفيديو ✓"
                tvStatus.setTextColor(selectedColor)
                btnSetWallpaper.isEnabled = true
            }
        }
    }
}
