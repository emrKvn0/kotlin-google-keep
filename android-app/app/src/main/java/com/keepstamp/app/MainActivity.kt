package com.keepstamp.app

import android.os.Bundle
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
        val btnCopyNow = findViewById<Button>(R.id.btn_copy_now)
        val btnOpenKeep = findViewById<Button>(R.id.btn_open_keep)

        etFormat.setText(DateHelper.getFormat(this))
        tvPreview.text = "Önizleme: ${DateHelper.getCurrentStamp(this)}"

        btnSaveFormat.setOnClickListener {
            val newFormat = etFormat.text.toString().ifBlank { DateHelper.DEFAULT_FORMAT }
            DateHelper.setFormat(this, newFormat)
            tvPreview.text = "Önizleme: ${DateHelper.getCurrentStamp(this)}"
            Toast.makeText(this, "Tarih formatı kaydedildi!", Toast.LENGTH_SHORT).show()
        }

        btnCopyNow.setOnClickListener {
            val stamp = DateHelper.copyToClipboard(this, showToast = true)
            tvPreview.text = "Önizleme: $stamp"
        }

        btnOpenKeep.setOnClickListener {
            DateHelper.openKeepWithDate(this)
        }
    }
}
