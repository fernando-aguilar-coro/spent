package com.app.spent.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.app.spent.data.local.dao.SpentDao
import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.FamilyMemberEntity
import com.app.spent.data.local.entity.LoanEntity
import com.app.spent.data.local.entity.ParentalControlConfigEntity
import com.app.spent.data.local.entity.PayCycleEntity
import com.app.spent.data.local.entity.RecurringRuleEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.data.local.entity.UserAccountEntity

@Database(
    entities = [
        UserAccountEntity::class,
        FamilyMemberEntity::class,
        ParentalControlConfigEntity::class,
        CategoryEntity::class,
        PayCycleEntity::class,
        TransactionEntity::class,
        RecurringRuleEntity::class,
        LoanEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class SpentDatabase : RoomDatabase() {

  abstract fun spentDao(): SpentDao

  companion object {
    @Volatile
    private var INSTANCE: SpentDatabase? = null

    val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `loans` (
              `id` TEXT NOT NULL,
              `ownerProfileId` TEXT NOT NULL,
              `type` TEXT NOT NULL,
              `counterpartyName` TEXT NOT NULL,
              `principalAmount` REAL NOT NULL,
              `paidAmount` REAL NOT NULL,
              `categoryId` TEXT NOT NULL,
              `createdAt` INTEGER NOT NULL,
              `dueDate` INTEGER,
              `isInstallment` INTEGER NOT NULL,
              `installmentAmount` REAL,
              `installmentDurationMonths` INTEGER,
              `interestRate` REAL NOT NULL,
              `note` TEXT NOT NULL,
              `isSettled` INTEGER NOT NULL,
              PRIMARY KEY(`id`)
          )
          """.trimIndent()
        )
      }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
      override fun migrate(db: SupportSQLiteDatabase) {
        try {
          db.execSQL("ALTER TABLE `loans` ADD COLUMN `startDate` INTEGER NOT NULL DEFAULT 0")
        } catch (e: Exception) {
          // Column may already exist
        }
        try {
          db.execSQL("ALTER TABLE `loans` ADD COLUMN `endDate` INTEGER DEFAULT NULL")
        } catch (e: Exception) {
          // Column may already exist
        }
        try {
          db.execSQL("ALTER TABLE `loans` ADD COLUMN `calculationMode` TEXT NOT NULL DEFAULT 'TOTAL_PRINCIPAL'")
        } catch (e: Exception) {
          // Column may already exist
        }
      }
    }

    val MIGRATION_1_3 = object : Migration(1, 3) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `loans` (
              `id` TEXT NOT NULL,
              `ownerProfileId` TEXT NOT NULL,
              `type` TEXT NOT NULL,
              `counterpartyName` TEXT NOT NULL,
              `principalAmount` REAL NOT NULL,
              `paidAmount` REAL NOT NULL,
              `categoryId` TEXT NOT NULL,
              `createdAt` INTEGER NOT NULL,
              `startDate` INTEGER NOT NULL DEFAULT 0,
              `endDate` INTEGER,
              `calculationMode` TEXT NOT NULL DEFAULT 'TOTAL_PRINCIPAL',
              `dueDate` INTEGER,
              `isInstallment` INTEGER NOT NULL DEFAULT 0,
              `installmentAmount` REAL,
              `installmentDurationMonths` INTEGER,
              `interestRate` REAL NOT NULL DEFAULT 0.0,
              `note` TEXT NOT NULL DEFAULT '',
              `isSettled` INTEGER NOT NULL DEFAULT 0,
              PRIMARY KEY(`id`)
          )
          """.trimIndent()
        )
      }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
      override fun migrate(db: SupportSQLiteDatabase) {
        try {
          db.execSQL("ALTER TABLE `recurring_rules` ADD COLUMN `type` TEXT NOT NULL DEFAULT 'EXPENSE'")
        } catch (e: Exception) {
          // Column may already exist
        }
      }
    }

    fun getInstance(context: Context): SpentDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          SpentDatabase::class.java,
          "spent_database"
        )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3, MIGRATION_3_4)
        .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
