package com.app.spent.util

import java.util.Locale

import android.content.Context
import android.content.res.Configuration
import androidx.core.os.ConfigurationCompat
object LocaleHelper {

  /**
   * Retrieves the primary preferred Locale from system configuration using ConfigurationCompat.
   */
  fun getSystemPreferredLocale(context: Context): Locale {
    val localeList = ConfigurationCompat.getLocales(context.resources.configuration)
    return localeList.get(0) ?: Locale.getDefault()
  }

  /**
   * Returns the active Locale based on the user-selected language code or system default.
   */
  fun resolveLocale(context: Context, languageCode: String?): Locale {
    return when (languageCode) {
      "en" -> Locale.ENGLISH
      "es" -> Locale.forLanguageTag("es")
      "pt" -> Locale.forLanguageTag("pt")
      "fr" -> Locale.FRENCH
      "de" -> Locale.GERMAN
      "it" -> Locale.ITALIAN
      "ja" -> Locale.JAPANESE
      "hi" -> Locale.forLanguageTag("hi")
      else -> getSystemPreferredLocale(context)
    }
  }

  /**
   * Creates a Configuration configured with the specified language.
   */
  fun createLocalizedConfiguration(context: Context, languageCode: String?): Configuration {
    val targetLocale = resolveLocale(context, languageCode)
    val config = Configuration(context.resources.configuration)
    config.setLocale(targetLocale)
    return config
  }

  /**
   * Wraps a Context with the target Locale configuration.
   */
  fun createLocalizedContext(baseContext: Context, languageCode: String?): Context {
    val targetLocale = resolveLocale(baseContext, languageCode)
    Locale.setDefault(targetLocale)
    val config = Configuration(baseContext.resources.configuration)
    config.setLocale(targetLocale)
    return baseContext.createConfigurationContext(config)
  }

  /**
   * Returns the system currency symbol based on system locale / location.
   * If determination fails for any reason, falls back to "$".
   */
  fun getSystemCurrencySymbol(context: Context): String {
    return try {
      val sysLocale = getSystemPreferredLocale(context)
      val currency = java.util.Currency.getInstance(sysLocale)
      val symbol = currency.getSymbol(sysLocale)
      if (!symbol.isNullOrBlank()) symbol else "$"
    } catch (e: Exception) {
      try {
        val defaultLocale = Locale.getDefault()
        val currency = java.util.Currency.getInstance(defaultLocale)
        val symbol = currency.getSymbol(defaultLocale)
        if (!symbol.isNullOrBlank()) symbol else "$"
      } catch (e2: Exception) {
        "$"
      }
    }
  }

  /**
   * Returns the native/standard currency symbol associated with each supported language or system locale.
   */
  fun getDefaultCurrencySymbol(languageCode: String?, context: Context): String {
    return when (languageCode) {
      "hi" -> "₹"
      "ja" -> "¥"
      "pt" -> "R$"
      "de", "fr", "it" -> "€"
      "es" -> {
        val sysLocale = getSystemPreferredLocale(context)
        if (sysLocale.country.equals("ES", ignoreCase = true)) "€" else "$"
      }
      "en" -> {
        val sysLocale = getSystemPreferredLocale(context)
        when (sysLocale.country.uppercase()) {
          "GB" -> "£"
          "CA" -> "CA$"
          "AU" -> "A$"
          else -> "$"
        }
      }
      else -> getSystemCurrencySymbol(context)
    }
  }
}
