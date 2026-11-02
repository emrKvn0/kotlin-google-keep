package com.keepstamp.app

import android.service.quicksettings.TileService

/**
 * Samsung One UI üst bildirim çubuğuna (Wi-Fi, Fener yanına) "📅 Tarih Kopyala" butonu ekler.
 * Google Keep'te not yazarken bildirim çubuğunu indirip tek tıkla tarihi panoya kopyalar.
 */
class DateTileService : TileService() {
    override fun onClick() {
        super.onClick()
        DateHelper.copyToClipboard(applicationContext, showToast = true)
    }
}
