package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.db.PaymentEntity
import com.example.data.preferences.UserPreferences
import com.example.data.repository.PaymentRepository
import com.example.utils.TtsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "XyloSmsReceiver"
    }

    private val receiverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        Log.d(TAG, "SMS broadcast intercepted!")

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val fullBodyBuilder = StringBuilder()
        var sender = ""
        for (sms in messages) {
            fullBodyBuilder.append(sms.displayMessageBody)
            sender = sms.displayOriginatingAddress ?: ""
        }

        val body = fullBodyBuilder.toString()
        if (body.isBlank()) return

        Log.d(TAG, "SMS body: $body from sender: $sender")

        // Parse SMS for banking credits
        val parsed = parseBankSms(body, sender) ?: return

        Log.d(TAG, "Bank SMS parsed successfully: Amount ₹${parsed.amount} from bank: ${parsed.appSource}")

        receiverScope.launch {
            val prefs = UserPreferences(context.applicationContext)
            val isEnabled = prefs.isServiceEnabled.first()
            if (!isEnabled) {
                Log.d(TAG, "Service is disabled globally, skipping SMS announcement")
                return@launch
            }

            // Deduplicate! Verify if a duplicate UPI notification has already spoken within the sliding window
            val isDuplicate = com.example.utils.PaymentDeDuplicator.isDuplicate(
                amount = parsed.amount,
                appSource = parsed.appSource,
                senderName = null,
                isBankOrSms = true
            )
            if (isDuplicate) {
                Log.d(TAG, "SMS payment of ₹${parsed.amount} suppressed as duplicate")
                return@launch
            }

            val db = AppDatabase.getInstance(context.applicationContext)
            val repository = PaymentRepository(db.paymentDao())
            val ttsManager = TtsManager(context.applicationContext)

            val lang = prefs.selectedLanguage.first()
            val style = prefs.announcementStyle.first()
            val boost = prefs.volumeBoost.first()

            val speakText = ttsManager.buildAnnouncementText(
                amount = parsed.amount,
                senderName = null,
                appSource = parsed.appSource,
                style = style,
                localeCode = lang
            )

            // Trigger offline voice announcement
            ttsManager.init {
                receiverScope.launch(Dispatchers.Main) {
                    ttsManager.setLanguage(lang)
                    ttsManager.speak(speakText, boostVolume = boost)
                }
            }

            // Persist the transaction details to Room DB
            val entity = PaymentEntity(
                amount = parsed.amount,
                senderName = null,
                appSource = parsed.appSource,
                rawNotificationText = "[SMS from $sender] $body"
            )
            repository.insertPayment(entity)

            // Broadcast the outcome to update UI dashboards reactively
            val broadcastIntent = Intent(UpiNotificationListenerService.ACTION_PAYMENT_RECEIVED).apply {
                putExtra(UpiNotificationListenerService.EXTRA_AMOUNT, parsed.amount)
                putExtra(UpiNotificationListenerService.EXTRA_SENDER, "")
                putExtra(UpiNotificationListenerService.EXTRA_APP, parsed.appSource)
                setPackage(context.packageName)
            }
            context.sendBroadcast(broadcastIntent)
            Log.d(TAG, "SMS receipt broadcasted successfully!")
        }
    }

    data class ParsedSms(val amount: Double, val appSource: String)

    private fun parseBankSms(body: String, sender: String): ParsedSms? {
        val trimmedSender = sender.trim()
        
        // Block personal/standard numbers (e.g. standard contacts start with + or have 10+ digits or only digits)
        val isPersonalNumber = trimmedSender.startsWith("+") || 
                trimmedSender.all { it.isDigit() || it == '-' || it == ' ' || it == '+' } || 
                trimmedSender.length >= 10
        if (isPersonalNumber) {
            Log.d(TAG, "Ignoring SMS from billing/personal number: $trimmedSender")
            return null
        }

        val lowerText = body.lowercase()

        // 1. Verify if text refers directly to credit transactions
        val creditKeywords = listOf("credited", "received", "credited to", "deposited", "received of", "credit of", "added to your wallet")
        val debitKeywords = listOf("debited", "paid to", "sent to", "spent", "debit card", "withdrawn", "payment to")

        val hasCredit = creditKeywords.any { lowerText.contains(it) }
        val hasDebit = debitKeywords.any { lowerText.contains(it) }

        if (!hasCredit || hasDebit) return null

        // 2. Reject promotional noise sent by banks (e.g. loan offers)
        val promokeywords = listOf(
            "pre-approved", "loan", "off", "discount", "save", "offer", "apply", "rewards", "coupon", "gift",
            "order", "delivery", "food", "dinner", "lunch", "swiggy", "zomato", "by swiggy", "by zomato",
            "burger", "pizza", "meal", "feast", "swiggyit", "guarantee"
        )
        if (promokeywords.any { lowerText.contains(it) }) {
            return null
        }

        // 3. Extract core decimal transaction digit amount
        val cleanedText = body.replace(",", "")
        val patterns = listOf(
            Regex("""(?:₹|Rs\.?|INR)\s*([0-9]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
            Regex("""credited\s*(?:with|of)?\s*(?:₹|Rs\.?|INR)?\s*([0-9]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
        )

        var amount: Double? = null
        for (pattern in patterns) {
            val match = pattern.find(cleanedText)
            if (match != null) {
                val amountStr = match.groupValues[1]
                amount = amountStr.toDoubleOrNull()
                if (amount != null) break
            }
        }

        if (amount == null) return null

        // 4. Identify originating bank / channel name from SMS Sender Code (e.g. AD-HDFCBK)
        val cleanedSender = sender.uppercase().trim()
        val bankSource = when {
            cleanedSender.contains("HDFC") -> "HDFC Bank"
            cleanedSender.contains("ICICI") -> "ICICI Bank"
            cleanedSender.contains("SBIBK") || cleanedSender.contains("SBIIN") || cleanedSender.contains("SBICD") -> "SBI"
            cleanedSender.contains("AXIS") -> "Axis Bank"
            cleanedSender.contains("PNB") -> "PNB Bank"
            cleanedSender.contains("BOB") -> "Bank of Baroda"
            cleanedSender.contains("KOTAK") -> "Kotak Bank"
            cleanedSender.contains("PAYTM") -> "Paytm"
            cleanedSender.contains("PNSB") -> "PhonePe"
            else -> {
                val parts = cleanedSender.split("-")
                val actualSender = if (parts.size == 2) parts[1] else cleanedSender
                if (actualSender.length >= 3) {
                    "$actualSender Bank"
                } else {
                    "Bank SMS"
                }
            }
        }

        return ParsedSms(amount, bankSource)
    }
}
