package com.keepstamp.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class KeepDateWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_COPY_DATE -> DateHelper.copyToClipboard(context, showToast = true)
            ACTION_NEW_KEEP_NOTE -> DateHelper.openKeepWithDate(context)
        }
    }

    companion object {
        const val ACTION_COPY_DATE = "com.keepstamp.app.ACTION_COPY_DATE"
        const val ACTION_NEW_KEEP_NOTE = "com.keepstamp.app.ACTION_NEW_KEEP_NOTE"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_keep_date)

            val copyIntent = Intent(context, KeepDateWidget::class.java).apply {
                action = ACTION_COPY_DATE
            }
            val copyPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                copyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val newNoteIntent = Intent(context, KeepDateWidget::class.java).apply {
                action = ACTION_NEW_KEEP_NOTE
            }
            val newNotePendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                newNoteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.btn_widget_copy, copyPendingIntent)
            views.setOnClickPendingIntent(R.id.btn_widget_new_note, newNotePendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
