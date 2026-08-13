package com.example.spent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    val photoUrl: String? = null,
    val role: String = "CHILD",
    val pairingCode: String? = null,
    val isLinked: Boolean = false,
    val maxSingleExpenseLimit: Double? = null,
    val lastSyncTimestamp: Long = 0L
)
