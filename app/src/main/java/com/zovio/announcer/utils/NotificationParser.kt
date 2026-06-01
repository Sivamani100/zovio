package com.zovio.announcer.utils

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
        // Major / Popular Apps
        "com.phonepe.app",                        // PhonePe
        "com.phonepe.app.preprod",                // PhonePe staging
        "com.phonepe.app.business",               // PhonePe Business
        "net.one97.paytm",                        // Paytm
        "com.paytmmall",                          // Paytm Mall
        "com.paytm.business",                     // Paytm Business
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "com.google.android.apps.nbu.paisa.merchant", // Google Pay Business
        "com.google.android.apps.nandimobile",   // Google Pay alternate
        "com.whatsapp",                           // WhatsApp Pay
        "com.whatsapp.w4b",                       // WhatsApp Business Pay
        "in.amazon.mShop.android.shopping",      // Amazon Pay
        "in.org.npci.upiapp",                    // BHIM UPI
        "com.dreamplug.androidapp",               // CRED Pay
        "com.mobikwik_new",                       // MobiKwik
        "com.freecharge.android",                 // Freecharge
        "com.truecaller",                         // Truecaller Pay
        "com.tatadigital.tcp",                    // Tata Neu
        "com.naviapp",                            // Navi App
        "com.fampay.in",                          // FamPay / FamApp
        "com.nextbillion.groww",                  // Groww UPI
        "com.epifi.paisa",                        // Fi Money
        "com.olacabs.customer",                   // Ola Money
        "money.super.payments",                   // Super Money
        "org.altruist.BajajExperia",              // Bajaj Finserv
        "in.bajajfinservmarkets.app",             // Bajaj Finserv Markets
        "com.postpe.app",                         // BharatPe Consumer
        "com.bharatpe.app",                       // BharatPe Merchant
        "indwin.c3.shareapp",                     // Slice UPI
        "com.moneytap.bnpl.app",                  // Freo / MoneyTap
        "com.paypal.android.p2pmobile",           // PayPal India

        // Bank-Backed and Private Bank UPI Apps
        "com.sbi.upi",                            // SBI Pay
        "com.sbi.SBIFreedomPlus",                 // SBI YONO / Freedom Plus
        "com.csam.icici.bank.imobile",            // ICICI iMobile Pay
        "com.icicibank.pockets",                  // ICICI Pockets
        "com.snapwork.hdfc",                      // HDFC PayZapp
        "com.snapwork.hdfcbank",                  // HDFC PayZapp alternate
        "com.enstage.wibmo.hdfc",                 // HDFC Wibmo
        "com.upi.axispay",                        // Axis Pay
        "com.axis.mobile",                        // Axis Bank Mobile
        "com.olive.kotak.upi",                    // Kotak Pay
        "com.kotak.mobilebanking",                // Kotak Bank
        "com.YesBank",                            // Yes Bank
        "com.yesbank.yespaynext",                 // YES HUB Pay
        "com.fss.pnbpsp",                         // PNB UPI
        "com.bankofbaroda.upi",                   // Bank of Baroda
        "com.canarabank.mobility",                // Canara Bank
        "com.mgs.induspsp",                       // IndusInd Bank
        "com.upi.federalbank.org.lotza",          // Federal Bank
        "com.SIBMobile",                          // South Indian Bank
        "com.rblbank.mobank",                     // RBL MoBank
        "com.olive.dcb.upi",                      // DCB Bank
        "com.dbs.in.digitalbank",                 // DBS Digibank
        "com.fss.idfcpsp",                        // IDFC PSP
        "com.finopaytech.bpayfino",               // Fino Pay
        "com.fss.jnkpsp",                         // Jammu & Kashmir Bank
        "com.fss.vijayapsp",                      // Vijaya Bank
        "com.dena.upi.gui",                       // Dena Bank
        "com.mycompany.kvb",                      // KVB
        "com.mgs.obcbank",                        // Oriental Bank of Commerce
        "com.mgs.hsbcupi",                        // HSBC UPI
        "com.fisglobal.bandhanupi.app",           // Bandhan Bank
        "com.abipbl.upi",                         // ABIPBL (Allahabad Bank)
        "com.fss.unbipsp",                        // UNBI
        "com.iexceed.appzillon.ippbMB",           // India Post Payments Bank MB

        // Telecom / Other Payment Apps
        "com.myairtelapp",                        // Airtel Thanks / Airtel Pay
        "com.jio.myjio",                          // Jio Pay / MyJio
        "com.samsung.android.spay",               // Samsung Pay
        "com.samsung.android.spaymini",           // Samsung Pay Mini
        "com.citrus.citruspay",                   // CitrusPay
        "com.bsb.hike",                           // Hike
        "in.chillr",                              // Chillr
        "ai.wizely.android",                      // Wizely
        "in.gokiwi.kiwitpap",                     // Kiwi App
        "com.ultracash.payment.customer",         // Ultracash
        "com.svs.shriramcity",                    // Shriram One
        "com.popclub.android",                    // POPclub
        "inance.global.travel.niyo",              // Niyo Global
        "com.microsoft.mobile.polymer",           // Microsoft Polymer
        "com.msf.angelmobile",                    // Angel Broking Mobile
        "com.angelbroking.angelwealth",           // Angel Wealth
        "com.fundsindia",                        // Funds India

        // Additional / Existing known UPI providers
        "com.phonepe.app.preprod",
        "com.phonepe.app.business",
        "com.commercial.android",                 // fallback pattern if needed
        "com.naviapp"
    )

    private val APP_DISPLAY_NAMES = mapOf(
        "com.phonepe.app" to "PhonePe",
        "com.phonepe.app.preprod" to "PhonePe",
        "com.phonepe.app.business" to "PhonePe Business",
        "net.one97.paytm" to "Paytm",
        "com.paytmmall" to "Paytm Mall",
        "com.paytm.business" to "Paytm Business",
        "com.google.android.apps.nbu.paisa.user" to "Google Pay",
        "com.google.android.apps.nbu.paisa.merchant" to "Google Pay Business",
        "com.google.android.apps.nandimobile" to "Google Pay",
        "com.whatsapp" to "WhatsApp Pay",
        "com.whatsapp.w4b" to "WhatsApp Pay",
        "in.amazon.mShop.android.shopping" to "Amazon Pay",
        "in.org.npci.upiapp" to "BHIM UPI",
        "com.dreamplug.androidapp" to "CRED Pay",
        "com.cred.android" to "CRED",
        "com.mobikwik_new" to "MobiKwik",
        "com.mobikwik_instapay" to "MobiKwik",
        "com.freecharge.android" to "Freecharge",
        "com.truecaller" to "Truecaller Pay",
        "com.tatadigital.tcp" to "Tata Neu",
        "com.naviapp" to "Navi",
        "com.fampay.in" to "FamPay",
        "com.nextbillion.groww" to "Groww UPI",
        "com.epifi.paisa" to "Fi Money",
        "com.olacabs.customer" to "Ola Money",
        "money.super.payments" to "Super Money",
        "org.altruist.BajajExperia" to "Bajaj Finserv",
        "in.bajajfinservmarkets.app" to "Bajaj Finserv Markets",
        "com.postpe.app" to "BharatPe",
        "com.bharatpe.app" to "BharatPe Merchant",
        "indwin.c3.shareapp" to "Slice UPI",
        "com.moneytap.bnpl.app" to "Freo / MoneyTap",
        "com.paypal.android.p2pmobile" to "PayPal India",
        "com.sbi.upi" to "SBI Pay",
        "com.sbi.SBIFreedomPlus" to "SBI YONO",
        "com.csam.icici.bank.imobile" to "ICICI iMobile",
        "com.icicibank.pockets" to "ICICI Pockets",
        "com.snapwork.hdfc" to "HDFC PayZapp",
        "com.snapwork.hdfcbank" to "HDFC PayZapp",
        "com.enstage.wibmo.hdfc" to "HDFC Wibmo",
        "com.upi.axispay" to "Axis Pay",
        "com.axis.mobile" to "Axis Mobile",
        "com.olive.kotak.upi" to "Kotak Pay",
        "com.kotak.mobilebanking" to "Kotak Bank",
        "com.YesBank" to "Yes Bank",
        "com.yesbank.yespaynext" to "YES HUB Pay",
        "com.fss.pnbpsp" to "PNB UPI",
        "com.bankofbaroda.upi" to "Bank of Baroda",
        "com.canarabank.mobility" to "Canara Bank",
        "com.mgs.induspsp" to "IndusInd Bank",
        "com.upi.federalbank.org.lotza" to "Federal Bank",
        "com.SIBMobile" to "South Indian Bank",
        "com.rblbank.mobank" to "RBL MoBank",
        "com.olive.dcb.upi" to "DCB Bank",
        "com.dbs.in.digitalbank" to "DBS Digibank",
        "com.fss.idfcpsp" to "IDFC PSP",
        "com.finopaytech.bpayfino" to "Fino Pay",
        "com.fss.jnkpsp" to "Jammu & Kashmir Bank",
        "com.fss.vijayapsp" to "Vijaya Bank",
        "com.dena.upi.gui" to "Dena Bank",
        "com.mycompany.kvb" to "KVB",
        "com.mgs.obcbank" to "OBC Bank",
        "com.mgs.hsbcupi" to "HSBC UPI",
        "com.fisglobal.bandhanupi.app" to "Bandhan Bank",
        "com.abipbl.upi" to "Allahabad Bank",
        "com.fss.unbipsp" to "UNBI",
        "com.iexceed.appzillon.ippbMB" to "India Post Payments Bank",
        "com.myairtelapp" to "Airtel Thanks",
        "com.jio.myjio" to "Jio Pay",
        "com.samsung.android.spay" to "Samsung Pay",
        "com.samsung.android.spaymini" to "Samsung Pay Mini",
        "com.citrus.citruspay" to "CitrusPay",
        "com.bsb.hike" to "Hike",
        "in.chillr" to "Chillr",
        "ai.wizely.android" to "Wizely",
        "in.gokiwi.kiwitpap" to "Kiwi",
        "com.ultracash.payment.customer" to "Ultracash",
        "com.svs.shriramcity" to "Shriram One",
        "com.popclub.android" to "POPclub",
        "inance.global.travel.niyo" to "Niyo Global",
        "com.microsoft.mobile.polymer" to "Microsoft Polymer",
        "com.msf.angelmobile" to "Angel Broking",
        "com.angelbroking.angelwealth" to "Angel Wealth",
        "com.fundsindia" to "Funds India",
        "com.tatadigital.tcp" to "Tata Neu"
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

        // 1. Promotional/Spam Exclusion only when there is no clear received payment context.
        val hasReceivedKeyword = RECEIVED_KEYWORDS.any { lowerText.contains(it) }
        val amountDetected = extractAmount(fullText) != null
        if (!hasReceivedKeyword && !amountDetected && PROMOTIONAL_KEYWORDS.any { lowerText.contains(it) }) {
            Log.d("NotificationParser", "Suppressed promotional/ad notification: $fullText")
            return null
        }

        // 2. Strict Conversational WhatsApp Chat Exclusion for WhatsApp Pay notifications.
        if (packageName == "com.whatsapp" || packageName == "com.whatsapp.w4b") {
            // WhatsApp chat notifications use Notification.MessagingStyle and contain android.messages.
            // Official WhatsApp Pay notifications typically do not carry chat thread message bubbles.
            if (extras.containsKey("android.messages") || extras.getParcelableArray("android.messages") != null) {
                Log.d("NotificationParser", "Skipped casual WhatsApp chat thread notification")
                return null
            }
            val lowerTitle = title.lowercase()
            val isOfficialPayment = lowerTitle.contains("payment") || lowerTitle.contains("whatsapp pay") ||
                    lowerText.contains("payment received") || lowerText.contains("received ₹") ||
                    lowerText.contains("credited ₹") || lowerText.contains("sent you ₹") ||
                    lowerText.contains("whatsapp pay") || lowerText.contains("paid you")
            val hasConversationalIndicators = lowerText.contains("?") || lowerText.contains("please") ||
                    lowerText.contains("bro") || lowerText.contains("bhai") || lowerText.contains("check") ||
                    lowerText.contains("ask") || lowerText.contains("group") || lowerText.contains("call") ||
                    lowerText.contains("request")
            if (!isOfficialPayment || hasConversationalIndicators) {
                Log.d("NotificationParser", "Skipped casual WhatsApp chat notification: $fullText")
                return null
            }
        }

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
