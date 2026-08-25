package com.app.spent.ui.dashboard.components.dialogs

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.app.spent.R
import androidx.compose.ui.platform.LocalContext
import com.app.spent.data.local.entity.CategoryEntity
import com.app.spent.data.local.entity.TransactionEntity
import com.app.spent.ui.theme.ExpenseRed
import com.app.spent.ui.theme.IncomeGreen
import com.app.spent.util.ImageStorageHelper

@Composable
fun TransactionDetailsDialog(
  transaction: TransactionEntity,
  categories: List<CategoryEntity>,
  currencySymbol: String,
  onDismiss: () -> Unit,
  onRequestDelete: (TransactionEntity) -> Unit,
  onEdit: (TransactionEntity) -> Unit = {}
) {
  val context = LocalContext.current
  val category = categories.find { it.id == transaction.categoryId }
  val isExpense = transaction.type == "EXPENSE"
  val formattedDate = SimpleDateFormat("EEEE, MMMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(transaction.timestamp))
  var showFullImage by remember { mutableStateOf(false) }
  val resolvedImageModel = remember(transaction.imageUri) {
    ImageStorageHelper.resolveImageUri(context, transaction.imageUri)
  }
  val imageModelToLoad = remember(resolvedImageModel, transaction.imageUri) {
    ImageRequest.Builder(context)
      .data(resolvedImageModel ?: transaction.imageUri)
      .crossfade(true)
      .build()
  }

  AlertDialog(
  onDismissRequest = onDismiss,
  title = {
    Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
      modifier = Modifier
      .size(32.dp)
      .clip(CircleShape)
      .background(if (isExpense) ExpenseRed.copy(alpha = 0.15f) else IncomeGreen.copy(alpha = 0.15f)),
      contentAlignment = Alignment.Center
      ) {
        Text(
        text = if (isExpense) "-" else "+",
        fontWeight = FontWeight.Bold,
        color = if (isExpense) ExpenseRed else IncomeGreen
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Text(
      text = if (isExpense) stringResource(R.string.expense_details) else stringResource(R.string.income_details),
      fontWeight = FontWeight.Bold
      )
    }
  },
  text = {
    Column {
      Text(
      text = "${if (isExpense) "-" else "+"}$currencySymbol${"%.2f".format(transaction.amount)}",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
      color = if (isExpense) ExpenseRed else IncomeGreen
      )
      Spacer(modifier = Modifier.height(12.dp))

      Text(
      text = stringResource(R.string.category_label),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      val localizedCategoryName = category?.let { com.app.spent.util.CategoryLocalizationHelper.getLocalizedCategoryName(it) }
        ?: stringResource(R.string.category_general)
      Text(
      text = localizedCategoryName,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.SemiBold
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
      text = stringResource(R.string.note_merchant_label),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
      text = transaction.note.ifEmpty { stringResource(R.string.no_note_provided) },
      style = MaterialTheme.typography.bodyMedium
      )

      if (!transaction.imageUri.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
        text = stringResource(R.string.attach_image_label),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
        modifier = Modifier
        .fillMaxWidth()
        .height(120.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        .clickable { showFullImage = true }
        ) {
          AsyncImage(
          model = imageModelToLoad,
          contentDescription = stringResource(R.string.receipt_image_preview),
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
      text = stringResource(R.string.date_time_label),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
      text = formattedDate,
      style = MaterialTheme.typography.bodySmall
      )
    }
  },
  confirmButton = {
    Row(
      horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedButton(
        onClick = {
          onDismiss()
          onRequestDelete(transaction)
        },
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed)
      ) {
        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(stringResource(R.string.delete_record))
      }

      Button(
        onClick = {
          onDismiss()
          onEdit(transaction)
        }
      ) {
        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(stringResource(R.string.edit_transaction_btn))
      }
    }
  },
  dismissButton = {
    TextButton(onClick = onDismiss) {
      Text(stringResource(R.string.btn_close))
    }
  }
  )

  if (showFullImage && !transaction.imageUri.isNullOrBlank()) {
    Dialog(
    onDismissRequest = { showFullImage = false },
    properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
      Box(
      modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.92f))
      .padding(16.dp)
      ) {
        IconButton(
        onClick = { showFullImage = false },
        modifier = Modifier
        .align(Alignment.TopEnd)
        .size(44.dp)
        .clip(CircleShape)
        .background(Color.Black.copy(alpha = 0.5f))
        ) {
          Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "Close",
          tint = Color.White
          )
        }

        AsyncImage(
        model = imageModelToLoad,
        contentDescription = stringResource(R.string.receipt_viewer_title),
        contentScale = ContentScale.Fit,
        modifier = Modifier
        .fillMaxSize()
        .padding(vertical = 48.dp)
        )
      }
    }
  }
}
