package com.app.spent.ui.transaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.spent.R
import com.app.spent.ui.components.CustomNumericKeypad
import com.app.spent.ui.theme.ExpenseRed
import com.app.spent.ui.theme.IncomeGreen
import com.app.spent.ui.transaction.components.AddCategoryDialog
import com.app.spent.ui.transaction.components.CategoryEnvelopeSelector
import com.app.spent.ui.transaction.components.DateTimePickerField
import com.app.spent.ui.transaction.components.RecurringOptionsSection
import com.app.spent.ui.transaction.components.TransactionImageAttachmentSection
import com.app.spent.ui.transaction.components.TransactionTypeSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
  viewModel: AddTransactionViewModel,
  onNavigateBack: () -> Unit
) {
  val state by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }

  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
    viewModel.effect.collect { effect ->
      when (effect) {
        is AddTransactionUiEffect.NavigateBack -> onNavigateBack()
        is AddTransactionUiEffect.ShowSnackbar -> {
          snackbarHostState.showSnackbar(effect.message)
        }
      }
    }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        title = {
          Text(
            text = if (state.selectedType == "EXPENSE") stringResource(R.string.add_expense_title) else stringResource(R.string.add_income_title),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
          )
        },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back"
            )
          }
        }
      )
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 12.dp)
          .padding(bottom = if (state.showKeypad) 390.dp else 110.dp)
      ) {
        // Type Toggle (Expense / Income)
        TransactionTypeSelector(
          selectedType = state.selectedType,
          onTypeSelected = { viewModel.onIntent(AddTransactionUiIntent.SelectType(it)) }
        )

        if (state.selectedType == "INCOME") {
          Spacer(modifier = Modifier.height(10.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(IncomeGreen.copy(alpha = 0.12f))
              .padding(horizontal = 14.dp, vertical = 10.dp)
          ) {
            Text(
              text = stringResource(R.string.salary_funding_note),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.Medium
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Amount Input Field with Native Soft Keyboard & Calculator Icon
        OutlinedTextField(
          value = state.amountExpression,
          onValueChange = { input ->
            if (input.isEmpty() || input.matches(Regex("^[0-9+×÷\\-\\.\\,\\s]*$"))) {
              viewModel.onIntent(AddTransactionUiIntent.UpdateAmount(input))
            }
          },
          label = { Text(stringResource(R.string.amount_label, state.currencySymbol)) },
          placeholder = { Text("0.00") },
          prefix = {
            Text(
              text = "${state.currencySymbol} ",
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp,
              color = if (state.selectedType == "EXPENSE") ExpenseRed else IncomeGreen
            )
          },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          shape = RoundedCornerShape(20.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
          ),
          trailingIcon = {
            IconButton(
              onClick = {
                if (!state.showKeypad) {
                  keyboardController?.hide()
                  focusManager.clearFocus()
                }
                viewModel.onIntent(AddTransactionUiIntent.ToggleKeypad(!state.showKeypad))
              }
            ) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .background(if (state.showKeypad) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Calculate,
                  contentDescription = "Toggle Calculator",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Category Envelope Selector Row
        CategoryEnvelopeSelector(
          categories = state.categories,
          selectedCategoryId = state.selectedCategoryId,
          onCategorySelected = { viewModel.onIntent(AddTransactionUiIntent.SelectCategory(it)) },
          onAddNewCategoryClick = { viewModel.onIntent(AddTransactionUiIntent.ShowAddCategoryDialog(true)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Note / Merchant Input Field
        OutlinedTextField(
          value = state.noteText,
          onValueChange = { viewModel.onIntent(AddTransactionUiIntent.UpdateNote(it)) },
          label = { Text(stringResource(R.string.note_merchant_optional)) },
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
          ),
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Image Attachment Section
        TransactionImageAttachmentSection(
          selectedImageUri = state.selectedImageUri,
          storageLocation = state.imageStorageLocation,
          isProcessing = state.isProcessingImage,
          onImageSelected = { uri, ctx ->
            viewModel.onIntent(AddTransactionUiIntent.AttachImage(uri, ctx))
          },
          onRemoveImage = {
            viewModel.onIntent(AddTransactionUiIntent.RemoveImage)
          }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Date & Time Picker Section
        DateTimePickerField(
          timestamp = state.selectedTimestamp,
          onTimestampChanged = { viewModel.onIntent(AddTransactionUiIntent.UpdateTimestamp(it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Recurring Payment Section
        RecurringOptionsSection(
          isRecurring = state.isRecurring,
          onRecurringChange = { viewModel.onIntent(AddTransactionUiIntent.ToggleRecurring(it)) },
          selectedFrequency = state.selectedFrequency,
          onFrequencySelected = { viewModel.onIntent(AddTransactionUiIntent.SelectFrequency(it)) }
        )

        Spacer(modifier = Modifier.height(24.dp))
      }

      // Sticky Bottom Container with Save Button & Keypad
      Column(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .imePadding()
          .navigationBarsPadding()
      ) {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          color = MaterialTheme.colorScheme.background,
          tonalElevation = 8.dp,
          shadowElevation = 12.dp
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 12.dp)
          ) {
            Button(
              onClick = {
                viewModel.onIntent(AddTransactionUiIntent.SaveTransaction)
              },
              enabled = state.isValid && !state.isSaving,
              modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
              shape = RoundedCornerShape(18.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (state.selectedType == "EXPENSE") ExpenseRed else IncomeGreen,
                disabledContainerColor = (if (state.selectedType == "EXPENSE") ExpenseRed else IncomeGreen).copy(alpha = 0.4f)
              ),
              elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
              )
            ) {
              Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = stringResource(R.string.save_transaction),
                  fontWeight = FontWeight.Bold,
                  fontSize = 16.sp,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  color = Color.White
                )
              }
            }
          }
        }

        // Bottom Docked Custom Numeric Keypad (shown when calculator icon is tapped)
        AnimatedVisibility(
          visible = state.showKeypad,
          enter = slideInVertically { it } + fadeIn(),
          exit = slideOutVertically { it } + fadeOut()
        ) {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 16.dp
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
              // Keypad Header Bar with Drag Handle, Live Amount & Done Button
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .width(36.dp)
                      .height(4.dp)
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Text(
                    text = "${state.currencySymbol}${state.amountExpression.ifEmpty { "0.00" }}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (state.selectedType == "EXPENSE") ExpenseRed else IncomeGreen
                  )
                }

                IconButton(
                  onClick = { viewModel.onIntent(AddTransactionUiIntent.ToggleKeypad(false)) },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Done",
                    tint = MaterialTheme.colorScheme.primary
                  )
                }
              }

              CustomNumericKeypad(
                currentExpression = state.amountExpression,
                onExpressionChanged = { viewModel.onIntent(AddTransactionUiIntent.UpdateAmount(it)) },
                onConfirm = { viewModel.onIntent(AddTransactionUiIntent.ToggleKeypad(false)) }
              )
            }
          }
        }
      }
    }
  }

  // Add New Category Dialog
  if (state.showAddCategoryDialog) {
    AddCategoryDialog(
    onDismiss = { viewModel.onIntent(AddTransactionUiIntent.ShowAddCategoryDialog(false)) },
    onSaveCategory = { name, colorHex, iconName ->
      viewModel.onIntent(AddTransactionUiIntent.CreateCategory(name, colorHex, iconName))
    }
    )
  }
}
