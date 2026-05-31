package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.db.AppDatabase
import com.example.data.db.PaymentEntity
import com.example.data.preferences.UserPreferences
import com.example.data.repository.PaymentRepository
import com.example.utils.NotificationParser
import com.example.utils.TtsManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class UpiNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "UpiNotificationService"
        const val ACTION_PAYMENT_RECEIVED = "com.example.upipaymentannouncer.PAYMENT_RECEIVED"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_SENDER = "extra_sender"
        const val EXTRA_APP = "extra_app"

        const val CHANNEL_ID_SERVICE = "upi_announcer_service_channel"
        const val SERVICE_NOTIFICATION_ID = 1001
    }

    private lateinit var ttsManager: TtsManager
    private lateinit var prefs: UserPreferences
    private lateinit var repository: PaymentRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "UpiNotificationListenerService created")

        prefs = UserPreferences(applicationContext)
        val db = AppDatabase.getInstance(applicationContext)
        repository = PaymentRepository(db.paymentDao())

        ttsManager = TtsManager(applicationContext)
        ttsManager.init {
            serviceScope.launch {
                val lang = prefs.selectedLanguage.first()
                withContext(Dispatchers.Main) {
                    ttsManager.setLanguage(lang)
                }
            }
        }
    }



    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        if (!NotificationParser.isFromUpiApp(sbn.packageName)) return

        // 1. Strict time-based filtering to ignore unread/historical/stale notifications on app startup/rebind
        val ageMs = System.currentTimeMillis() - sbn.postTime
        if (ageMs > 20000L) {
            Log.d(TAG, "Ignoring stale notification from ${sbn.packageName} posted ${ageMs / 1000}s ago")
            return
        }

        val parsed = NotificationParser.parse(sbn)
        if (parsed == null) {
            Log.d(TAG, "Notification from ${sbn.packageName} is not a confirmed payment alert; leaving it intact")
            return
        }

        Log.d(TAG, "Notification intercepted from confirmed payment alert: ${sbn.packageName}")

        // Cancel/Dismiss only confirmed payment notifications so normal chat messages remain unaffected.
        try {
            cancelNotification(sbn.key)
            Log.d(TAG, "Successfully intercepted and dismissed payment notification to silent its default sound!")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cancel sbn key ${sbn.key}", e)
        }

        serviceScope.launch {
            val isEnabled = prefs.isServiceEnabled.first()
            if (!isEnabled) {
                Log.d(TAG, "Service is globally disabled in settings, skipping announcement")
                return@launch
            }

            // Check sliding window deduplication to prevent double alerts (e.g. from duplicate Bank apps or SMS)
            val isDuplicate = com.example.utils.PaymentDeDuplicator.isDuplicate(
                amount = parsed.amount,
                appSource = parsed.appSource,
                senderName = parsed.senderName,
                isBankOrSms = parsed.isBankApp
            )
            if (isDuplicate) {
                Log.d(TAG, "Deduplication: Ignored duplicate payment of ₹${parsed.amount} via ${parsed.appSource}")
                return@launch
            }

            Log.d(TAG, "Verified incoming payment parsed! Amount: ₹${parsed.amount}")

            // Prepare voice speech
            val lang = prefs.selectedLanguage.first()
            val style = prefs.announcementStyle.first()
            val boost = prefs.volumeBoost.first()
            val speakText = ttsManager.buildAnnouncementText(
                amount = parsed.amount,
                senderName = parsed.senderName,
                appSource = parsed.appSource,
                style = style,
                localeCode = lang
            )

            // Trigger TTS in the Main thread
            withContext(Dispatchers.Main) {
                ttsManager.setLanguage(lang)
                ttsManager.speak(speakText, boostVolume = boost)
            }

            // Post a clean, aesthetic silent notification from Xylo itself so the shopkeeper still has an active desk confirmation!
            postSilentPaymentNotification(parsed.amount, parsed.appSource)

            // Save to Local DB
            val entity = PaymentEntity(
                amount = parsed.amount,
                senderName = parsed.senderName,
                appSource = parsed.appSource,
                rawNotificationText = parsed.rawText
            )
            repository.insertPayment(entity)

            // Broadcast the outcome to update UI dashboards reactively
            val broadcastIntent = Intent(ACTION_PAYMENT_RECEIVED).apply {
                putExtra(EXTRA_AMOUNT, parsed.amount)
                putExtra(EXTRA_SENDER, parsed.senderName ?: "")
                putExtra(EXTRA_APP, parsed.appSource)
                setPackage(packageName)
            }
            sendBroadcast(broadcastIntent)
        }
    }

    private fun postSilentPaymentNotification(amount: Double, appSource: String) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val alertChannelId = "zovio_payment_alerts"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    alertChannelId,
                    "Zovio Payment Receipts",
                    NotificationManager.IMPORTANCE_LOW // Low importance ensures it is completely silent
                ).apply {
                    description = "Silent receipts for incoming payments"
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notificationIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this,
                System.currentTimeMillis().toInt(),
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(this, alertChannelId)
                .setContentTitle("Payment Received")
                .setContentText("₹$amount received successfully via $appSource")
                .setSmallIcon(applicationInfo.icon)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
            Log.d(TAG, "Posted silent receipt notification for ₹$amount via $appSource")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post silent receipt notification", e)
        }
    }

    private fun startServiceInForeground() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID_SERVICE,
                    "Zovio Live Terminal",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Active listening channel for payment alerts"
                    setShowBadge(false)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notificationIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID_SERVICE)
                .setContentTitle("Xylo Soundbox Active")
                .setContentText("Checking payment receipts in real-time...")
                .setSmallIcon(applicationInfo.icon)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build()

            startForeground(SERVICE_NOTIFICATION_ID, notification)
            Log.d(TAG, "Started foreground service successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start service in foreground container", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No action required
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "UPI Notification Listener connected and ready to process alerts!")
        startServiceInForeground()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "UPI Notification Listener disconnected. Triggering rebind request...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(android.content.ComponentName(this, UpiNotificationListenerService::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
        serviceScope.cancel()
        Log.d(TAG, "UpiNotificationListenerService destroyed")
    }

}
