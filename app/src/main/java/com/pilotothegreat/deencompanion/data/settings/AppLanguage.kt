package com.pilotothegreat.deencompanion.data.settings

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * The UI language is applied through the per-app locale API. The chosen tag is also kept in
 * settings because receivers and widgets can't read AppCompat's stored locale below Android 13.
 */
object AppLanguage {
    /** Follow the system language. */
    const val SYSTEM = ""
    val supported = listOf("en", "ar")

    fun apply(tag: String) {
        AppCompatDelegate.setApplicationLocales(
            if (tag == SYSTEM) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
        )
    }

    /** Language picked in system settings on Android 13+, or null when not available. */
    fun fromSystemSettings(context: Context): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val locales = context.getSystemService(LocaleManager::class.java)?.applicationLocales ?: return null
        return if (locales.isEmpty) SYSTEM else locales[0].language
    }

    fun locale(tag: String): Locale = if (tag == SYSTEM) Locale.getDefault() else Locale.forLanguageTag(tag)

    /** Context whose resources use [tag], for UI rendered outside an activity. */
    fun localizedContext(context: Context, tag: String): Context {
        if (tag == SYSTEM) return context
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(tag))
            setLayoutDirection(Locale.forLanguageTag(tag))
        }
        return context.createConfigurationContext(configuration)
    }
}
