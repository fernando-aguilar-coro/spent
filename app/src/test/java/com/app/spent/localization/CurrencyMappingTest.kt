package com.app.spent.localization

import com.app.spent.util.LocaleHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Locale

class CurrencyMappingTest {

    @Test
    fun testCustomCurrencySymbolsMapping() {
        assertEquals("Bs", LocaleHelper.getSymbolForCurrencyCode("BOB"))
        assertEquals("$", LocaleHelper.getSymbolForCurrencyCode("USD"))
        assertEquals("Bs.", LocaleHelper.getSymbolForCurrencyCode("VES"))
        assertEquals("€", LocaleHelper.getSymbolForCurrencyCode("EUR"))
        assertEquals("S/", LocaleHelper.getSymbolForCurrencyCode("PEN"))
        assertEquals("$", LocaleHelper.getSymbolForCurrencyCode("COP"))
        assertEquals("$", LocaleHelper.getSymbolForCurrencyCode("MXN"))
        assertEquals("$", LocaleHelper.getSymbolForCurrencyCode("ARS"))
        assertEquals("$", LocaleHelper.getSymbolForCurrencyCode("CLP"))
        assertEquals("R$", LocaleHelper.getSymbolForCurrencyCode("BRL"))
        assertEquals("£", LocaleHelper.getSymbolForCurrencyCode("GBP"))
        assertEquals("CA$", LocaleHelper.getSymbolForCurrencyCode("CAD"))
        assertEquals("A$", LocaleHelper.getSymbolForCurrencyCode("AUD"))
        assertEquals("¥", LocaleHelper.getSymbolForCurrencyCode("JPY"))
        assertEquals("¥", LocaleHelper.getSymbolForCurrencyCode("CNY"))
        assertEquals("₹", LocaleHelper.getSymbolForCurrencyCode("INR"))
        assertEquals("₲", LocaleHelper.getSymbolForCurrencyCode("PYG"))
        assertEquals("₡", LocaleHelper.getSymbolForCurrencyCode("CRC"))
    }

    @Test
    fun testCountryToCurrencyMapping() {
        assertEquals("BOB", LocaleHelper.COUNTRY_TO_CURRENCY_CODE["BO"])
        assertEquals("USD", LocaleHelper.COUNTRY_TO_CURRENCY_CODE["US"])
        assertEquals("VES", LocaleHelper.COUNTRY_TO_CURRENCY_CODE["VE"])
        assertEquals("EUR", LocaleHelper.COUNTRY_TO_CURRENCY_CODE["ES"])
        assertEquals("PEN", LocaleHelper.COUNTRY_TO_CURRENCY_CODE["PE"])
        assertEquals("COP", LocaleHelper.COUNTRY_TO_CURRENCY_CODE["CO"])
        assertEquals("MXN", LocaleHelper.COUNTRY_TO_CURRENCY_CODE["MX"])
    }

    @Test
    fun testSystemCurrencyResolutionFallback() {
        val originalDefault = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("es-BO"))
            assertEquals("Bs", LocaleHelper.getSystemCurrencySymbol())

            Locale.setDefault(Locale.forLanguageTag("es-VE"))
            assertEquals("Bs.", LocaleHelper.getSystemCurrencySymbol())

            Locale.setDefault(Locale.forLanguageTag("es-ES"))
            assertEquals("€", LocaleHelper.getSystemCurrencySymbol())

            Locale.setDefault(Locale.forLanguageTag("en-US"))
            assertEquals("$", LocaleHelper.getSystemCurrencySymbol())

            Locale.setDefault(Locale.forLanguageTag("es-PE"))
            assertEquals("S/", LocaleHelper.getSystemCurrencySymbol())

            Locale.setDefault(Locale.ROOT)
            assertNotNull(LocaleHelper.getSystemCurrencySymbol())
        } finally {
            Locale.setDefault(originalDefault)
        }
    }

    @Test
    fun testEffectiveSymbolFallback() {
        assertEquals("Bs", LocaleHelper.getEffectiveSymbol("BOB"))
        assertEquals("$", LocaleHelper.getEffectiveSymbol("USD"))
        assertEquals("CHF", LocaleHelper.getEffectiveSymbol("CHF"))
        assertEquals("XYZ", LocaleHelper.getEffectiveSymbol("XYZ"))
    }

    @Test
    fun testSupportedCurrenciesList() {
        assert(LocaleHelper.SUPPORTED_CURRENCIES.isNotEmpty())
        val bob = LocaleHelper.getCurrencyItemForSymbol("Bs")
        assertNotNull(bob)
        assertEquals("BOB", bob?.code)
    }
}
