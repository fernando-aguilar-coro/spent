package com.app.spent.util

import java.util.Locale
import kotlin.math.abs

/**
 * Universal Currency Formatter for Spent.
 * Intelligently handles both prefix symbols ($, €, £, ¥) and suffix/word currencies
 * (Bs., ARS, BOB, S/, kr, zł) with consistent number formatting and sign placement.
 */
object CurrencyFormatter {

    fun format(
        amount: Double,
        currencySymbol: String = "$",
        currencyCode: String? = null,
        includePositiveSign: Boolean = false
    ): String {
        val symbolToUse = if (currencySymbol == "$" && !currencyCode.isNullOrBlank() && currencyCode != "USD") {
            LocaleHelper.getCurrencyItemForCode(currencyCode)?.symbol ?: currencySymbol
        } else {
            currencySymbol
        }
        val cleanSymbol = symbolToUse.trim()
        val absAmount = abs(amount)
        val formattedNumber = String.format(Locale.US, "%,.2f", absAmount)

        val sign = when {
            amount < 0 -> "-"
            includePositiveSign && amount > 0 -> "+"
            else -> ""
        }

        return if (isSuffixCurrency(cleanSymbol, currencyCode)) {
            // Suffix formatting e.g. "150.00 kr"
            "$sign$formattedNumber $cleanSymbol"
        } else {
            // Prefix formatting e.g. "$1,250.00", "+€50.00", "Bs. 1,250.00", "ARS 2,500.00"
            if (cleanSymbol.length > 1 && !cleanSymbol.endsWith("$")) {
                "$sign$cleanSymbol $formattedNumber"
            } else {
                "$sign$cleanSymbol$formattedNumber"
            }
        }
    }

    /**
     * Determines whether a given currency symbol or code is naturally formatted as a suffix.
     */
    fun isSuffixCurrency(symbol: String, code: String?): Boolean {
        val s = symbol.trim().lowercase()
        val c = code?.trim()?.uppercase()

        return s.endsWith("kr") ||
                s == "zł" ||
                s == "ft" ||
                c in listOf("SEK", "NOK", "DKK", "PLN", "HUF", "CZK")
    }
}
