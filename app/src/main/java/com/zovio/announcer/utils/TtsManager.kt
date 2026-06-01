package com.zovio.announcer.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.media.audiofx.LoudnessEnhancer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class TtsManager(private val context: Context) {

    private data class PendingSpeech(val text: String, val boostVolume: Boolean)

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val pendingQueue = mutableListOf<PendingSpeech>()
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var savedVolume = -1
    private var focusRequest: Any? = null
    private val activeEnhancers = ConcurrentHashMap<String, LoudnessEnhancer>()

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        Log.d(TAG, "Audio focus transient change feedback: $focusChange")
    }

    companion object {
        private const val TAG = "TtsManager"
        private const val UTTERANCE_ID_PREFIX = "upi_announce_"
    }

    fun init(onReady: (() -> Unit)? = null) {
        if (tts != null) return

        try {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isInitialized = true
                    Log.d(TAG, "TextToSpeech initialized successfully")
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            Log.d(TAG, "TTS utterance started: $utteranceId")
                        }

                        private fun cleanEnhancer(id: String?) {
                            if (id != null) {
                                activeEnhancers.remove(id)?.let { enhancer ->
                                    try {
                                        enhancer.enabled = false
                                        enhancer.release()
                                        Log.d(TAG, "Successfully released LoudnessEnhancer for utterance: $id")
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error releasing LoudnessEnhancer for utterance: $id", e)
                                    }
                                }
                            }
                        }

                        override fun onDone(utteranceId: String?) {
                            Log.d(TAG, "TTS utterance done: $utteranceId")
                            cleanEnhancer(utteranceId)
                            restoreVolume()
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            Log.e(TAG, "TTS utterance error: $utteranceId")
                            cleanEnhancer(utteranceId)
                            restoreVolume()
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            Log.e(TAG, "TTS utterance error: $utteranceId, code: $errorCode")
                            cleanEnhancer(utteranceId)
                            restoreVolume()
                        }
                    })

                    val itemsToSpeak = synchronized(pendingQueue) {
                        val list = ArrayList(pendingQueue)
                        pendingQueue.clear()
                        list
                    }
                    itemsToSpeak.forEach { speak(it.text, boostVolume = it.boostVolume) }
                    onReady?.invoke()
                } else {
                    Log.e(TAG, "TTS initialization failed status: $status")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to instantiate TextToSpeech engine", e)
        }
    }

    fun setLanguage(localeCode: String) {
        if (!isInitialized || tts == null) return
        val parts = localeCode.split("_")
        val locale = if (parts.size == 2) {
            Locale(parts[0], parts[1])
        } else {
            Locale(parts[0])
        }

        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Language $localeCode is not supported, falling back to English India")
            tts?.setLanguage(Locale("en", "IN"))
        } else {
            Log.d(TAG, "TTS language set to $localeCode")
        }
    }

    fun buildAnnouncementText(
        amount: Double,
        senderName: String?,
        appSource: String,
        style: String, // unused but kept for compatibility
        localeCode: String
    ): String {
        val amountStr = formatAmount(amount)

        // Streamlined to always be "[Amount] Rupees received from [payment app]"
        // as per shopkeeper request to avoid reading phone numbers, names, or double announcements.
        return when (localeCode) {
            "hi_IN" -> { // Hindi
                "$appSource पर $amountStr रुपये प्राप्त हुए"
            }
            "te_IN" -> { // Telugu
                "$appSource ద్వారా $amountStr రూపాయలు వచ్చాయి"
            }
            "ta_IN" -> { // Tamil
                "$appSource மூலம் $amountStr ரூபாய் பெறப்பட்டது"
            }
            "kn_IN" -> { // Kannada
                "$appSource ಮೂಲಕ $amountStr ರೂಪಾಯಿ ಜಮೆಯಾಗಿದೆ"
            }
            "ml_IN" -> { // Malayalam
                "$appSource വഴി $amountStr രൂപ ലഭിച്ചു"
            }
            "mr_IN" -> { // Marathi
                "$appSource वर $amountStr रुपये मिळाले"
            }
            "bn_IN" -> { // Bengali
                "$appSource ए $amountStr টাকা পাওয়া গেছে"
            }
            "gu_IN" -> { // Gujarati
                "$appSource પર $amountStr રૂપિયા મળ્યા"
            }
            "pa_IN" -> { // Punjabi
                "$appSource ਤੇ $amountStr ਰੁਪਏ ਪ੍ਰਾਪਤ ਹੋਏ"
            }
            else -> { // DEFAULT English (en_IN or other)
                "$amountStr Rupees received from $appSource"
            }
        }
    }

    private fun formatAmount(amount: Double): String {
        return if (amount == amount.toLong().toDouble()) {
            amount.toLong().toString()
        } else {
            val parts = amount.toString().split(".")
            val decimalStr = parts[1]
            val suffix = if (decimalStr.length == 1) "${decimalStr}0" else decimalStr
            "${parts[0]} point $suffix"
        }
    }

    fun speak(text: String, boostVolume: Boolean = true) {
        if (!isInitialized || tts == null) {
            Log.w(TAG, "TTS not initialized yet. Queuing: $text")
            synchronized(pendingQueue) {
                pendingQueue.add(PendingSpeech(text, boostVolume))
            }
            return
        }

        if (boostVolume) {
            boostVolume()
        }
        requestAudioFocusTransient()

        val utteranceId = "$UTTERANCE_ID_PREFIX${System.currentTimeMillis()}"
        val bundle = Bundle()

        if (boostVolume && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                // Set maximum float volume parameter to guarantee full engine output power
                bundle.putFloat("volume", 1.0f)

                val sessionId = audioManager.generateAudioSessionId()
                bundle.putInt("audioSessionId", sessionId)
                
                // Construct LoudnessEnhancer for the exact TTS playback session ID
                // targetGain in millibels: 3000mB (30dB) yields stunning 200%+ perceived loudness.
                val enhancer = LoudnessEnhancer(sessionId).apply {
                    setTargetGain(3000)
                    enabled = true
                }
                activeEnhancers[utteranceId] = enhancer
                Log.d(TAG, "LoudnessEnhancer attached successfully to sessionId $sessionId with target gain of 3000mB (200% voice amplification)!")
            } catch (e: Exception) {
                Log.e(TAG, "Could not apply LoudnessEnhancer audio effect for session boost", e)
            }
        }

        bundle.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts?.speak(text, TextToSpeech.QUEUE_ADD, bundle, utteranceId)
        } else {
            @Suppress("DEPRECATION")
            val map = HashMap<String, String>().apply {
                put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }
            tts?.speak(text, TextToSpeech.QUEUE_ADD, map)
        }
        Log.d(TAG, "Commanded TTS to speak: [ $text ]")
    }

    private fun requestAudioFocusTransient() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attribs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(attribs)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener(focusChangeListener)
                    .build()
                focusRequest = request
                audioManager.requestAudioFocus(request)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    focusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }
            Log.d(TAG, "Requested transient audio focus successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting audio focus", e)
        }
    }

    private fun releaseAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val request = focusRequest as? AudioFocusRequest
                if (request != null) {
                    audioManager.abandonAudioFocusRequest(request)
                    focusRequest = null
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(focusChangeListener)
            }
            Log.d(TAG, "Released audio focus successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error abandoning audio focus", e)
        }
    }

    private fun boostVolume() {
        try {
            val stream = AudioManager.STREAM_MUSIC
            if (savedVolume == -1) {
                savedVolume = audioManager.getStreamVolume(stream)
            }
            val maxVolume = audioManager.getStreamMaxVolume(stream)
            // Push only the music stream to max volume for TTS clarity.
            audioManager.setStreamVolume(stream, maxVolume, 0)
            Log.d(TAG, "Volume boosted from $savedVolume to $maxVolume")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to boost music stream volume", e)
        }
    }

    private fun restoreVolume() {
        releaseAudioFocus()
        try {
            if (savedVolume != -1) {
                val stream = AudioManager.STREAM_MUSIC
                audioManager.setStreamVolume(stream, savedVolume, 0)
                Log.d(TAG, "Volume restored back to $savedVolume")
                savedVolume = -1
            }
            Log.d(TAG, "Restored music stream volume")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore volume", e)
        }
    }

    fun shutdown() {
        try {
            activeEnhancers.values.forEach { enhancer ->
                try {
                    enhancer.enabled = false
                    enhancer.release()
                } catch (e: Exception) {
                    // Ignore release errors on destroyed objects
                }
            }
            activeEnhancers.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing activeEnhancers", e)
        }
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        } finally {
            tts = null
            isInitialized = false
        }
    }

    fun isReady() = isInitialized
}
