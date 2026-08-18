package com.app.spent.ui.onboarding

import com.app.spent.ui.mvi.UiEffect
import com.app.spent.ui.mvi.UiIntent
import com.app.spent.ui.mvi.UiState
enum class OnboardingStep {
  WELCOME,
  PROFILE_SELECTION,
  PAY_SCHEDULE
}

data class OnboardingUiState(
val currentStep: OnboardingStep = OnboardingStep.WELCOME,
val selectedProfileRole: String = "EMPLOYED", // UNEMPLOYED, FREELANCER, EMPLOYED
val selectedFrequency: String = "MONTHLY", // WEEKLY, BIWEEKLY, SEMIMONTHLY, MONTHLY
val selectedStartDate: Long = System.currentTimeMillis(),
val salaryText: String = "",
val currencySymbol: String = "$",
val isLoading: Boolean = false,
val isRestoring: Boolean = false,
val isDriveConnected: Boolean = false,
val driveAccountEmail: String? = null
) : UiState

sealed class OnboardingUiIntent : UiIntent {
  // Navigation
  object NavigateBack : OnboardingUiIntent()
  object ProceedFromWelcome : OnboardingUiIntent()
  data class SelectProfileRole(val role: String) : OnboardingUiIntent()
  data class SelectFrequency(val frequency: String) : OnboardingUiIntent()
  data class SelectStartDate(val timestamp: Long) : OnboardingUiIntent()
  data class UpdateSalaryText(val salary: String) : OnboardingUiIntent()
  object CompleteSetup : OnboardingUiIntent()

  // Drive single unified intent
  object RequestDriveConnect : OnboardingUiIntent()
  data class OnDriveAccountConnected(val account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) : OnboardingUiIntent()
}

sealed class OnboardingUiEffect : UiEffect {
  object NavigateToDashboard : OnboardingUiEffect()
  data class ShowSnackbar(val message: String) : OnboardingUiEffect()
  object LaunchDriveSignIn : OnboardingUiEffect()
}
