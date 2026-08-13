package com.example.spent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val ownerProfileId: String = "primary_account",
    val name: String,
    val iconName: String,
    val colorHex: String,
    val budgetAmount: Double,
    val displayOrder: Int = 0,
    val isParentalLocked: Boolean = false
)
