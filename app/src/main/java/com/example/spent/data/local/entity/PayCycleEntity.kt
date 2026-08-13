package com.example.spent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "pay_cycles")
data class PayCycleEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val ownerProfileId: String = "primary_account",
    val frequency: String = "MONTHLY", // WEEKLY, BIWEEKLY, SEMIMONTHLY, MONTHLY
    val startDate: Long = System.currentTimeMillis(),
    val income: Double = 0.0
)
