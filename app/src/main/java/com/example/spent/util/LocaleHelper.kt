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
      "es" -> Locale("es")
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
}
