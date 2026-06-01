package com.zovio.announcer.service

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.zovio.announcer.MainActivity
import com.zovio.announcer.R

class QrScannerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            val remoteViews = RemoteViews(context.packageName, R.layout.qr_scanner_widget_layout)

            // Click PendingIntent that targets MainActivity and forces instant camera scan trigger
            val scanIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("START_SCANNER", true)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                widgetId + 2000, // Unique requestCode offset
                scanIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            remoteViews.setOnClickPendingIntent(R.id.scanner_widget_container, pendingIntent)

            appWidgetManager.updateAppWidget(widgetId, remoteViews)
        }
        Log.d(TAG, "onUpdate completed for QrScannerWidgetProvider widgets: ${appWidgetIds.size}")
    }

    companion object {
        private const val TAG = "QrScannerWidget"
    }
}
