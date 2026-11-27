package com.kakuma.resourcelink.core.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.kakuma.resourcelink.core.datastore.PreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

/** The five platform languages. Native names are shown as-is in every locale. */
enum class AppLanguage(val tag: String, val nativeName: String, val flag: String) {
    ENGLISH("en", "English", "🇬🇧"),
    SWAHILI("sw", "Kiswahili", "🇰🇪"), // Kenya: Kakuma is in Kenya (matches the website)
    SPANISH("es", "Español", "🇪🇸"),
    ITALIAN("it", "Italiano", "🇮🇹"),
    GERMAN("de", "Deutsch", "🇩🇪");

    companion object {
        fun fromTag(tag: String?): AppLanguage = entries.firstOrNull { it.tag == tag?.take(2) } ?: ENGLISH
    }
}

@Singleton
class LanguageManager @Inject constructor(private val preferences: PreferencesRepository) {
    /** Persists the choice and switches the app locale (AppCompat backports per-app languages to API 26+). */
    suspend fun apply(language: AppLanguage) {
        preferences.setLanguage(language.tag)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }

    fun current(): AppLanguage {
        val locales = AppCompatDelegate.getApplicationLocales()
        return AppLanguage.fromTag(if (locales.isEmpty) java.util.Locale.getDefault().language else locales[0]?.language)
    }
}
