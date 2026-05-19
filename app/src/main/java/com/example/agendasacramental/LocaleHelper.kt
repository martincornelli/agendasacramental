package com.example.agendasacramental

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleHelper {

    private const val PREFS_NAME = "agenda_prefs"
    private const val KEY_IDIOMA = "idioma"

    val IDIOMAS = listOf(
        "es" to "Español",
        "en" to "English"
    )

    fun getIdiomaGuardado(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_IDIOMA, "sistema") ?: "sistema"
    }

    fun setIdioma(context: Context, codigoIdioma: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_IDIOMA, codigoIdioma).apply()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (codigoIdioma == "sistema") {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            } else {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(codigoIdioma))
            }
        }
    }

    // Crear contexto con idioma específico (para CompositionLocalProvider)
    fun aplicarIdiomaAContexto(context: Context, idioma: String): Context {
        if (idioma == "sistema") return context
        val locale = Locale(idioma)
        Locale.setDefault(locale)
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    // Llamar desde attachBaseContext en MainActivity (API < 33)
    fun aplicarIdioma(context: Context): Context {
        val idioma = getIdiomaGuardado(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return context
        if (idioma == "sistema") return context
        val locale = Locale(idioma)
        Locale.setDefault(locale)
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    // Llamar desde onCreate en MainActivity (API 33+)
    fun aplicarAlArrancar(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val idioma = getIdiomaGuardado(context)
        if (idioma == "sistema") {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(idioma))
        }
    }
}