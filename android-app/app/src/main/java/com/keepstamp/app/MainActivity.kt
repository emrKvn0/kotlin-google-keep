package com.keepstamp.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etFormat = findViewById<EditText>(R.id.et_format)
        val tvPreview = findViewById<TextView>(R.id.tv_preview)
        val btnSaveFormat = findViewById<Button>(R.id.btn_save_format)
        val btnEnableFloating = findViewById<Button>(R.id.btn_enable_floating)
        val btnCopyNow = findViewById<Button>(R.id.btn_copy_now)
        val btnOpenKeep = findViewById<Button>(R.id.btn_open_keep)

        etFormat.setText(DateHelper.getFormat(this))
        tvPreview.text = "Önizleme: ${DateHelper.getCurrentStamp(this).trim()}"

        btnSaveFormat.setOnClickListener {
            val newFormat = etFormat.text.toString().ifBlank { DateHelper.DEFAULT_FORMAT }
            DateHelper.setFormat(this, newFormat)
            tvPreview.text = "Önizleme: ${DateHelper.getCurrentStamp(this).trim()}"
            Toast.makeText(this, "Tarih formatı kaydedildi!", Toast.LENGTH_SHORT).show()
        }

        btnEnableFloating.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(
                this,
                "Yüklü Uygulamalar -> Keep Tarih Damgası'nı Açın",
                Toast.LENGTH_LONG
            ).show()
        }

        btnCopyNow.setOnClickListener {
            val stamp = DateHelper.copyToClipboard(this, showToast = true)
            tvPreview.text = "Önizleme: ${stamp.trim()}"
        }

        btnOpenKeep.setOnClickListener {
            DateHelper.openKeepWithDate(this)
        }
    }
}
