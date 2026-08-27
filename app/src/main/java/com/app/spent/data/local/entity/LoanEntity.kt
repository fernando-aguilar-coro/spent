package com.app.spent.data.local.entity

import java.util.UUID
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val ownerProfileId: String = "primary_account",
    val type: String, // "I_OWE" (Money I owe / Debo a) vs "OWED_TO_ME" (Money owed to me / Me deben)
    val counterpartyName: String = "", // Creditor or Debtor name (Optional)
    val principalAmount: Double, // Initial principal amount
    val paidAmount: Double = 0.0, // Total amount paid or collected so far
    val categoryId: String = "cat_general",
    val createdAt: Long = System.currentTimeMillis(),
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val calculationMode: String = "TOTAL_PRINCIPAL", // "TOTAL_PRINCIPAL" vs "MONTHLY_QUOTA"
    val dueDate: Long? = null,
    val isInstallment: Boolean = false,
    val installmentAmount: Double? = null,
    val installmentDurationMonths: Int? = null,
    val interestRate: Double = 0.0,
    val note: String = "",
    val isSettled: Boolean = false
) {
    val remainingAmount: Double
        get() = (principalAmount - paidAmount).coerceAtLeast(0.0)

    val progress: Float
        get() = if (principalAmount > 0) (paidAmount / principalAmount).toFloat().coerceIn(0f, 1f) else 0f
}
