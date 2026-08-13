package com.example.spent.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class TransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val ownerProfileId: String = "primary_account",
    val amount: Double,
    val type: String = "EXPENSE", // EXPENSE, INCOME
    val categoryId: String,
    val payCycleId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val merchantName: String? = null,
    val imageUri: String? = null,
    val recurringRuleId: String? = null
)
