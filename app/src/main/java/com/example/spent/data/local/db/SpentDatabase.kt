package com.example.spent.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.spent.data.local.dao.SpentDao
import com.example.spent.data.local.entity.CategoryEntity
import com.example.spent.data.local.entity.FamilyMemberEntity
import com.example.spent.data.local.entity.ParentalControlConfigEntity
import com.example.spent.data.local.entity.PayCycleEntity
import com.example.spent.data.local.entity.RecurringRuleEntity
import com.example.spent.data.local.entity.TransactionEntity
import com.example.spent.data.local.entity.UserAccountEntity

@Database(
    entities = [
        UserAccountEntity::class,
        FamilyMemberEntity::class,
        ParentalControlConfigEntity::class,
        CategoryEntity::class,
        PayCycleEntity::class,
        TransactionEntity::class,
        RecurringRuleEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SpentDatabase : RoomDatabase() {

    abstract fun spentDao(): SpentDao

    companion object {
        @Volatile
        private var INSTANCE: SpentDatabase? = null

        fun getInstance(context: Context): SpentDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SpentDatabase::class.java,
                    "spent_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
