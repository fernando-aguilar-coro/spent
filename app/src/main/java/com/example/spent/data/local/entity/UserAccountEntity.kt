package com.example.spent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_account")
data class UserAccountEntity(
    @PrimaryKey val id: String = "primary_account",
    val googleId: String? = null,
    val displayName: String? = "Primary User",
    val email: String? = null,
    val photoUrl: String? = null,
    val role: String = "INDEPENDENT",
    val activeProfileId: String = "primary_account",
    val isSignedIn: Boolean = false,
    val lastDriveSyncTimestamp: Long = 0L
)
