package com.app.spent.localization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import java.util.regex.Pattern
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Automated Unit Test suite to verify localization resources integrity:
 * - Checks key parity across all supported language directories.
 * - Detects missing, untranslated, or orphaned keys.
 * - Validates that format specifiers (e.g. %1$s, %2$.0f, %1$d) match the base strings.
 * - Validates non-empty content and well-formed XML resource elements.
 */
class StringResourcesLocalizationTest {

    companion object {
        private val EXPECTED_LOCALES = listOf("values", "values-es", "values-pt", "values-fr")
        private val FORMAT_SPECIFIER_REGEX = Pattern.compile("%(\\d+\\$)?[-#+ 0,(]*\\d*(\\.\\d+)?[a-zA-Z%]")

        private lateinit var resDir: File
        private val stringMapByLocale = mutableMapOf<String, Map<String, String>>()

        @JvmStatic
        @BeforeClass
        fun setUpAll() {
            // Find res folder whether test is executed from project root or module root
            val candidatePaths = listOf(
                File("src/main/res"),
                File("app/src/main/res"),
                File("../app/src/main/res")
            )
            resDir = candidatePaths.firstOrNull { it.exists() && it.isDirectory }
                ?: throw IllegalStateException("Could not find res directory in candidate paths: $candidatePaths")

            val docBuilder = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = false
                isValidating = false
            }.newDocumentBuilder()

            for (localeName in EXPECTED_LOCALES) {
                val stringsFile = File(resDir, "$localeName/strings.xml")
                assertTrue("strings.xml must exist for locale folder '$localeName'", stringsFile.exists())

                val doc = docBuilder.parse(stringsFile)
                doc.documentElement.normalize()

                val stringNodes = doc.getElementsByTagName("string")
                val map = mutableMapOf<String, String>()
                for (i in 0 until stringNodes.length) {
                    val element = stringNodes.item(i) as Element
                    val keyName = element.getAttribute("name")
                    val valueText = element.textContent ?: ""
                    map[keyName] = valueText
                }
                stringMapByLocale[localeName] = map
            }
        }
    }

    @Test
    fun testAllExpectedLocaleFoldersExist() {
        for (locale in EXPECTED_LOCALES) {
            val folder = File(resDir, locale)
            assertTrue("Expected locale folder '$locale' to exist in $resDir", folder.exists() && folder.isDirectory)
            val stringsFile = File(folder, "strings.xml")
            assertTrue("strings.xml must exist in '$locale'", stringsFile.exists())
        }
    }

    @Test
    fun testAllLocalizedFilesHaveAllBaseKeys() {
        val baseStrings = stringMapByLocale["values"]
            ?: throw IllegalStateException("Base strings in 'values' not found")
        val baseKeys = baseStrings.keys

        val failureMessages = mutableListOf<String>()

        for (locale in EXPECTED_LOCALES.filter { it != "values" }) {
            val localizedStrings = stringMapByLocale[locale] ?: emptyMap()
            val missingKeys = baseKeys.filter { !localizedStrings.containsKey(it) }

            if (missingKeys.isNotEmpty()) {
                failureMessages.add(
                    "Locale '$locale' is missing ${missingKeys.size} key(s) from base values:\n" +
                            missingKeys.joinToString("\n") { "  - $it" }
                )
            }
        }

        assertTrue(
            "Missing keys detected in localized strings:\n" + failureMessages.joinToString("\n\n"),
            failureMessages.isEmpty()
        )
    }

    @Test
    fun testNoOrphanedKeysInLocalizedFiles() {
        val baseStrings = stringMapByLocale["values"]
            ?: throw IllegalStateException("Base strings in 'values' not found")
        val baseKeys = baseStrings.keys

        val failureMessages = mutableListOf<String>()

        for (locale in EXPECTED_LOCALES.filter { it != "values" }) {
            val localizedStrings = stringMapByLocale[locale] ?: emptyMap()
            val extraKeys = localizedStrings.keys.filter { !baseKeys.contains(it) }

            if (extraKeys.isNotEmpty()) {
                failureMessages.add(
                    "Locale '$locale' has ${extraKeys.size} extraneous/orphaned key(s) not present in base:\n" +
                            extraKeys.joinToString("\n") { "  - $it" }
                )
            }
        }

        assertTrue(
            "Extraneous/orphaned keys detected in localized strings:\n" + failureMessages.joinToString("\n\n"),
            failureMessages.isEmpty()
        )
    }

    @Test
    fun testStringCountsMatchAcrossAllLocales() {
        val baseCount = stringMapByLocale["values"]?.size ?: 0
        assertTrue("Base string count must be greater than 0", baseCount > 0)

        for (locale in EXPECTED_LOCALES) {
            val count = stringMapByLocale[locale]?.size ?: 0
            assertEquals(
                "String count for locale '$locale' ($count) should match base 'values' count ($baseCount)",
                baseCount,
                count
            )
        }
    }

    @Test
    fun testNoEmptyOrBlankStringValues() {
        val failureMessages = mutableListOf<String>()

        for ((locale, map) in stringMapByLocale) {
            for ((key, value) in map) {
                if (value.trim().isEmpty()) {
                    failureMessages.add("Locale '$locale' has an empty/blank value for key: '$key'")
                }
            }
        }

        assertTrue(
            "Found empty string resources:\n" + failureMessages.joinToString("\n"),
            failureMessages.isEmpty()
        )
    }

    @Test
    fun testFormatSpecifierConsistencyAcrossLocales() {
        val baseStrings = stringMapByLocale["values"]
            ?: throw IllegalStateException("Base strings in 'values' not found")

        val failureMessages = mutableListOf<String>()

        fun extractFormatSpecifiers(str: String): List<String> {
            val matcher = FORMAT_SPECIFIER_REGEX.matcher(str)
            val list = mutableListOf<String>()
            while (matcher.find()) {
                list.add(matcher.group())
            }
            return list
        }

        for ((key, baseValue) in baseStrings) {
            val baseSpecifiers = extractFormatSpecifiers(baseValue)
            if (baseSpecifiers.isEmpty()) continue

            for (locale in EXPECTED_LOCALES.filter { it != "values" }) {
                val localizedValue = stringMapByLocale[locale]?.get(key) ?: continue
                val localizedSpecifiers = extractFormatSpecifiers(localizedValue)

                if (baseSpecifiers.sorted() != localizedSpecifiers.sorted()) {
                    failureMessages.add(
                        "Format specifier mismatch in locale '$locale' for key '$key':\n" +
                                "  Base value: '$baseValue' -> specifiers: $baseSpecifiers\n" +
                                "  Localized:  '$localizedValue' -> specifiers: $localizedSpecifiers"
                    )
                }
            }
        }

        assertTrue(
            "Format specifiers mismatch between base and localized resources:\n" +
                    failureMessages.joinToString("\n\n"),
            failureMessages.isEmpty()
        )
    }

    @Test
    fun testLanguageSelectionKeysConfigured() {
        val baseStrings = stringMapByLocale["values"] ?: emptyMap()
        val languageKeys = listOf("language_title", "language_desc", "language_system", "language_en", "language_es", "language_pt", "language_fr")

        for (key in languageKeys) {
            assertTrue("Base strings must define '$key'", baseStrings.containsKey(key))
            for (locale in EXPECTED_LOCALES) {
                val localizedValue = stringMapByLocale[locale]?.get(key)
                assertNotNull("Locale '$locale' must define '$key'", localizedValue)
                assertFalse("Locale '$locale' key '$key' must not be blank", localizedValue.isNullOrBlank())
            }
        }
    }
}
