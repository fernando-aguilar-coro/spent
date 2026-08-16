package com.example.spent.ui.onboarding

import androidx.lifecycle.viewModelScope
import com.example.spent.data.local.entity.PayCycleEntity
import com.example.spent.data.local.entity.UserAccountEntity
import com.example.spent.data.repository.SpentRepository
import com.example.spent.data.sync.DriveBackupManager
import com.example.spent.ui.mvi.BaseViewModel
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
        }
    }

    override fun onIntent(intent: OnboardingUiIntent) {
        when (intent) {
            is OnboardingUiIntent.ProceedFromWelcome -> {
                setState { copy(currentStep = OnboardingStep.PROFILE_SELECTION) }
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
            is OnboardingUiIntent.RestoreFromDriveJson -> {
                restoreFromDrive(intent.jsonString)
            }
        }
    }

    private fun handleProfileSelection(role: String) {
        if (role == "EMPLOYED") {
            setState {
                copy(
                    selectedProfileRole = role,
                    currentStep = OnboardingStep.PAY_SCHEDULE
                )
            }
        } else {
            // UNEMPLOYED or FREELANCER: Set flexible cycle and complete onboarding immediately
            viewModelScope.launch {
                setState { copy(isLoading = true) }
                val userAccount = UserAccountEntity(
                    id = "primary_account",
                    displayName = if (role == "FREELANCER") "Freelancer" else "User",
                    role = role
                )
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

    private fun restoreFromDrive(jsonString: String) {
        viewModelScope.launch {
            setState { copy(isRestoring = true) }
            val result = DriveBackupManager.restoreFromJson(jsonString, repository)
            setState { copy(isRestoring = false) }
            if (result.isSuccess) {
                sendEffect(OnboardingUiEffect.ShowSnackbar("Data restored from Google Drive successfully"))
                sendEffect(OnboardingUiEffect.NavigateToDashboard)
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Failed to restore backup"
                sendEffect(OnboardingUiEffect.ShowSnackbar("Restore failed: $errorMsg"))
            }
        }
    }
}
