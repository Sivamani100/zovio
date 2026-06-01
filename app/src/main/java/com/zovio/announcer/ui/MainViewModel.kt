package com.zovio.announcer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zovio.announcer.data.db.AppDatabase
import com.zovio.announcer.data.db.PaymentEntity
import com.zovio.announcer.data.preferences.UserPreferences
import com.zovio.announcer.data.repository.PaymentRepository
import com.zovio.announcer.utils.TtsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = PaymentRepository(db.paymentDao())
    private val prefs = UserPreferences(application)

    // Statistics Flows directly from Room database
    val todayTotal: StateFlow<Double> = repository.getTodayTotal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val weekTotal: StateFlow<Double> = repository.getWeekTotal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthTotal: StateFlow<Double> = repository.getMonthTotal()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val allPayments: StateFlow<List<PaymentEntity>> = repository.getAllPayments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todaysPayments: StateFlow<List<PaymentEntity>> = repository.getTodaysPayments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = repository.getTotalCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // User preferences settings
    val isServiceEnabled: StateFlow<Boolean> = prefs.isServiceEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val selectedLanguage: StateFlow<String> = prefs.selectedLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en_IN")

    val announcementStyle: StateFlow<String> = prefs.announcementStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "detailed")

    val volumeBoost: StateFlow<Boolean> = prefs.volumeBoost
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val onboardingDone: StateFlow<Boolean> = prefs.onboardingDone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val activeQrType: StateFlow<String> = prefs.activeQrType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "none")

    val appTheme: StateFlow<String> = prefs.appTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val savedAccounts: StateFlow<List<String>> = prefs.savedAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedCategories: StateFlow<List<String>> = prefs.savedCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedPayees: StateFlow<List<String>> = prefs.savedPayees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedTags: StateFlow<List<String>> = prefs.savedTags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userName: StateFlow<String> = prefs.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Siva")

    // Live state tracking for last payment simulated or processed
    val lastPaymentReceived = MutableStateFlow<PaymentEntity?>(null)
    val lastPaymentIsTest = MutableStateFlow(false)

    private var localTts: TtsManager? = null

    init {
        // Prepare local TTS in ViewModel just for test-simulation trigger 
        localTts = TtsManager(getApplication())
        localTts?.init {
            viewModelScope.launch {
                val lang = selectedLanguage.value
                localTts?.setLanguage(lang)
            }
        }

        // Monitor real payments from the database and update lastPaymentReceived state
        viewModelScope.launch {
            repository.getAllPayments().collect { payments ->
                val latest = payments.firstOrNull()
                if (latest != null) {
                    // Update main dashboard with the latest real transaction when it becomes available
                    lastPaymentReceived.value = latest
                    lastPaymentIsTest.value = false
                }
            }
        }
    }

    fun setServiceEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.setServiceEnabled(enabled)
        }
    }

    private fun getLanguageConfirmText(localeCode: String): String {
        return when (localeCode) {
            "hi_IN" -> "हिंदी आवाज़ सक्षम कर दी गई है।"
            "te_IN" -> "తెలుగు వాయిస్ యాక్టివేట్ చేయబడింది"
            "ta_IN" -> "தமிழ் குரல் செயல்படுத்தப்பட்டது"
            "kn_IN" -> "ಕನ್ನಡ ಧ್ವನಿಯನ್ನು ಸಕ್ರಿಯಗೊಳಿಸಲಾಗಿದೆ"
            "ml_IN" -> "മലയാളം ശബ്ദം സജീവമാക്കിയിരിക്കുന്നു"
            "gu_IN" -> "ગુજરાતી અવાજ સક્રિય થયેલ છે"
            "pa_IN" -> "ਪੰਜਾਬੀ ਆਵਾਜ਼ ਚਾਲੂ ਕਰ ਦਿੱਤੀ ਗਈ ਹੈ"
            "bn_IN" -> "বাংলা ভয়েস সক্রিয় করা হয়েছে"
            "mr_IN" -> "मराठी आवाज सक्रिय केला आहे"
            else -> "English voice alert activated."
        }
    }

    fun setSelectedLanguage(localeCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.setSelectedLanguage(localeCode)
            withContext(Dispatchers.Main) {
                localTts?.setLanguage(localeCode)
                val confirmText = getLanguageConfirmText(localeCode)
                // Speak the confirmation in the target language accent immediately 
                localTts?.speak(confirmText, boostVolume = false)
            }
        }
    }

    fun setAnnouncementStyle(style: String) {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.setAnnouncementStyle(style)
        }
    }

    fun setVolumeBoost(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.setVolumeBoost(enabled)
        }
    }

    fun clearAllPayments() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAll()
            lastPaymentReceived.value = null
        }
    }

    fun setOnboardingCompleted(completed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.setOnboardingDone(completed)
        }
    }

    fun setUserName(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.setUserName(name)
        }
    }

    fun setAppTheme(theme: String) {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.setAppTheme(theme)
        }
    }

    fun setSavedAccounts(accounts: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.setSavedAccounts(accounts)
        }
    }

    fun setSavedCategories(categories: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.setSavedCategories(categories)
        }
    }

    fun setSavedPayees(payees: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.setSavedPayees(payees)
        }
    }

    fun setSavedTags(tags: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.setSavedTags(tags)
        }
    }

    fun setActiveQrType(type: String) {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.setActiveQrType(type)
            withContext(Dispatchers.Main) {
                com.zovio.announcer.service.QrWidgetProvider.triggerWidgetUpdate(getApplication(), type)
            }
        }
    }

    /**
     * SIMULATE PAYMENT (Vocal Sound Test Only)
     * Exclusively triggers the voice speech loop for diagnostic testing, WITHOUT saving to the SQL/Room database.
     * Keeps user balances pristine by preventing mock/simulated money from corrupting the real received total.
     */
    fun simulatePayment(amount: Double, sender: String, appName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val rawText = "Notification info: Received ₹$amount from $sender on $appName"
            val entity = PaymentEntity(
                amount = amount,
                senderName = sender,
                appSource = appName,
                rawNotificationText = rawText
            )
            // DO NOT SAVE to Repository/Local database so stats and lists remain pristine

            // Update live state in-memory so the GUI can review the test played text
            lastPaymentReceived.value = entity
            lastPaymentIsTest.value = true

            // Speech announcement trigger
            val lang = selectedLanguage.value
            val style = announcementStyle.value
            val boost = volumeBoost.value
            val speech = localTts?.buildAnnouncementText(amount, sender, appName, style, lang) ?: ""

            withContext(Dispatchers.Main) {
                localTts?.setLanguage(lang)
                localTts?.speak(speech, boostVolume = boost)
            }
        }
    }

    fun speakText(text: String) {
        viewModelScope.launch {
            localTts?.setLanguage(selectedLanguage.value)
            localTts?.speak(text, boostVolume = volumeBoost.value)
        }
    }

    override fun onCleared() {
        super.onCleared()
        localTts?.shutdown()
    }
}
