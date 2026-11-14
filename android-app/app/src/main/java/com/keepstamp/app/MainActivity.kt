package com.keepstamp.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.CheckBox
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
        val cbSmartSpacing = findViewById<CheckBox>(R.id.cb_smart_spacing)
        val btnSaveFormat = findViewById<Button>(R.id.btn_save_format)
        val btnEnableFloating = findViewById<Button>(R.id.btn_enable_floating)
        val btnCopyNow = findViewById<Button>(R.id.btn_copy_now)
        val btnOpenKeep = findViewById<Button>(R.id.btn_open_keep)

        val presetButtons = listOf(
            R.id.btn_preset_option_a to DateHelper.DEFAULT_FORMAT,
            R.id.btn_preset_option_b to DateHelper.OPTION_B_FORMAT,
            R.id.btn_preset_classic to DateHelper.CLASSIC_FORMAT,
            R.id.btn_preset_short to DateHelper.SHORT_FORMAT
        )

        fun updatePreview(pattern: String) {
            val formatted = DateHelper.formatStamp(pattern)
            tvPreview.text = "Önizleme:\n$formatted"
        }

        val currentFormat = DateHelper.getFormat(this)
        etFormat.setText(currentFormat)
        cbSmartSpacing.isChecked = DateHelper.isSmartSpacingEnabled(this)
        updatePreview(currentFormat)

        etFormat.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updatePreview(s?.toString().orEmpty())
            }
        })

        presetButtons.forEach { (btnId, presetPattern) ->
            findViewById<Button>(btnId).setOnClickListener {
                etFormat.setText(presetPattern)
                updatePreview(presetPattern)
            }
        }

        cbSmartSpacing.setOnCheckedChangeListener { _, isChecked ->
            DateHelper.setSmartSpacingEnabled(this, isChecked)
        }

        btnSaveFormat.setOnClickListener {
            val newFormat = etFormat.text.toString().ifBlank { DateHelper.DEFAULT_FORMAT }
            DateHelper.setFormat(this, newFormat)
            DateHelper.setSmartSpacingEnabled(this, cbSmartSpacing.isChecked)
            updatePreview(newFormat)
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
            DateHelper.copyToClipboard(this, showToast = true)
            updatePreview(DateHelper.getFormat(this))
        }

        btnOpenKeep.setOnClickListener {
            DateHelper.openKeepWithDate(this)
        }
    }
}
