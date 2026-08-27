package com.app.spent.util

import java.util.Currency
import java.util.Locale

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.core.os.ConfigurationCompat

data class CurrencyItem(
  val code: String,
  val symbol: String,
  val name: String
)

object LocaleHelper {

  /**
   * Custom mapping of ISO-4217 Currency Codes to their user-friendly, standard display symbols.
   */
  val CUSTOM_CURRENCY_SYMBOLS: Map<String, String> = mapOf(
    "BOB" to "Bs",
    "USD" to "$",
    "VES" to "Bs.",
    "EUR" to "€",
    "PEN" to "S/",
    "COP" to "$",
    "MXN" to "$",
    "ARS" to "$",
    "CLP" to "$",
    "UYU" to "$",
    "PYG" to "₲",
    "CRC" to "₡",
    "GTQ" to "Q",
    "HNL" to "L",
    "NIO" to "C$",
    "DOP" to "RD$",
    "PAB" to "B/.",
    "GBP" to "£",
    "BRL" to "R$",
    "CAD" to "CA$",
    "AUD" to "A$",
    "NZD" to "NZ$",
    "JPY" to "¥",
    "CNY" to "¥",
    "INR" to "₹",
    "KRW" to "₩",
    "CHF" to "CHF",
    "SEK" to "kr",
    "NOK" to "kr",
    "DKK" to "kr",
    "PLN" to "zł",
    "TRY" to "₺",
    "RUB" to "₽",
    "SAR" to "﷼",
    "AED" to "د.إ",
    "ZAR" to "R",
    "SGD" to "S$",
    "HKD" to "HK$",
    "THB" to "฿",
    "IDR" to "Rp",
    "MYR" to "RM",
    "PHP" to "₱",
    "VND" to "₫",
    "EGP" to "E£",
    "ILS" to "₪"
  )

  val SUPPORTED_CURRENCIES: List<CurrencyItem> = listOf(
    CurrencyItem("BOB", "Bs", "Bolivian Boliviano (Bs • BOB)"),
    CurrencyItem("USD", "$", "US Dollar ($ • USD)"),
    CurrencyItem("VES", "Bs.", "Venezuelan Bolívar (Bs. • VES)"),
    CurrencyItem("EUR", "€", "Euro (€ • EUR)"),
    CurrencyItem("PEN", "S/", "Peruvian Sol (S/ • PEN)"),
    CurrencyItem("COP", "$", "Colombian Peso ($ • COP)"),
    CurrencyItem("MXN", "$", "Mexican Peso ($ • MXN)"),
    CurrencyItem("ARS", "$", "Argentine Peso ($ • ARS)"),
    CurrencyItem("CLP", "$", "Chilean Peso ($ • CLP)"),
    CurrencyItem("BRL", "R$", "Brazilian Real (R$ • BRL)"),
    CurrencyItem("GBP", "£", "British Pound (£ • GBP)"),
    CurrencyItem("CAD", "CA$", "Canadian Dollar (CA$ • CAD)"),
    CurrencyItem("AUD", "A$", "Australian Dollar (A$ • AUD)"),
    CurrencyItem("JPY", "¥", "Japanese Yen (¥ • JPY)"),
    CurrencyItem("CNY", "¥", "Chinese Yuan (¥ • CNY)"),
    CurrencyItem("INR", "₹", "Indian Rupee (₹ • INR)"),
    CurrencyItem("CHF", "CHF", "Swiss Franc (CHF • CHF)"),
    CurrencyItem("PYG", "₲", "Paraguayan Guaraní (₲ • PYG)"),
    CurrencyItem("UYU", "$", "Uruguayan Peso ($ • UYU)"),
    CurrencyItem("CRC", "₡", "Costa Rican Colón (₡ • CRC)"),
    CurrencyItem("GTQ", "Q", "Guatemalan Quetzal (Q • GTQ)"),
    CurrencyItem("HNL", "L", "Honduran Lempira (L • HNL)"),
    CurrencyItem("NIO", "C$", "Nicaraguan Córdoba (C$ • NIO)"),
    CurrencyItem("DOP", "RD$", "Dominican Peso (RD$ • DOP)"),
    CurrencyItem("PAB", "B/.", "Panamanian Balboa (B/. • PAB)"),
    CurrencyItem("KRW", "₩", "South Korean Won (₩ • KRW)"),
    CurrencyItem("RUB", "₽", "Russian Ruble (₽ • RUB)"),
    CurrencyItem("TRY", "₺", "Turkish Lira (₺ • TRY)"),
    CurrencyItem("SAR", "﷼", "Saudi Riyal (﷼ • SAR)"),
    CurrencyItem("AED", "د.إ", "UAE Dirham (د.إ • AED)"),
    CurrencyItem("ZAR", "R", "South African Rand (R • ZAR)"),
    CurrencyItem("SGD", "S$", "Singapore Dollar (S$ • SGD)"),
    CurrencyItem("NZD", "NZ$", "New Zealand Dollar (NZ$ • NZD)"),
    CurrencyItem("HKD", "HK$", "Hong Kong Dollar (HK$ • HKD)"),
    CurrencyItem("SEK", "kr", "Swedish Krona (kr • SEK)"),
    CurrencyItem("NOK", "kr", "Norwegian Krone (kr • NOK)"),
    CurrencyItem("DKK", "kr", "Danish Krone (kr • DKK)"),
    CurrencyItem("PLN", "zł", "Polish Zloty (zł • PLN)"),
    CurrencyItem("THB", "฿", "Thai Baht (฿ • THB)"),
    CurrencyItem("IDR", "Rp", "Indonesian Rupiah (Rp • IDR)"),
    CurrencyItem("MYR", "RM", "Malaysian Ringgit (RM • MYR)"),
    CurrencyItem("PHP", "₱", "Philippine Peso (₱ • PHP)"),
    CurrencyItem("VND", "₫", "Vietnamese Dong (₫ • VND)")
  )

  val COUNTRY_TO_CURRENCY_CODE: Map<String, String> = mapOf(
    "BO" to "BOB",
    "US" to "USD",
    "VE" to "VES",
    "ES" to "EUR",
    "DE" to "EUR",
    "FR" to "EUR",
    "IT" to "EUR",
    "PT" to "EUR",
    "NL" to "EUR",
    "BE" to "EUR",
    "AT" to "EUR",
    "IE" to "EUR",
    "FI" to "EUR",
    "GR" to "EUR",
    "PE" to "PEN",
    "CO" to "COP",
    "MX" to "MXN",
    "AR" to "ARS",
    "CL" to "CLP",
    "BR" to "BRL",
    "UY" to "UYU",
    "PY" to "PYG",
    "EC" to "USD",
    "PA" to "USD",
    "SV" to "USD",
    "CR" to "CRC",
    "GT" to "GTQ",
    "HN" to "HNL",
    "NI" to "NIO",
    "DO" to "DOP",
    "GB" to "GBP",
    "CA" to "CAD",
    "AU" to "AUD",
    "NZ" to "NZD",
    "JP" to "JPY",
    "CN" to "CNY",
    "IN" to "INR",
    "KR" to "KRW",
    "RU" to "RUB",
    "TR" to "TRY",
    "CH" to "CHF",
    "SE" to "SEK",
    "NO" to "NOK",
    "DK" to "DKK",
    "PL" to "PLN",
    "TH" to "THB",
    "ID" to "IDR",
    "MY" to "MYR",
    "PH" to "PHP",
    "VN" to "VND",
    "SA" to "SAR",
    "AE" to "AED",
    "ZA" to "ZAR",
    "SG" to "SGD",
    "HK" to "HKD",
    "IL" to "ILS",
    "EG" to "EGP"
  )

  fun getSymbolForCurrencyCode(currencyCode: String): String? {
    return CUSTOM_CURRENCY_SYMBOLS[currencyCode.uppercase()]
  }

  fun getEffectiveSymbol(currencyCode: String): String {
    return CUSTOM_CURRENCY_SYMBOLS[currencyCode.uppercase()] ?: currencyCode.uppercase()
  }

  fun getCurrencyItemForSymbol(symbol: String): CurrencyItem? {
    return SUPPORTED_CURRENCIES.find { it.symbol == symbol }
  }

  /**
   * Retrieves the primary preferred Locale from system configuration using ConfigurationCompat.
   */
  fun getSystemPreferredLocale(context: Context? = null): Locale {
    try {
      val sysConfig = Resources.getSystem().configuration
      val locales = ConfigurationCompat.getLocales(sysConfig)
      if (!locales.isEmpty) {
        val loc = locales.get(0)
        if (loc != null) return loc
      }
    } catch (e: Exception) {}

    if (context != null) {
      try {
        val locales = ConfigurationCompat.getLocales(context.resources.configuration)
        if (!locales.isEmpty) {
          val loc = locales.get(0)
          if (loc != null) return loc
        }
      } catch (e: Exception) {}
    }

    return Locale.getDefault()
  }

  /**
   * Returns an ordered list of all preferred system locales from Android System Preferences.
   */
  fun getAllSystemPreferredLocales(context: Context? = null): List<Locale> {
    val result = mutableListOf<Locale>()

    try {
      val sysConfig = Resources.getSystem().configuration
      val locales = ConfigurationCompat.getLocales(sysConfig)
      for (i in 0 until locales.size()) {
        locales.get(i)?.let { if (!result.contains(it)) result.add(it) }
      }
    } catch (e: Exception) {}

    if (context != null) {
      try {
        val locales = ConfigurationCompat.getLocales(context.resources.configuration)
        for (i in 0 until locales.size()) {
          locales.get(i)?.let { if (!result.contains(it)) result.add(it) }
        }
      } catch (e: Exception) {}
    }

    val defaultLocale = Locale.getDefault()
    if (!result.contains(defaultLocale)) {
      result.add(defaultLocale)
    }

    return result
  }

  /**
   * Resolves the currency symbol strictly from System Preferences (Language / Region settings).
   * Checks country and custom mappings first, falls back to Currency.getInstance, then to "$".
   */
  fun getSystemCurrencySymbol(context: Context? = null): String {
    val locales = getAllSystemPreferredLocales(context)

    for (loc in locales) {
      val country = loc.country.trim().uppercase()
      if (country.isNotBlank()) {
        // 1. Direct country to custom currency symbol mapping
        val mappedCurrencyCode = COUNTRY_TO_CURRENCY_CODE[country]
        if (mappedCurrencyCode != null) {
          val customSym = CUSTOM_CURRENCY_SYMBOLS[mappedCurrencyCode]
          if (!customSym.isNullOrBlank()) return customSym
        }

        // 2. Currency.getInstance(Locale) with custom symbol lookup
        try {
          val currency = Currency.getInstance(loc)
          val code = currency.currencyCode
          val customSym = CUSTOM_CURRENCY_SYMBOLS[code]
          if (!customSym.isNullOrBlank()) return customSym

          val symbol = currency.getSymbol(loc)
          if (!symbol.isNullOrBlank() && symbol != code) return symbol
        } catch (e: Exception) {}
      }
    }

    // 3. If no country or country matching failed, check system language
    for (loc in locales) {
      val lang = loc.language.lowercase()
      when (lang) {
        "hi" -> return "₹"
        "ja" -> return "¥"
        "pt" -> return "R$"
        "de", "fr", "it" -> return "€"
      }
    }

    // 4. Default fallback
    return "$"
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
   * Returns default currency symbol based on system preferences.
   */
  fun getDefaultCurrencySymbol(languageCode: String?, context: Context? = null): String {
    return getSystemCurrencySymbol(context)
  }
}
