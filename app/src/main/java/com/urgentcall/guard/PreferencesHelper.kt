package com.urgentcall.guard

import android.content.Context

/**
 * Centralise la lecture/écriture des réglages de l'application via SharedPreferences.
 * Correspond aux paramètres définis côté prototype (AppSettings dans types.ts).
 */
object PreferencesHelper {

    private const val PREFS_NAME = "urgentcall_settings_v1"

    private const val KEY_SERVICE_ENABLED = "serviceEnabled"
    private const val KEY_VOLUME_THRESHOLD = "volumeThreshold"
    private const val KEY_TIMER_MINUTES = "timerMinutes"
    private const val KEY_SMS_TEMPLATE = "smsTemplate"
    private const val KEY_IMMEDIATE_WHITELIST = "immediateWhitelistBreakthrough"
    private const val KEY_ALLOW_SYSTEM_FAVORITES = "allowSystemFavorites"
    private const val KEY_AUTO_RESTORE_DELAY = "autoRestoreDelayAfterCallSec"

    private const val DEFAULT_SMS_TEMPLATE =
        "[UrgentCall Guard] Mon telephone est en mode silencieux. S'il s'agit d'une URGENCE, rappelez-moi dans les {TIMER} min pour faire sonner mon telephone a volume MAX."

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isServiceEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SERVICE_ENABLED, true)

    fun setServiceEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply()
    }

    fun getVolumeThreshold(context: Context): Int =
        prefs(context).getInt(KEY_VOLUME_THRESHOLD, 20)

    fun setVolumeThreshold(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_VOLUME_THRESHOLD, value).apply()
    }

    fun getTimerMinutes(context: Context): Int =
        prefs(context).getInt(KEY_TIMER_MINUTES, 5)

    fun setTimerMinutes(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_TIMER_MINUTES, value).apply()
    }

    fun getSmsTemplate(context: Context): String =
        prefs(context).getString(KEY_SMS_TEMPLATE, DEFAULT_SMS_TEMPLATE) ?: DEFAULT_SMS_TEMPLATE

    fun setSmsTemplate(context: Context, value: String) {
        prefs(context).edit().putString(KEY_SMS_TEMPLATE, value).apply()
    }

    fun isImmediateWhitelistBreakthrough(context: Context): Boolean =
        prefs(context).getBoolean(KEY_IMMEDIATE_WHITELIST, true)

    fun setImmediateWhitelistBreakthrough(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_IMMEDIATE_WHITELIST, value).apply()
    }

    fun isAllowSystemFavorites(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ALLOW_SYSTEM_FAVORITES, true)

    fun setAllowSystemFavorites(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_ALLOW_SYSTEM_FAVORITES, value).apply()
    }

    fun getAutoRestoreDelaySec(context: Context): Int =
        prefs(context).getInt(KEY_AUTO_RESTORE_DELAY, 3)
}
