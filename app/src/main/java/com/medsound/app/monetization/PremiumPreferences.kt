package com.medsound.app.monetization

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.premiumDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "medsound_premium",
)

/**
 * Lokalny cache statusu Premium.
 * Źródłem prawdy przy starcie jest też [BillingManager] (queryPurchases).
 */
class PremiumPreferences(context: Context) {

    private val dataStore = context.applicationContext.premiumDataStore

    val isPremium: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_IS_PREMIUM] == true
    }

    suspend fun setPremium(purchased: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_IS_PREMIUM] = purchased
        }
    }

    companion object {
        private val KEY_IS_PREMIUM = booleanPreferencesKey("is_premium")
    }
}
