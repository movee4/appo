package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.model.PiHoleConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.nio.charset.StandardCharsets

private val Context.dataStore by preferencesDataStore(name = "network_commander_prefs")

class EncryptedSettingsDataStore(private val context: Context) {

    companion object {
        private const val ENCRYPTED_PREFS_NAME = "network_commander_secure_secrets"
        private const val KEY_SECURE_API_PASSWORD = "secure_pihole_api_password"

        private val KEY_PIHOLE_HOST = stringPreferencesKey("pihole_host")
        private val KEY_PIHOLE_PORT = intPreferencesKey("pihole_port")
        private val KEY_PIHOLE_HTTPS = booleanPreferencesKey("pihole_https")
        private val KEY_PIHOLE_SID = stringPreferencesKey("pihole_sid")
        private val KEY_NAS_HOST = stringPreferencesKey("nas_host")
        private val KEY_NAS_MAC = stringPreferencesKey("nas_mac")
        private val KEY_NAS_NAME = stringPreferencesKey("nas_name")
        private val KEY_DEMO_MODE = booleanPreferencesKey("demo_mode")
        private val KEY_NICKNAMES_JSON = stringPreferencesKey("client_nicknames_json")
    }

    /**
     * Hardware-backed EncryptedSharedPreferences using AES256-GCM.
     * Includes a graceful fallback if AndroidKeyStore is unavailable (e.g. during local tests).
     */
    private val securePrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            // Local fallback for unit test / Robolectric execution where AndroidKeyStore may not be initialized
            context.getSharedPreferences("${ENCRYPTED_PREFS_NAME}_compat", Context.MODE_PRIVATE)
        }
    }

    val configFlow: Flow<PiHoleConfig> = context.dataStore.data.map { prefs ->
        val host = prefs[KEY_PIHOLE_HOST] ?: "192.168.1.100"
        val port = prefs[KEY_PIHOLE_PORT] ?: 80
        val useHttps = prefs[KEY_PIHOLE_HTTPS] ?: false
        val sid = prefs[KEY_PIHOLE_SID] ?: ""
        val nasHost = prefs[KEY_NAS_HOST] ?: "192.168.1.100"
        val nasMac = prefs[KEY_NAS_MAC] ?: "00:11:32:8A:2B:1C"
        val nasName = prefs[KEY_NAS_NAME] ?: "Synology NAS"
        val isDemo = prefs[KEY_DEMO_MODE] ?: false

        val securePassword = securePrefs.getString(KEY_SECURE_API_PASSWORD, "") ?: ""

        PiHoleConfig(
            host = host,
            port = port,
            useHttps = useHttps,
            apiPassword = securePassword,
            sid = sid,
            nasHost = nasHost,
            nasMacAddress = nasMac,
            nasDeviceName = nasName,
            isDemoMode = isDemo
        )
    }

    suspend fun saveConfig(config: PiHoleConfig) {
        // 1. Securely save API Password in EncryptedSharedPreferences
        securePrefs.edit()
            .putString(KEY_SECURE_API_PASSWORD, config.apiPassword)
            .apply()

        // 2. Save general connection metadata in DataStore
        context.dataStore.edit { prefs ->
            prefs[KEY_PIHOLE_HOST] = config.host.trim()
            prefs[KEY_PIHOLE_PORT] = config.port
            prefs[KEY_PIHOLE_HTTPS] = config.useHttps
            prefs[KEY_PIHOLE_SID] = config.sid
            prefs[KEY_NAS_HOST] = config.nasHost.trim()
            prefs[KEY_NAS_MAC] = config.nasMacAddress.trim()
            prefs[KEY_NAS_NAME] = config.nasDeviceName.trim()
            prefs[KEY_DEMO_MODE] = config.isDemoMode
        }
    }

    suspend fun saveSessionSid(sid: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PIHOLE_SID] = sid
        }
    }

    suspend fun saveClientNickname(ip: String, nickname: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_NICKNAMES_JSON] ?: "{}"
            val obj = try { JSONObject(current) } catch (_: Exception) { JSONObject() }
            obj.put(ip, nickname)
            prefs[KEY_NICKNAMES_JSON] = obj.toString()
        }
    }

    val nicknamesFlow: Flow<Map<String, String>> = context.dataStore.data.map { prefs ->
        val current = prefs[KEY_NICKNAMES_JSON] ?: "{}"
        val map = mutableMapOf<String, String>()
        try {
            val obj = JSONObject(current)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = obj.getString(key)
            }
        } catch (_: Exception) {}
        map
    }
}
