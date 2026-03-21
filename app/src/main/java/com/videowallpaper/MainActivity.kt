package com.videowallpaper

import android.app.AlertDialog
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
            tvStatus.text = "Video selected ✓"
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

        btnReset.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Reset")
                .setMessage("هل تريد إعادة تعيين الفيديو واللون؟")
                .setPositiveButton("نعم") { _, _ ->
                    val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
                    prefs.edit().clear().apply()
                    selectedColor = Color.parseColor("#6200EE")
                    updateColorUI()
                    tvStatus.text = "No video selected"
                    btnSetWallpaper.isEnabled = false
                    Toast.makeText(this, "تم الإعادة ✓", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("إلغاء", null)
                .show()
        }
    }

    private fun showColorPickerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_color_picker, null)
        val previewBox = dialogView.findViewById<View>(R.id.dialogColorPreview)
        val etHex = dialogView.findViewById<EditText>(R.id.etHex)
        val etR = dialogView.findViewById<EditText>(R.id.etR)
        val etG = dialogView.findViewById<EditText>(R.id.etG)
        val etB = dialogView.findViewById<EditText>(R.id.etB)
        val etH = dialogView.findViewById<EditText>(R.id.etH)
        val etS = dialogView.findViewById<EditText>(R.id.etS)
        val etL = dialogView.findViewById<EditText>(R.id.etL)
        val sbR = dialogView.findViewById<SeekBar>(R.id.sbR)
        val sbG = dialogView.findViewById<SeekBar>(R.id.sbG)
        val sbB = dialogView.findViewById<SeekBar>(R.id.sbB)

        var updating = false
        var r = Color.red(selectedColor)
        var g = Color.green(selectedColor)
        var b = Color.blue(selectedColor)

        fun updateAll() {
            if (updating) return
            updating = true
            val color = Color.rgb(r, g, b)
            previewBox.setBackgroundColor(color)
            etHex.setText("#%02X%02X%02X".format(r, g, b))
            etR.setText(r.toString())
            etG.setText(g.toString())
            etB.setText(b.toString())
            sbR.progress = r
            sbG.progress = g
            sbB.progress = b
            val hsv = FloatArray(3)
            Color.RGBToHSV(r, g, b, hsv)
            etH.setText(hsv[0].toInt().toString())
            etS.setText((hsv[1] * 100).toInt().toString())
            etL.setText((hsv[2] * 100).toInt().toString())
            updating = false
        }

        updateAll()

        sbR.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, v: Int, u: Boolean) { r = v; updateAll() }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
        sbG.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, v: Int, u: Boolean) { g = v; updateAll() }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
        sbB.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, v: Int, u: Boolean) { b = v; updateAll() }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        etHex.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                if (updating) return
                try {
                    val hex = s.toString().trim()
                    val color = Color.parseColor(if (hex.startsWith("#")) hex else "#$hex")
                    r = Color.red(color)
                    g = Color.green(color)
                    b = Color.blue(color)
                    updateAll()
                } catch (e: Exception) {}
            }
            override fun beforeTextChanged(s: CharSequence, a: Int, c: Int, d: Int) {}
            override fun onTextChanged(s: CharSequence, a: Int, c: Int, d: Int) {}
        })

        val hslWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                if (updating) return
                try {
                    val h = etH.text.toString().toFloat()
                    val sv = etS.text.toString().toFloat() / 100f
                    val l = etL.text.toString().toFloat() / 100f
                    val color = Color.HSVToColor(floatArrayOf(h, sv, l))
                    r = Color.red(color)
                    g = Color.green(color)
                    b = Color.blue(color)
                    updateAll()
                } catch (e: Exception) {}
            }
            override fun beforeTextChanged(s: CharSequence, a: Int, c: Int, d: Int) {}
            override fun onTextChanged(s: CharSequence, a: Int, c: Int, d: Int) {}
        }
        etH.addTextChangedListener(hslWatcher)
        etS.addTextChangedListener(hslWatcher)
        etL.addTextChangedListener(hslWatcher)

        AlertDialog.Builder(this, R.style.GlassDialog)
            .setTitle("Material You Color")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                selectedColor = Color.rgb(r, g, b)
                val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
                prefs.edit().putInt("accent_color", selectedColor).apply()
                updateColorUI()
                Toast.makeText(this, "Color saved ✓", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
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
                tvStatus.text = "Video selected ✓"
                btnSetWallpaper.isEnabled = true
                updateColorUI()
            }
        }
    }
}
