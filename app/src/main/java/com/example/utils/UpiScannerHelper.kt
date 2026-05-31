package com.example.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.widget.Toast
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

data class UpiAppInfo(
    val name: String,
    val packageName: String,
    val icon: Bitmap,
    val resolveInfo: ResolveInfo? = null
)

data class ParsedUpiData(
    val rawPayload: String,
    val upiId: String,
    val payeeName: String?,
    val amount: String?,
    val note: String?
)

object UpiScannerHelper {
    private const val TAG = "UpiScannerHelper"

    @Volatile
    private var cachedUpiApps: List<UpiAppInfo>? = null

    /**
     * Parse raw UPI URI string into readable components.
     */
    fun parseUpiUri(uriString: String): ParsedUpiData? {
        if (!uriString.startsWith("upi://pay", ignoreCase = true)) {
            return null
        }
        return try {
            val uri = Uri.parse(uriString)
            val pa = uri.getQueryParameter("pa") ?: ""
            val pn = uri.getQueryParameter("pn")
            val am = uri.getQueryParameter("am")
            val tn = uri.getQueryParameter("tn")
            ParsedUpiData(
                rawPayload = uriString,
                upiId = pa,
                payeeName = pn,
                amount = am,
                note = tn
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding upi uri", e)
            null
        }
    }

    /**
     * Query PM for installed payment applications that support handling standard upi:// scheme actions.
     */
    fun queryInstalledUpiApps(context: Context): List<UpiAppInfo> {
        val cached = cachedUpiApps
        if (cached != null) {
            return cached
        }
        return synchronized(this) {
            val cachedSecond = cachedUpiApps
            if (cachedSecond != null) {
                cachedSecond
            } else {
                val pm = context.packageManager
                // A minimal dummy valid link using standard scheme to resolve handler list
                val dummyUpiUri = Uri.parse("upi://pay?pa=payment@xylobiz&pn=Xylo")
                val intent = Intent(Intent.ACTION_VIEW, dummyUpiUri)
                
                val list = try {
                    val defaultList = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                    if (defaultList.isEmpty()) {
                        pm.queryIntentActivities(intent, 0)
                    } else {
                        defaultList
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed resolving intent handlers for upi scheme", e)
                    emptyList()
                }

                val apps = ArrayList<UpiAppInfo>()

                // 1. Process standard dynamic intent-filter matches safely
                for (resolveInfo in list) {
                    try {
                        val actInfo = resolveInfo.activityInfo
                        if (actInfo != null) {
                            val packageName = actInfo.packageName
                            if (packageName != null && packageName != context.packageName) {
                                val name = resolveInfo.loadLabel(pm).toString().ifBlank { actInfo.name }
                                val icon = drawableToBitmap(resolveInfo.loadIcon(pm))
                                apps.add(UpiAppInfo(name, packageName, icon, resolveInfo))
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing resolveInfo dynamically", e)
                    }
                }

                // 2. Fallback robust explicit package checks for systems bypass
                val knownUpiApps = listOf(
                    "com.phonepe.app" to "PhonePe",
                    "com.google.android.apps.nandimobile" to "Google Pay",
                    "net.one97.paytm" to "Paytm",
                    "com.whatsapp" to "WhatsApp",
                    "com.whatsapp.w4b" to "WhatsApp Business",
                    "me.fampay.app" to "FamApp",
                    "com.fampay" to "FamApp Legacy",
                    "in.org.npci.upiapp" to "BHIM",
                    "com.dreamplug.androidapp" to "CRED",
                    "in.amazon.mShop.android.shopping" to "Amazon Pay",
                    "com.sbi.lotusintouch" to "BHIM SBI Pay",
                    "com.csg.imobile" to "iMobile ICICI",
                    "com.snapwork.hdfc" to "HDFC MobileBanking",
                    "com.axis.mobile" to "Axis Mobile",
                    "com.mobikwik_instapay" to "MobiKwik",
                    "com.freecharge.android" to "Freecharge",
                    "com.jupiter.money" to "Jupiter",
                    "com.slice.android" to "Slice",
                    "com.kotak.mobilebanking" to "Kotak Bank",
                    "com.mymoney" to "PhonePe MyMoney"
                )

                for ((pkgName, displayName) in knownUpiApps) {
                    // Skip if already successfully discovered dynamically
                    if (apps.any { it.packageName == pkgName }) continue
                    
                    try {
                        val appInfo = pm.getApplicationInfo(pkgName, PackageManager.GET_META_DATA)
                        if (pkgName != context.packageName) {
                            val name = appInfo.loadLabel(pm).toString().ifBlank { displayName }
                            val icon = drawableToBitmap(appInfo.loadIcon(pm))
                            apps.add(UpiAppInfo(name, pkgName, icon, null))
                        }
                    } catch (e: PackageManager.NameNotFoundException) {
                        // Ignore silently if app is not installed
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception querying package info for $pkgName", e)
                    }
                }

                val result = apps.distinctBy { it.packageName }
                cachedUpiApps = result
                result
            }
        }
    }

    /**
     * Launches a specific UPI application targeted by package namespace with the raw scanned UPI String.
     */
    fun launchUpiPayment(context: Context, rawUrl: String, app: UpiAppInfo) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rawUrl)).apply {
                `package` = app.packageName
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to launch ${app.name}: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Launch Play Services Code Scanner to capture barcodes/QR codes with automatic focusing.
     */
    fun startBarcodeScanner(
        context: Context,
        onSuccess: (String) -> Unit,
        onCancelOrFailure: (String) -> Unit = {}
    ) {
        try {
            val options = GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .enableAutoZoom()
                .build()
            
            val scanner = GmsBarcodeScanning.getClient(context, options)
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    val rawValue = barcode.rawValue
                    if (!rawValue.isNullOrBlank()) {
                        Log.d(TAG, "Successfully scanned code value: $rawValue")
                        onSuccess(rawValue)
                    } else {
                        onCancelOrFailure("Empty QR scan result parsed.")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "GmsScanner failed", e)
                    onCancelOrFailure(e.localizedMessage ?: "User canceled or GMS module is loading.")
                }
		} catch (e: Exception) {
            Log.e(TAG, "Exception launching startBarcodeScanner", e)
            onCancelOrFailure(e.localizedMessage ?: "Unknown hardware trigger error.")
        }
    }

    /**
     * Helper to render vector or standard android application drawables into beautiful Compose-compatible bitmaps.
     */
    fun drawableToBitmap(drawable: Drawable): Bitmap {
        try {
            if (drawable is BitmapDrawable) {
                val bmp = drawable.bitmap
                if (bmp != null && !bmp.isRecycled) {
                    return bmp
                }
            }
            
            // Limit maximum dimension to prevent OutOfMemoryError and keep it rendering light and fast
            val width = if (drawable.intrinsicWidth > 0) minOf(drawable.intrinsicWidth, 256) else 128
            val height = if (drawable.intrinsicHeight > 0) minOf(drawable.intrinsicHeight, 256) else 128
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
            return bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Exception rendering drawable to bitmap", e)
            val fallback = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            return fallback
        }
    }
}
