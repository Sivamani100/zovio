package com.zovio.announcer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("BootReceiver", "Boot completed - UPI Payment Announcer system listening handles initialized")
            // System binding automatically reactivates the NotificationListenerService.
            // This class acts as our wake handler to ensure background persistence.
        }
    }
}
