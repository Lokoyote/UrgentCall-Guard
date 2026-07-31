package com.urgentcall.guard

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class BlacklistEntry(
    val label: String,
    val phoneNumber: String
)

/**
 * Gère les numéros explicitement bloqués (ne recevront jamais de SMS ni ne
 * déclencheront de franchissement de volume), ainsi que les réglages
 * généraux pour les numéros inconnus (absents du répertoire) et masqués.
 */
object BlacklistHelper {

    private const val PREFS_NAME = "urgentcall_blacklist_v1"
    private const val KEY_ENTRIES = "entries"
    private const val KEY_BLOCK_HIDDEN = "blockHiddenNumbers"
    private const val KEY_BLOCK_UNKNOWN = "blockUnknownNumbers"

    private fun normalize(number: String): String = number.filter { it.isDigit() }.takeLast(9)

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getEntries(context: Context): List<BlacklistEntry> {
        val raw = prefs(context).getString(KEY_ENTRIES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                BlacklistEntry(obj.getString("label"), obj.getString("phoneNumber"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveEntries(context: Context, entries: List<BlacklistEntry>) {
        val arr = JSONArray()
        entries.forEach {
            val obj = JSONObject()
            obj.put("label", it.label)
            obj.put("phoneNumber", it.phoneNumber)
            arr.put(obj)
        }
        prefs(context).edit().putString(KEY_ENTRIES, arr.toString()).apply()
    }

    fun addEntry(context: Context, entry: BlacklistEntry) {
        val updated = getEntries(context).toMutableList()
        updated.add(entry)
        saveEntries(context, updated)
    }

    fun removeEntry(context: Context, phoneNumber: String) {
        val updated = getEntries(context).filterNot { normalize(it.phoneNumber) == normalize(phoneNumber) }
        saveEntries(context, updated)
    }

    fun isBlacklisted(context: Context, phoneNumber: String): Boolean {
        val target = normalize(phoneNumber)
        if (target.isEmpty()) return false
        return getEntries(context).any { normalize(it.phoneNumber) == target }
    }

    /** Un numéro masqué / privé : pas de numéro exploitable transmis par l'opérateur. */
    fun isHiddenNumber(phoneNumber: String?): Boolean =
        phoneNumber.isNullOrBlank() || normalize(phoneNumber).length < 3

    fun isBlockHiddenNumbers(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BLOCK_HIDDEN, true)

    fun setBlockHiddenNumbers(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_BLOCK_HIDDEN, value).apply()
    }

    fun isBlockUnknownNumbers(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BLOCK_UNKNOWN, false)

    fun setBlockUnknownNumbers(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_BLOCK_UNKNOWN, value).apply()
    }
}
