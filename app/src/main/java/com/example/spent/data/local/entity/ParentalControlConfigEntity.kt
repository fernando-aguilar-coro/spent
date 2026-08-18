package com.app.spent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "parental_control_config")
data class ParentalControlConfigEntity(
@PrimaryKey val id: String = "primary_config",
val isEnabled: Boolean = false,
val masterPinHash: String? = null,
val isBiometricEnabled: Boolean = false,
val protectSettings: Boolean = true,
val protectDataReset: Boolean = true
)
