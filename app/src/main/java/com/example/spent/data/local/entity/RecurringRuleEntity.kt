package com.example.spent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "recurring_rules")
data class RecurringRuleEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val ownerProfileId: String = "primary_account",
    val amount: Double,
    val categoryId: String,
    val frequency: String = "MONTHLY", // DAILY, WEEKLY, MONTHLY
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val lastExecuted: Long = 0L,
    val note: String = ""
)
