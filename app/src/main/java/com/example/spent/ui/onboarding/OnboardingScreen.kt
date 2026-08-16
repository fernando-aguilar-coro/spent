package com.example.spent.ui.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.spent.data.sync.DriveBackupManager
import com.example.spent.ui.onboarding.components.PayScheduleStep
import com.example.spent.ui.onboarding.components.ProfileSelectionStep
import com.example.spent.ui.onboarding.components.WelcomeStep

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onNavigateToDashboard: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val json = DriveBackupManager.readBackupFromUri(context, it)
            if (json != null) {
                viewModel.onIntent(OnboardingUiIntent.RestoreFromDriveJson(json))
            } else {
                viewModel.onIntent(OnboardingUiIntent.RestoreFromDriveJson(""))
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is OnboardingUiEffect.NavigateToDashboard -> onNavigateToDashboard()
                is OnboardingUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is OnboardingUiEffect.LaunchDriveFilePicker -> {
                    filePickerLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "onboarding_step"
            ) { step ->
                when (step) {
                    OnboardingStep.WELCOME -> WelcomeStep(
                        isRestoring = state.isRestoring,
                        onConnectDrive = {
                            filePickerLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        },
                        onContinue = {
                            viewModel.onIntent(OnboardingUiIntent.ProceedFromWelcome)
                        }
                    )
                    OnboardingStep.PROFILE_SELECTION -> ProfileSelectionStep(
                        isLoading = state.isLoading,
                        onSelectRole = { role ->
                            viewModel.onIntent(OnboardingUiIntent.SelectProfileRole(role))
                        }
                    )
                    OnboardingStep.PAY_SCHEDULE -> PayScheduleStep(
                        state = state,
                        onSelectFrequency = { viewModel.onIntent(OnboardingUiIntent.SelectFrequency(it)) },
                        onSelectStartDate = { viewModel.onIntent(OnboardingUiIntent.SelectStartDate(it)) },
                        onUpdateSalary = { viewModel.onIntent(OnboardingUiIntent.UpdateSalaryText(it)) },
                        onFinish = { viewModel.onIntent(OnboardingUiIntent.CompleteSetup) }
                    )
                }
            }
        }
    }
}
