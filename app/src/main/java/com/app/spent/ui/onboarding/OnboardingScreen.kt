package com.app.spent.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.app.spent.data.sync.GoogleDriveRestService
import com.app.spent.ui.onboarding.components.ImageStorageStep
import com.app.spent.ui.onboarding.components.InitialBalanceStep
import com.app.spent.ui.onboarding.components.PayScheduleStep
import com.app.spent.ui.onboarding.components.ProfileSelectionStep
import com.app.spent.ui.onboarding.components.WelcomeStep
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
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

    if (state.syncConflict != null) {
        com.app.spent.ui.components.SyncConflictDialog(
            conflictData = state.syncConflict!!,
            onResolve = { choice ->
                viewModel.onIntent(OnboardingUiIntent.ResolveSyncConflict(choice))
            },
            onDismiss = {
                viewModel.onIntent(OnboardingUiIntent.DismissSyncConflict)
            }
        )
    }

    // Handle system back button / gesture to step back within onboarding
    BackHandler(enabled = state.currentStep != OnboardingStep.WELCOME) {
        viewModel.onIntent(OnboardingUiIntent.NavigateBack)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                viewModel.onIntent(OnboardingUiIntent.OnDriveAccountConnected(account))
            }
        } catch (e: ApiException) {
            android.util.Log.e("GoogleSignIn", "Google Sign In failed. Status Code: ${e.statusCode}", e)
            val errorMsg = when (e.statusCode) {
                else -> "Sign-in error (${e.statusCode}): ${e.localizedMessage ?: "Unknown"}"
            }
            if (e.statusCode != 12501) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(errorMsg)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("GoogleSignIn", "Unexpected sign in error", e)
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Error: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is OnboardingUiEffect.NavigateToDashboard -> onNavigateToDashboard()
                is OnboardingUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is OnboardingUiEffect.LaunchDriveSignIn -> {
                    val signInClient = GoogleDriveRestService.getGoogleSignInClient(context)
                    googleSignInLauncher.launch(signInClient.signInIntent)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            OnboardingTopBar(
                currentStep = state.currentStep,
                selectedProfileRole = state.selectedProfileRole,
                onBackClick = { viewModel.onIntent(OnboardingUiIntent.NavigateBack) }
            )
        },
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
                transitionSpec = {
                    val isForward = targetState.ordinal > initialState.ordinal
                    if (isForward) {
                        (slideInHorizontally(
                            initialOffsetX = { fullWidth -> (fullWidth * 0.35f).toInt() },
                            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 300)
                        )).togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -(fullWidth * 0.35f).toInt() },
                                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                            ) + fadeOut(
                                animationSpec = tween(durationMillis = 200)
                            )
                        )
                    } else {
                        (slideInHorizontally(
                            initialOffsetX = { fullWidth -> -(fullWidth * 0.35f).toInt() },
                            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                        ) + fadeIn(
                            animationSpec = tween(durationMillis = 300)
                        )).togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> (fullWidth * 0.35f).toInt() },
                                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                            ) + fadeOut(
                                animationSpec = tween(durationMillis = 200)
                            )
                        )
                    }
                },
                label = "onboarding_step_transition"
            ) { step ->
                when (step) {
                    OnboardingStep.WELCOME -> WelcomeStep(
                        currencySymbol = state.currencySymbol,
                        isRestoring = state.isRestoring,
                        isConnected = state.isDriveConnected,
                        accountEmail = state.driveAccountEmail,
                        onSelectCurrency = {
                            viewModel.onIntent(OnboardingUiIntent.SelectCurrency(it))
                        },
                        onConnectDrive = {
                            viewModel.onIntent(OnboardingUiIntent.RequestDriveConnect)
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
                        onContinue = { viewModel.onIntent(OnboardingUiIntent.ProceedFromPaySchedule) }
                    )
                    OnboardingStep.INITIAL_BALANCE -> InitialBalanceStep(
                        state = state,
                        onUpdateInitialBalance = { viewModel.onIntent(OnboardingUiIntent.UpdateInitialBalanceText(it)) },
                        onContinue = { viewModel.onIntent(OnboardingUiIntent.ProceedFromInitialBalance) }
                    )
                    OnboardingStep.IMAGE_STORAGE -> ImageStorageStep(
                        currentLocation = state.imageStorageLocation,
                        isLoading = state.isLoading,
                        onSelectLocation = { location ->
                            viewModel.onIntent(OnboardingUiIntent.SelectImageStorageLocation(location))
                        },
                        onFinish = { viewModel.onIntent(OnboardingUiIntent.CompleteSetup) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingTopBar(
    currentStep: OnboardingStep,
    selectedProfileRole: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = if (selectedProfileRole == "EMPLOYED") {
        listOf(
            OnboardingStep.WELCOME,
            OnboardingStep.PROFILE_SELECTION,
            OnboardingStep.PAY_SCHEDULE,
            OnboardingStep.INITIAL_BALANCE,
            OnboardingStep.IMAGE_STORAGE
        )
    } else {
        listOf(
            OnboardingStep.WELCOME,
            OnboardingStep.PROFILE_SELECTION,
            OnboardingStep.INITIAL_BALANCE,
            OnboardingStep.IMAGE_STORAGE
        )
    }

    val currentStepIndex = steps.indexOf(currentStep).coerceAtLeast(0)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (currentStep != OnboardingStep.WELCOME) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // Step Progress Indicator Dots/Pills
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, _ ->
                val isActive = index == currentStepIndex
                val isPassed = index < currentStepIndex

                val width by animateDpAsState(
                    targetValue = if (isActive) 28.dp else 8.dp,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label = "step_pill_width"
                )

                val color by animateColorAsState(
                    targetValue = when {
                        isActive -> MaterialTheme.colorScheme.primary
                        isPassed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    animationSpec = tween(durationMillis = 300),
                    label = "step_pill_color"
                )

                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }

        // Right spacer to keep step indicator centered
        Spacer(modifier = Modifier.size(48.dp))
    }
}
