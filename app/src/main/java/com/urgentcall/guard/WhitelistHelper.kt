package com.urgentcall.guard

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class WhitelistContact(
    val name: String,
    val phoneNumber: String,
    val isPriority: Boolean,
    // true si ajouté automatiquement depuis les favoris du téléphone : dans ce
    // cas l'entrée n'est pas supprimable individuellement (voir removeContact).
    val isSystemFavorite: Boolean = false
)

/**
 * Gère la liste blanche des contacts qui peuvent forcer la sonnerie
 * immédiatement, sans attendre le premier appel manqué + SMS.
 * Tout contact de la liste blanche déclenche systématiquement ce
 * franchissement immédiat — il n'y a pas de réglage à part pour ça.
 * Les numéros sont stockés normalisés (chiffres uniquement) pour la comparaison.
 */
object WhitelistHelper {

    private const val PREFS_NAME = "urgentcall_whitelist_v1"
    private const val KEY_CONTACTS = "contacts"

    private fun normalize(number: String): String =
        number.filter { it.isDigit() }.takeLast(9) // compare sur les 9 derniers chiffres (évite pb d'indicatif)

    fun getContacts(context: Context): List<WhitelistContact> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CONTACTS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                WhitelistContact(
                    name = obj.getString("name"),
                    phoneNumber = obj.getString("phoneNumber"),
                    isPriority = obj.optBoolean("isPriority", true),
                    isSystemFavorite = obj.optBoolean("isSystemFavorite", false)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveContacts(context: Context, contacts: List<WhitelistContact>) {
        val arr = JSONArray()
        contacts.forEach {
            val obj = JSONObject()
            obj.put("name", it.name)
            obj.put("phoneNumber", it.phoneNumber)
            obj.put("isPriority", it.isPriority)
            obj.put("isSystemFavorite", it.isSystemFavorite)
            arr.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_CONTACTS, arr.toString()).apply()
    }

    fun addContact(context: Context, contact: WhitelistContact) {
        val updated = getContacts(context).toMutableList()
        updated.add(contact)
        saveContacts(context, updated)
    }

    /**
     * Retire un contact de la liste blanche — sauf si c'est une entrée
     * synchronisée depuis les favoris du téléphone : celles-ci ne sont pas
     * décochables individuellement, seul le switch "Inclure les favoris..."
     * dans l'onglet liste blanche permet de toutes les retirer d'un coup.
     */
    fun removeContact(context: Context, phoneNumber: String) {
        val target = normalize(phoneNumber)
        val updated = getContacts(context).filterNot {
            normalize(it.phoneNumber) == target && !it.isSystemFavorite
        }
        saveContacts(context, updated)
    }

    /**
     * Aligne la liste blanche sur les favoris actuels du répertoire du téléphone.
     * - Si l'option est désactivée : retire toutes les entrées synchronisées précédemment.
     * - Si activée : ajoute les nouveaux favoris (sans dupliquer un numéro déjà présent,
     *   qu'il soit manuel ou déjà synchronisé) et retire les entrées synchronisées dont
     *   le contact n'est plus favori côté téléphone.
     * À appeler à l'ouverture de l'onglet liste blanche et quand le switch change d'état.
     */
    fun syncSystemFavorites(context: Context) {
        val current = getContacts(context)

        if (!PreferencesHelper.isAllowSystemFavorites(context)) {
            val cleaned = current.filterNot { it.isSystemFavorite }
            if (cleaned.size != current.size) saveContacts(context, cleaned)
            return
        }

        val favorites = ContactsHelper.getFavoriteContacts(context)
        val favoriteKeys = favorites.map { normalize(it.phoneNumber) }.toSet()

        // Retire les favoris synchronisés qui ne sont plus favoris côté téléphone.
        val stillValid = current.filter { !it.isSystemFavorite || normalize(it.phoneNumber) in favoriteKeys }

        // Ajoute les favoris pas encore présents (manuellement ou déjà synchronisés).
        val existingKeys = stillValid.map { normalize(it.phoneNumber) }.toSet()
        val toAdd = favorites
            .filter { normalize(it.phoneNumber).isNotEmpty() && normalize(it.phoneNumber) !in existingKeys }
            .map { WhitelistContact(it.name, it.phoneNumber, isPriority = true, isSystemFavorite = true) }

        val updated = stillValid + toAdd
        if (updated != current) saveContacts(context, updated)
    }

    fun isWhitelisted(context: Context, phoneNumber: String): Boolean {
        val target = normalize(phoneNumber)
        return getContacts(context).any { normalize(it.phoneNumber) == target }
    }
}
