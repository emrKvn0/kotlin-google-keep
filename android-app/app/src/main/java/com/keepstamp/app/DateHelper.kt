package com.keepstamp.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateHelper {
    private const val PREFS_NAME = "keep_stamp_prefs"
    private const val KEY_FORMAT = "date_format"
    const val DEFAULT_FORMAT = "dd.MM.yyyy HH:mm"

    fun getFormat(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_FORMAT, DEFAULT_FORMAT) ?: DEFAULT_FORMAT
    }

    fun setFormat(context: Context, format: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FORMAT, format).apply()
    }

    fun getCurrentStamp(context: Context): String {
        val pattern = getFormat(context)
        val formatted = try {
            SimpleDateFormat(pattern, Locale("tr", "TR")).format(Date())
        } catch (e: Exception) {
            SimpleDateFormat(DEFAULT_FORMAT, Locale("tr", "TR")).format(Date())
        }
        return "$formatted\n"
    }

    fun copyToClipboard(context: Context, showToast: Boolean = true): String {
        val stamp = getCurrentStamp(context)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Tarih", stamp))
        if (showToast) {
            Toast.makeText(context, "Kopyalandı: ${stamp.trim()}", Toast.LENGTH_SHORT).show()
        }
        return stamp
    }

    fun openKeepWithDate(context: Context) {
        val stamp = copyToClipboard(context, showToast = false)
        val keepIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage("com.google.android.keep")
            putExtra(Intent.EXTRA_TEXT, stamp)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(keepIntent)
        } catch (e: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "$stamp\n")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(fallbackIntent, "Tarihli Not Aç").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}
