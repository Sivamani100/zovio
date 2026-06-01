package com.zovio.announcer.service

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.RemoteViews
import com.zovio.announcer.MainActivity
import com.zovio.announcer.R
import com.zovio.announcer.data.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class QrWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Run blocking on the IO dispatcher to quickly load the preference string and update RemoteViews synchronized with the system call
        kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            val prefs = UserPreferences(context.applicationContext)
            val activeType = prefs.getActiveQrType()
            for (widgetId in appWidgetIds) {
                updateWidgetView(context, appWidgetManager, widgetId, activeType)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == ACTION_REFRESH_WIDGET || action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, QrWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            // Get active type either from intent extra (instant update) or fall back to datastore
            val forcedActiveType = intent.getStringExtra(EXTRA_ACTIVE_TYPE)
            if (forcedActiveType != null) {
                for (widgetId in appWidgetIds) {
                    updateWidgetView(context, appWidgetManager, widgetId, forcedActiveType)
                }
            } else {
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.zovio.announcer.action.REFRESH_QR_WIDGET"
        const val EXTRA_ACTIVE_TYPE = "extra_active_type"
        private const val TAG = "QrWidgetProvider"

        fun triggerWidgetUpdate(context: Context, forceType: String? = null) {
            // Send to all installed widget IDs via standard appwidget update broadcast
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, QrWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, QrWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH_WIDGET
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                    if (forceType != null) {
                        putExtra(EXTRA_ACTIVE_TYPE, forceType)
                    }
                }
                context.sendBroadcast(intent)
                Log.d(TAG, "Sent widget update broadcast with forcedType: $forceType to ${appWidgetIds.size} widgets")
            } else {
                Log.d(TAG, "Zero widgets currently placed on the homescreen, skipping intent dispatch")
            }
        }

        private fun updateWidgetView(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            activeType: String
        ) {
            val remoteViews = RemoteViews(context.packageName, R.layout.qr_widget_layout)

            // 1. Create click PendingIntent that launches the Main App at the Qr Screen
            val configIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                // Pass extra parameter to indicate QR tab should be shown
                putExtra("NAVIGATE_TO_QR", true)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                widgetId,
                configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            remoteViews.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            // 2. Load dynamic photo from the files storage
            if (activeType != "none" && activeType.isNotBlank()) {
                // Locate the image file
                val qrsDir = File(context.filesDir, "qrs")
                val cleanKey = activeType.lowercase().replace(" ", "_")
                val qrFile = File(qrsDir, "qr_$cleanKey.png")

                if (qrFile.exists() && qrFile.length() > 0) {
                    try {
                        val bitmap = BitmapFactory.decodeFile(qrFile.absolutePath)
                        if (bitmap != null) {
                            remoteViews.setImageViewBitmap(R.id.widget_qr_image, bitmap)
                            Log.d(TAG, "Widget $widgetId updated with custom QR file of $activeType")
                        } else {
                            remoteViews.setImageViewResource(R.id.widget_qr_image, R.drawable.ic_qr_placeholder)
                            Log.e(TAG, "Failed to decode QR image for $activeType, loaded template")
                        }
                    } catch (e: Exception) {
                        remoteViews.setImageViewResource(R.id.widget_qr_image, R.drawable.ic_qr_placeholder)
                        Log.e(TAG, "Exception reading widget QR image", e)
                    }
                } else {
                    remoteViews.setImageViewResource(R.id.widget_qr_image, R.drawable.ic_qr_placeholder)
                    Log.d(TAG, "File path of $activeType QR missing: ${qrFile.absolutePath}")
                }
            } else {
                remoteViews.setImageViewResource(R.id.widget_qr_image, R.drawable.ic_qr_placeholder)
            }

            // Bind the update to the system AppWidget manager
            appWidgetManager.updateAppWidget(widgetId, remoteViews)
        }
    }
}
