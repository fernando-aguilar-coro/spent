package com.app.spent.ui.onboarding

import androidx.lifecycle.viewModelScope
import com.app.spent.data.local.entity.PayCycleEntity
import com.app.spent.data.repository.SpentRepository
import com.app.spent.data.sync.DriveConnectResult
import com.app.spent.ui.mvi.BaseViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
class OnboardingViewModel(
private val repository: SpentRepository
) : BaseViewModel<OnboardingUiState, OnboardingUiIntent, OnboardingUiEffect>(OnboardingUiState()) {

  init {
    observeInitialData()
  }

  private fun observeInitialData() {
    viewModelScope.launch {
      val currency = repository.currencySymbolFlow.firstOrNull() ?: "$"
      setState { copy(currencySymbol = currency) }

      launch {
        repository.imageStorageLocationFlow.collect { location ->
          setState { copy(imageStorageLocation = location) }
        }
      }

      combine(
      repository.isDriveConnectedFlow,
      repository.driveAccountEmailFlow
      ) { isConnected, email ->
        Pair(isConnected, email)
      }.collect { (isConnected, email) ->
        setState {
          copy(
          isDriveConnected = isConnected,
          driveAccountEmail = email
          )
        }
      }
    }
  }

  override fun onIntent(intent: OnboardingUiIntent) {
    when (intent) {
      is OnboardingUiIntent.NavigateBack -> {
        when (currentState.currentStep) {
          OnboardingStep.PAY_SCHEDULE -> {
            setState { copy(currentStep = OnboardingStep.PROFILE_SELECTION) }
          }
          OnboardingStep.PROFILE_SELECTION -> {
            setState { copy(currentStep = OnboardingStep.WELCOME) }
          }
          OnboardingStep.WELCOME -> {
            // At root welcome step, back is handled by system
          }
        }
      }
      is OnboardingUiIntent.ProceedFromWelcome -> {
        setState { copy(currentStep = OnboardingStep.PROFILE_SELECTION) }
      }
      is OnboardingUiIntent.SelectImageStorageLocation -> {
        handleSelectImageStorageLocation(intent.location)
      }
      is OnboardingUiIntent.SelectProfileRole -> {
        handleProfileSelection(intent.role)
      }
      is OnboardingUiIntent.SelectFrequency -> {
        setState { copy(selectedFrequency = intent.frequency) }
      }
      is OnboardingUiIntent.SelectStartDate -> {
        setState { copy(selectedStartDate = intent.timestamp) }
      }
      is OnboardingUiIntent.UpdateSalaryText -> {
        if (intent.salary.isEmpty() || intent.salary.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
          setState { copy(salaryText = intent.salary) }
        }
      }
      is OnboardingUiIntent.CompleteSetup -> {
        finishEmployedSetup()
      }
      is OnboardingUiIntent.RequestDriveConnect -> {
        sendEffect(OnboardingUiEffect.LaunchDriveSignIn)
      }
      is OnboardingUiIntent.OnDriveAccountConnected -> {
        handleDriveAccountConnected(intent.account)
      }
    }
  }

  private fun handleSelectImageStorageLocation(location: String) {
    viewModelScope.launch {
      repository.setImageStorageLocation(location)
    }
  }

  private fun handleDriveAccountConnected(account: GoogleSignInAccount) {
    viewModelScope.launch {
      setState { copy(isRestoring = true) }
      val result = repository.connectGoogleDrive(account)
      setState { copy(isRestoring = false) }

      when (result) {
        is DriveConnectResult.RestoredFromCloud -> {
          repository.setWalkthroughCompleted(true)
          sendEffect(OnboardingUiEffect.NavigateToDashboard)
        }
        is DriveConnectResult.ConnectedNew -> {
          setState { copy(currentStep = OnboardingStep.PROFILE_SELECTION) }
        }
        is DriveConnectResult.Error -> {
          sendEffect(OnboardingUiEffect.ShowSnackbar(result.message))
        }
      }
    }
  }

  private fun handleProfileSelection(role: String) {
    viewModelScope.launch {
      setState { copy(isLoading = true) }
      // Ensure categories exist before proceeding
      repository.seedStarterDataIfEmpty()

      if (role == "EMPLOYED") {
        setState {
          copy(
          selectedProfileRole = role,
          currentStep = OnboardingStep.PAY_SCHEDULE,
          isLoading = false
          )
        }
      } else {
        val payCycle = PayCycleEntity(
        id = "default_cycle",
        frequency = "NONE",
        startDate = System.currentTimeMillis(),
        income = 0.0
        )
        repository.setPayCycle(payCycle)
        repository.setWalkthroughCompleted(true)
        setState { copy(isLoading = false) }
        sendEffect(OnboardingUiEffect.NavigateToDashboard)
      }
    }
  }

  private fun finishEmployedSetup() {
    viewModelScope.launch {
      setState { copy(isLoading = true) }
      // Double check seeding
      repository.seedStarterDataIfEmpty()

      val state = currentState
      val parsedIncome = state.salaryText.toDoubleOrNull() ?: 0.0
      val payCycle = PayCycleEntity(
      id = "default_cycle",
      frequency = state.selectedFrequency,
      startDate = state.selectedStartDate,
      income = parsedIncome
      )
      repository.setPayCycle(payCycle)
      repository.setWalkthroughCompleted(true)
      setState { copy(isLoading = false) }
      sendEffect(OnboardingUiEffect.NavigateToDashboard)
    }
  }
}
