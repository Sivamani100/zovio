package com.example.utils

import android.service.notification.StatusBarNotification
import android.util.Log

object PaymentDeDuplicator {
    private const val WINDOW_MS = 20000L // 20 seconds sliding window

    data class ActivePayment(
        val amount: Double,
        val timestamp: Long,
        val appSource: String,
        val senderName: String?,
        val isBankOrSms: Boolean
    )

    private val history = mutableListOf<ActivePayment>()

    @Synchronized
    fun isDuplicate(amount: Double, appSource: String, senderName: String?, isBankOrSms: Boolean): Boolean {
        val now = System.currentTimeMillis()
        // Clean old records
        history.removeAll { now - it.timestamp > WINDOW_MS }

        val matching = history.filter { it.amount == amount }
        if (matching.isEmpty()) {
            history.add(ActivePayment(amount, now, appSource, senderName, isBankOrSms))
            return false
        }

        for (old in matching) {
            // If both have non-null, non-blank sender names, and the names are DIFFERENT,
            // they are actually two distinct people paying the same amount. Do not deduplicate!
            val oldName = old.senderName?.trim()?.lowercase()
            val newName = senderName?.trim()?.lowercase()
            if (!oldName.isNullOrBlank() && !newName.isNullOrBlank() && oldName != newName) {
                continue
            }

            // Otherwise, they are duplicates (e.g. same sender name, or one/both have null names like SMS/bank alerts,
            // or from different apps within the same 20-second window representing the same transaction)
            Log.d("PaymentDeDuplicator", "Suppressed duplicate payment: ₹$amount from $appSource (already announced from ${old.appSource})")
            return true
        }

        history.add(ActivePayment(amount, now, appSource, senderName, isBankOrSms))
        return false
    }
}

object NotificationParser {

    private val UPI_APP_PACKAGES = setOf(
        "com.phonepe.app",                          // PhonePe
        "com.phonepe.app.preprod",                  // PhonePe staging
        "com.phonepe.app.business",                 // PhonePe Business
        "net.one97.paytm",                          // Paytm
        "net.one97.paytm.merchant",                 // Paytm Business
        "com.paytm.business",                       // Paytm Business alternative
        "com.google.android.apps.nbu.paisa.user",  // Google Pay
        "com.google.android.apps.nbu.paisa.merchant", // Google Pay Business
        "in.org.npci.upiapp",                      // BHIM
        "com.freecharge.android",                   // Freecharge
        "com.mobikwik_new",                         // MobiKwik
        "com.amazon.mShop.android.shopping",       // Amazon Pay
        "com.samsung.android.spay",                 // Samsung Pay
        "com.whatsapp",                             // WhatsApp Pay
        "io.navi.app",                              // Navi
        "com.bajajfinserv.wallet",                  // Bajaj Pay
        "com.axis.mobile",                          // Axis Bank
        "com.csam.icici.bank.imobile",             // ICICI iMobile
        "com.snapwork.hdfc",                        // HDFC PayZapp
        "com.sbi.SBIFreedomPlus",                  // SBI YONO
        "com.cred.android"                          // CRED
    )

    private val APP_DISPLAY_NAMES = mapOf(
        "com.phonepe.app" to "PhonePe",
        "com.phonepe.app.business" to "PhonePe Business",
        "net.one97.paytm" to "Paytm",
        "net.one97.paytm.merchant" to "Paytm Business",
        "com.paytm.business" to "Paytm Business",
        "com.google.android.apps.nbu.paisa.user" to "Google Pay",
        "com.google.android.apps.nbu.paisa.merchant" to "Google Pay Business",
        "in.org.npci.upiapp" to "BHIM",
        "com.freecharge.android" to "FreeCharge",
        "com.mobikwik_new" to "MobiKwik",
        "com.amazon.mShop.android.shopping" to "Amazon Pay",
        "com.whatsapp" to "WhatsApp Pay",
        "io.navi.app" to "Navi",
        "com.cred.android" to "CRED"
    )

    private val RECEIVED_KEYWORDS = listOf(
        "received", "credited", "credit", "paid to you", "paid you", "sent you", "sent to you",
        "payment received", "money received", "transferred to you", "deposit", "deposited", "received from",
        "प्राप्त", "मिले", "क्रेडिट", "जमा",  // Hindi
        "జమ", "స్వీకరించారు",             // Telugu
        "பெற்றீர்கள்", "வரவு",           // Tamil
        "ಸ್ವೀಕರಿಸಿದ್ದೀರಿ", "ಜಮೆಯಾಗಿದೆ",              // Kannada
        "ಲഭിച്ചു",                       // Malayalam
        "mili", "prapt"                   // Transliterated Hindi
    )

    private val SENT_KEYWORDS = listOf(
        "you paid", "you sent", "paid to", "sent to", "debited", "debit",
        "payment sent", "money sent", "transferred to", "debited from",
        "bheja", "send", "payment successful to"
    )

    private val PROMOTIONAL_KEYWORDS = listOf(
        "off", "discount", "cashback", "win", "scratch card", "save", "offer", "promo", "voucher",
        "apply now", "rewards", "coupon", "gift", "loan", "limit", "recharge", "promo", "deal", "cash back",
        "flat", "up to", "earn", "upgrade", "spin", "chance to", "pay later", "avail", "apply", "voucher",
        "ad", "sponsored", "hurry", "insure", "insurance", "recharge", "bill payment", "pre-approved",
        "order", "delivery", "food", "dinner", "lunch", "swiggy", "zomato", "by swiggy", "by zomato",
        "burger", "pizza", "meal", "feast", "swiggyit", "guarantee"
    )

    private val AMOUNT_PATTERNS = listOf(
        Regex("""(?:₹|Rs\.?|INR)\s*([0-9]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
        Regex("""([0-9]+(?:\.[0-9]{1,2})?)\s*(?:rupees?|rs\.?|₹)""", RegexOption.IGNORE_CASE),
        Regex("""(?:received|credited|received\s+of|payment\s+of)\s*(?:₹|Rs\.?|INR)?\s*([0-9]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
    )

    private val SENDER_PATTERNS = listOf(
        Regex("""from\s+([A-Za-z0-9 ]{2,30})""", RegexOption.IGNORE_CASE),
        Regex("""([A-Za-z ]{2,25})\s+(?:paid|sent|transferred)\s+you""", RegexOption.IGNORE_CASE),
        Regex("""by\s+([A-Za-z0-9 ]{2,30})""", RegexOption.IGNORE_CASE)
    )

    data class ParsedPayment(
        val amount: Double,
        val senderName: String?,
        val appSource: String,
        val rawText: String,
        val isBankApp: Boolean
    )

    fun parse(sbn: StatusBarNotification): ParsedPayment? {
        val packageName = sbn.packageName

        if (packageName !in UPI_APP_PACKAGES) return null

        val extras = sbn.notification?.extras ?: return null
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""
        val subText = extras.getCharSequence("android.subText")?.toString() ?: ""
        val infoText = extras.getCharSequence("android.infoText")?.toString() ?: ""

        // Process message bubbles (MessagingStyle) if present
        val messageTexts = StringBuilder()
        val messages = extras.getParcelableArray("android.messages")
        if (messages != null) {
            for (msgObj in messages) {
                if (msgObj is android.os.Bundle) {
                    val msgText = msgObj.getCharSequence("text")?.toString() ?: ""
                    if (msgText.isNotBlank()) {
                        messageTexts.append(" ").append(msgText)
                    }
                }
            }
        }

        // Combine all possible parts of notification text together to run matcher
        val items = listOf(title, text, bigText, subText, infoText, messageTexts.toString())
        val fullText = items.filter { it.isNotBlank() }.joinToString(" ").replace(Regex("\\s+"), " ").trim()

        if (fullText.isBlank()) return null

        val lowerText = fullText.lowercase()

        // 1. Strict Promotional/Spam Exclusion (Stops PhonePe/Paytm advertisements and Swiggy offers)
        if (PROMOTIONAL_KEYWORDS.any { lowerText.contains(it) }) {
            Log.d("NotificationParser", "Suppressed promotional/ad notification: $fullText")
            return null
        }

        // 2. Strict Conversational WhatsApp Chat Exclusion
        if (packageName == "com.whatsapp") {
            // WhatsApp chat notifications use Notification.MessagingStyle and contain android.messages.
            // Official WhatsApp Pay notifications do not contain messages chat threads!
            if (extras.containsKey("android.messages") || extras.getParcelableArray("android.messages") != null) {
                Log.d("NotificationParser", "Skipped casual WhatsApp chat thread notification")
                return null
            }
            val lowerTitle = title.lowercase()
            val isOfficialPayment = lowerTitle.contains("payment") || lowerTitle.contains("whatsapp pay") || 
                    lowerText.contains("payment received") || lowerText.contains("received ₹") || 
                    lowerText.contains("credited ₹") || lowerText.contains("sent you ₹") ||
                    lowerText.contains("whatsapp pay")
            val hasConversationalIndicators = lowerText.contains("?") || lowerText.contains("please") || 
                    lowerText.contains("bro") || lowerText.contains("bhai") || lowerText.contains("check") || 
                    lowerText.contains("ask") || lowerText.contains("group") || lowerText.contains("call") || 
                    lowerText.contains("send") || lowerText.contains("request")
            if (!isOfficialPayment || hasConversationalIndicators) {
                Log.d("NotificationParser", "Skipped casual WhatsApp chat notification: $fullText")
                return null
            }
        }

        // Ignore clear outgoing transactions
        if (SENT_KEYWORDS.any { lowerText.contains(it) } &&
            !RECEIVED_KEYWORDS.any { lowerText.contains(it) }) {
            return null
        }

        val hasReceivedKeyword = RECEIVED_KEYWORDS.any { lowerText.contains(it) }
        val hasSentKeyword = SENT_KEYWORDS.any { lowerText.contains(it) }
        if (!hasReceivedKeyword && hasSentKeyword) return null

        val amount = extractAmount(fullText) ?: return null
        
        // Extract sender name
        var sender = extractSender(fullText)
        if (sender.isNullOrBlank()) {
            val titleClean = title.trim()
            val lowerTitle = titleClean.lowercase()
            val systemTitles = setOf(
                "google pay", "phonepe", "paytm", "bhim", "upi", "payment", 
                "announcer", "alert", "transaction", "bank", "notification", 
                "cashback", "cred", "incoming", "received", "credited", "business"
            )
            val isSystemWord = systemTitles.any { lowerTitle.contains(it) }
            if (titleClean.length in 2..30 && !isSystemWord) {
                sender = titleClean
            }
        }

        val appName = APP_DISPLAY_NAMES[packageName] ?: "UPI"
        val isBankApp = packageName.contains("bank") || packageName.contains("sbi") || packageName.contains("axis") || packageName.contains("hdfc") || packageName.contains("icici")

        return ParsedPayment(
            amount = amount,
            senderName = sender,
            appSource = appName,
            rawText = fullText,
            isBankApp = isBankApp
        )
    }

    private fun extractAmount(text: String): Double? {
        val cleanedText = text.replace(",", "") // Remove digit commas first to allow simple numeric parsing
        for (pattern in AMOUNT_PATTERNS) {
            val match = pattern.find(cleanedText)
            if (match != null) {
                val amountStr = match.groupValues[1]
                return amountStr.toDoubleOrNull()
            }
        }
        return null
    }

    private fun extractSender(text: String): String? {
        val noiseWords = setOf("you", "your", "the", "a", "an", "upi", "payment", "pay", "user")
        for (pattern in SENDER_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                val name = match.groupValues[1].trim()
                if (name.isNotBlank() && name.lowercase() !in noiseWords && name.length >= 2) {
                    return name
                }
            }
        }
        return null
    }

    fun isFromUpiApp(packageName: String): Boolean = packageName in UPI_APP_PACKAGES
}
