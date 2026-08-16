package com.urgentcall.guard

import android.content.Context
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import java.util.Locale

/**
 * Un pays sélectionnable pour la détection mobile/fixe : code ISO 3166-1
 * alpha-2, nom localisé et emoji drapeau généré à partir du code.
 */
data class CountryEntry(val code: String, val name: String) {
    val flag: String = code.uppercase(Locale.ROOT)
        .map { 0x1F1E6 + (it.code - 'A'.code) }
        .joinToString("") { codePoint -> String(Character.toChars(codePoint)) }

    val label: String get() = "$flag $name"
}

/**
 * Fournit la liste de tous les pays reconnus par libphonenumber (plutôt
 * qu'une liste figée codée en dur), pour permettre à l'utilisateur de
 * rechercher et ajouter n'importe quel pays dans les réglages de détection
 * mobile/fixe.
 */
object CountrySearchHelper {

    @Volatile
    private var cache: List<CountryEntry>? = null

    fun allCountries(context: Context): List<CountryEntry> {
        cache?.let { return it }
        synchronized(this) {
            cache?.let { return it }
            val phoneUtil = PhoneNumberUtil.createInstance(context.applicationContext)
            val entries = phoneUtil.supportedRegions
                .filter { it.length == 2 } // exclut les codes spéciaux type "001"
                .mapNotNull { code ->
                    val name = Locale("", code).getDisplayCountry(Locale.FRENCH)
                    if (name.isBlank() || name.equals(code, ignoreCase = true)) null
                    else CountryEntry(code, name)
                }
                .sortedBy { it.name }
            cache = entries
            return entries
        }
    }

    fun byCode(context: Context, code: String): CountryEntry? =
        allCountries(context).firstOrNull { it.code.equals(code, ignoreCase = true) }

    fun label(context: Context, code: String): String = byCode(context, code)?.label ?: code

    /**
     * Câble un AutoCompleteTextView de recherche de pays (utilisé à la fois
     * dans les Réglages et dans l'avertissement des dialogues d'ajout de
     * contact) : filtre en direct, exclut les pays déjà sélectionnés
     * (fournis via [excludedCodes]), et vide le champ après sélection.
     */
    fun setupSearchInput(
        context: Context,
        input: android.widget.AutoCompleteTextView,
        excludedCodes: () -> Set<String>,
        onCountryPicked: (String) -> Unit
    ) {
        input.setAdapter(CountryAutoCompleteAdapter(context, excludedCodes))
        input.threshold = 1
        input.setOnItemClickListener { parent, _, position, _ ->
            val entry = parent.getItemAtPosition(position) as CountryEntry
            onCountryPicked(entry.code)
            input.setText("")
        }
    }
}
