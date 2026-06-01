package com.zovio.announcer.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {

    companion object {
        val KEY_SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
        val KEY_IS_SERVICE_ENABLED = booleanPreferencesKey("is_service_enabled")
        val KEY_ANNOUNCEMENT_STYLE = stringPreferencesKey("announcement_style") // "short" or "detailed"
        val KEY_VOLUME_BOOST = booleanPreferencesKey("volume_boost")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val KEY_ACTIVE_QR_TYPE = stringPreferencesKey("active_qr_type")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_APP_THEME = stringPreferencesKey("app_theme")
        val KEY_SAVED_ACCOUNTS = stringPreferencesKey("saved_accounts")
        val KEY_SAVED_CATEGORIES = stringPreferencesKey("saved_categories")
        val KEY_SAVED_PAYEES = stringPreferencesKey("saved_payees")
        val KEY_SAVED_TAGS = stringPreferencesKey("saved_tags")

        val SUPPORTED_LANGUAGES = mapOf(
            "English" to "en_IN",
            "Hindi" to "hi_IN",
            "Telugu" to "te_IN",
            "Tamil" to "ta_IN",
            "Kannada" to "kn_IN",
            "Malayalam" to "ml_IN",
            "Gujarati" to "gu_IN",
            "Punjabi" to "pa_IN",
            "Bengali" to "bn_IN",
            "Marathi" to "mr_IN"
        )
    }

    val selectedLanguage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_LANGUAGE] ?: "en_IN"
    }

    val isServiceEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_SERVICE_ENABLED] ?: true
    }

    val announcementStyle: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_ANNOUNCEMENT_STYLE] ?: "detailed"
    }

    val volumeBoost: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_VOLUME_BOOST] ?: true
    }

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_DONE] ?: false
    }

    val activeQrType: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_QR_TYPE] ?: "none"
    }

    val appTheme: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_APP_THEME] ?: "system"
    }

    val savedAccounts: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_SAVED_ACCOUNTS]?.split("|||")?.filter { it.isNotBlank() } ?: emptyList()
    }

    val savedCategories: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_SAVED_CATEGORIES]?.split("|||")?.filter { it.isNotBlank() } ?: emptyList()
    }

    val savedPayees: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_SAVED_PAYEES]?.split("|||")?.filter { it.isNotBlank() } ?: emptyList()
    }

    val savedTags: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_SAVED_TAGS]?.split("|||")?.filter { it.isNotBlank() } ?: emptyList()
    }

    val userName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_NAME] ?: "Siva"
    }

    suspend fun getActiveQrType(): String {
        return context.dataStore.data.map { prefs ->
            prefs[KEY_ACTIVE_QR_TYPE] ?: "none"
        }.first()
    }

    suspend fun setActiveQrType(type: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_QR_TYPE] = type
        }
    }

    suspend fun setAppTheme(theme: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_APP_THEME] = theme
        }
    }

    suspend fun setSavedAccounts(accounts: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SAVED_ACCOUNTS] = accounts.joinToString("|||")
        }
    }

    suspend fun setSavedCategories(categories: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SAVED_CATEGORIES] = categories.joinToString("|||")
        }
    }

    suspend fun setSavedPayees(payees: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SAVED_PAYEES] = payees.joinToString("|||")
        }
    }

    suspend fun setSavedTags(tags: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SAVED_TAGS] = tags.joinToString("|||")
        }
    }

    suspend fun setSelectedLanguage(localeCode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_LANGUAGE] = localeCode
        }
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_SERVICE_ENABLED] = enabled
        }
    }

    suspend fun setAnnouncementStyle(style: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ANNOUNCEMENT_STYLE] = style
        }
    }

    suspend fun setVolumeBoost(boost: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_VOLUME_BOOST] = boost
        }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_DONE] = done
        }
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_NAME] = name
        }
    }
}
