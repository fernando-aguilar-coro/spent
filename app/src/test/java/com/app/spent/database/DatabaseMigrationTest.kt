package com.app.spent.database

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.app.spent.data.local.db.SpentDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class DatabaseMigrationTest {

    @Test
    fun testMigration1To2SqlExecution() {
        val executedSql = mutableListOf<String>()
        val proxyDb = createMockDatabase(executedSql)

        SpentDatabase.MIGRATION_1_2.migrate(proxyDb)

        assertTrue(
            "MIGRATION_1_2 must create loans table",
            executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `loans`") }
        )
    }

    @Test
    fun testMigration2To3SqlExecution() {
        val executedSql = mutableListOf<String>()
        val proxyDb = createMockDatabase(executedSql)

        SpentDatabase.MIGRATION_2_3.migrate(proxyDb)

        assertTrue(
            "MIGRATION_2_3 must add startDate column",
            executedSql.any { it.contains("ALTER TABLE `loans` ADD COLUMN `startDate`") }
        )
        assertTrue(
            "MIGRATION_2_3 must add endDate column",
            executedSql.any { it.contains("ALTER TABLE `loans` ADD COLUMN `endDate`") }
        )
        assertTrue(
            "MIGRATION_2_3 must add calculationMode column",
            executedSql.any { it.contains("ALTER TABLE `loans` ADD COLUMN `calculationMode`") }
        )
    }

    @Test
    fun testMigration1To3SqlExecution() {
        val executedSql = mutableListOf<String>()
        val proxyDb = createMockDatabase(executedSql)

        SpentDatabase.MIGRATION_1_3.migrate(proxyDb)

        assertTrue(
            "MIGRATION_1_3 must create full loans table",
            executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `loans`") && it.contains("calculationMode") }
        )
    }

    @Test
    fun testMigrationVersionsAreConfiguredCorrectly() {
        assertEquals(1, SpentDatabase.MIGRATION_1_2.startVersion)
        assertEquals(2, SpentDatabase.MIGRATION_1_2.endVersion)

        assertEquals(2, SpentDatabase.MIGRATION_2_3.startVersion)
        assertEquals(3, SpentDatabase.MIGRATION_2_3.endVersion)

        assertEquals(1, SpentDatabase.MIGRATION_1_3.startVersion)
        assertEquals(3, SpentDatabase.MIGRATION_1_3.endVersion)
    }

    private fun createMockDatabase(executedSql: MutableList<String>): SupportSQLiteDatabase {
        return Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
            object : InvocationHandler {
                override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
                    if (method?.name == "execSQL" && args != null && args.isNotEmpty()) {
                        executedSql.add(args[0].toString())
                    }
                    return null
                }
            }
        ) as SupportSQLiteDatabase
    }
}
