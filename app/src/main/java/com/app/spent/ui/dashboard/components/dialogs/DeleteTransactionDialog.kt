package com.app.spent.ui.dashboard.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.spent.R
import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.ui.theme.ExpenseRed
@Composable
fun DeleteTransactionDialog(
transaction: TransactionEntity,
categories: List<CategoryEntity>,
currencySymbol: String,
onDismiss: () -> Unit,
onConfirmDelete: (TransactionEntity) -> Unit
) {
  val catName = categories.find { it.id == transaction.categoryId }?.let {
    com.app.spent.util.CategoryLocalizationHelper.getLocalizedCategoryName(it)
  } ?: stringResource(R.string.category_general)
  val isExpense = transaction.type == "EXPENSE"
  val isIncome = transaction.type == "INCOME"
  val signPrefix = when {
    isExpense -> "-"
    isIncome -> "+"
    else -> ""
  }

  AlertDialog(
  onDismissRequest = onDismiss,
  icon = {
    Box(
    modifier = Modifier
    .size(48.dp)
    .clip(CircleShape)
    .background(ExpenseRed.copy(alpha = 0.15f)),
    contentAlignment = Alignment.Center
    ) {
      Icon(
      imageVector = Icons.Default.Delete,
      contentDescription = "Delete",
      tint = ExpenseRed,
      modifier = Modifier.size(26.dp)
      )
    }
  },
  title = { Text(stringResource(R.string.delete_transaction_title), fontWeight = FontWeight.Bold) },
  text = {
    Column {
      Text(stringResource(R.string.delete_transaction_confirmation))
      Spacer(modifier = Modifier.height(10.dp))
      Box(
      modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant)
      .padding(12.dp)
      ) {
        Column {
          Text(
          text = "$signPrefix$currencySymbol${"%.2f".format(transaction.amount)} • $catName",
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          if (transaction.note.isNotEmpty()) {
            Text(
            text = transaction.note,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
      text = stringResource(R.string.delete_transaction_warning),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  },
  confirmButton = {
    Button(
    onClick = {
      onConfirmDelete(transaction)
      onDismiss()
    },
    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
    ) {
      Text(stringResource(R.string.delete_record), color = Color.White, fontWeight = FontWeight.Bold)
    }
  },
  dismissButton = {
    TextButton(onClick = onDismiss) {
      Text(stringResource(R.string.btn_cancel))
    }
  }
  )
}
