package com.keepstamp.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Google Keep (veya herhangi bir uygulama) içinde metin seçildiğinde / imlece basılı tutulduğunda
 * çıkan menüye "📅 Tarih Ekle" seçeneği ekler.
 * Tıklandığında seçili metnin yanına o anki tarihi ekler ve anında geri döner (ekran açmaz).
 */
class ProcessTextActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString() ?: ""
        val isReadOnly = intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false)
        val stamp = DateHelper.getCurrentStamp(this)

        if (!isReadOnly) {
            val updatedText = if (selectedText.isBlank()) {
                stamp
            } else {
                "$stamp$selectedText"
            }
            val resultIntent = Intent().apply {
                putExtra(Intent.EXTRA_PROCESS_TEXT, updatedText)
            }
            setResult(RESULT_OK, resultIntent)
        } else {
            DateHelper.copyToClipboard(this, showToast = true)
            setResult(RESULT_CANCELED)
        }

        finish()
    }
}
