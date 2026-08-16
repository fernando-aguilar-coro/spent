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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.spent.data.sync.DriveBackupManager
import com.example.spent.ui.onboarding.components.PayScheduleStep
import com.example.spent.ui.onboarding.components.ProfileSelectionStep
import com.example.spent.ui.onboarding.components.WelcomeStep

import com.example.spent.data.sync.GoogleDriveRestService
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onNavigateToDashboard: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var isRestoreAction by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                if (isRestoreAction) {
                    coroutineScope.launch {
                        val downloadRes = GoogleDriveRestService.downloadBackupFromDrive(context, account)
                        if (downloadRes.isSuccess) {
                            val json = downloadRes.getOrNull().orEmpty()
                            viewModel.onIntent(OnboardingUiIntent.RestoreFromDriveJson(json))
                        } else {
                            viewModel.onIntent(OnboardingUiIntent.RestoreFromDriveJson(""))
                        }
                    }
                } else {
                    // Connect Only logic
                    viewModel.handleConnectResult(context, account)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            viewModel.onIntent(OnboardingUiIntent.RestoreFromDriveJson(""))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is OnboardingUiEffect.NavigateToDashboard -> onNavigateToDashboard()
                is OnboardingUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is OnboardingUiEffect.LaunchDriveSignInForRestore -> {
                    isRestoreAction = true
                    val signInClient = GoogleDriveRestService.getGoogleSignInClient(context)
                    googleSignInLauncher.launch(signInClient.signInIntent)
                }
                is OnboardingUiEffect.LaunchDriveSignInForConnect -> {
                    isRestoreAction = false
                    val signInClient = GoogleDriveRestService.getGoogleSignInClient(context)
                    googleSignInLauncher.launch(signInClient.signInIntent)
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
                            viewModel.onIntent(OnboardingUiIntent.ConnectDriveOnly)
                        },
                        onRestoreDrive = {
                            viewModel.onIntent(OnboardingUiIntent.RestoreFromDrive)
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
