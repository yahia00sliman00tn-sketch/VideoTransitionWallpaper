package com.videowallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnSelectVideo: Button
    private lateinit var btnSetWallpaper: Button

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

        // تحقق إذا كان هناك فيديو محفوظ مسبقاً
        val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
        val savedUri = prefs.getString(PREF_VIDEO_URI, null)
        if (savedUri != null) {
            tvStatus.text = "تم اختيار الفيديو ✓"
            tvStatus.setTextColor(0xFF1DB954.toInt())
            btnSetWallpaper.isEnabled = true
        }

        btnSelectVideo.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "video/*"
            }
            startActivityForResult(intent, REQUEST_VIDEO)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VIDEO && resultCode == RESULT_OK) {
            val uri: Uri? = data?.data
            if (uri != null) {
                // احفظ صلاحية الوصول للفيديو بشكل دائم
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                // احفظ مسار الفيديو
                val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
                prefs.edit().putString(PREF_VIDEO_URI, uri.toString()).apply()

                tvStatus.text = "تم اختيار الفيديو ✓"
                tvStatus.setTextColor(0xFF1DB954.toInt())
                btnSetWallpaper.isEnabled = true
            }
        }
    }
}
