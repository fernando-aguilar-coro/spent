package com.app.spent.ui.onboarding

import java.util.UUID
import androidx.lifecycle.viewModelScope
import com.app.spent.data.local.entity.PayCycleEntity
import com.app.spent.data.local.entity.TransactionEntity
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
            launch {
                repository.currencySymbolFlow.collect { currency ->
                    setState { copy(currencySymbol = currency) }
                }
            }

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
                    OnboardingStep.IMAGE_STORAGE -> {
                        setState { copy(currentStep = OnboardingStep.INITIAL_BALANCE) }
                    }
                    OnboardingStep.INITIAL_BALANCE -> {
                        if (currentState.selectedProfileRole == "EMPLOYED") {
                            setState { copy(currentStep = OnboardingStep.PAY_SCHEDULE) }
                        } else {
                            setState { copy(currentStep = OnboardingStep.PROFILE_SELECTION) }
                        }
                    }
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
            is OnboardingUiIntent.ProceedFromPaySchedule -> {
                setState { copy(currentStep = OnboardingStep.INITIAL_BALANCE) }
            }
            is OnboardingUiIntent.UpdateInitialBalanceText -> {
                if (intent.initialBalance.isEmpty() || intent.initialBalance.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                    setState { copy(initialBalanceText = intent.initialBalance) }
                }
            }
            is OnboardingUiIntent.ProceedFromInitialBalance -> {
                setState { copy(currentStep = OnboardingStep.IMAGE_STORAGE) }
            }
            is OnboardingUiIntent.SelectImageStorageLocation -> {
                handleSelectImageStorageLocation(intent.location)
            }
            is OnboardingUiIntent.SelectCurrency -> {
                handleSelectCurrency(intent.symbol)
            }
            is OnboardingUiIntent.CompleteSetup -> {
                finishOnboardingSetup()
            }
            is OnboardingUiIntent.RequestDriveConnect -> {
                sendEffect(OnboardingUiEffect.LaunchDriveSignIn)
            }
            is OnboardingUiIntent.OnDriveAccountConnected -> {
                handleDriveAccountConnected(intent.account)
            }
        }
    }

    private fun handleSelectCurrency(symbol: String) {
        viewModelScope.launch {
            setState { copy(currencySymbol = symbol) }
            repository.setCurrencySymbol(symbol)
        }
    }

    private fun handleSelectImageStorageLocation(location: String) {
        viewModelScope.launch {
            setState { copy(imageStorageLocation = location) }
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
        setState { copy(selectedProfileRole = role) }
        if (role == "EMPLOYED") {
            setState { copy(currentStep = OnboardingStep.PAY_SCHEDULE) }
        } else {
            setState { copy(currentStep = OnboardingStep.INITIAL_BALANCE) }
        }
    }

    private fun finishOnboardingSetup() {
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            // Seed starter data if empty
            repository.seedStarterDataIfEmpty()

            val state = currentState

            // 1. Pay cycle setup
            if (state.selectedProfileRole == "EMPLOYED") {
                val parsedIncome = state.salaryText.toDoubleOrNull() ?: 0.0
                val payCycle = PayCycleEntity(
                    id = "default_cycle",
                    frequency = state.selectedFrequency,
                    startDate = state.selectedStartDate,
                    income = parsedIncome
                )
                repository.setPayCycle(payCycle)
            } else {
                val payCycle = PayCycleEntity(
                    id = "default_cycle",
                    frequency = "NONE",
                    startDate = System.currentTimeMillis(),
                    income = 0.0
                )
                repository.setPayCycle(payCycle)
            }

            // 2. Initial Capital / Savings setup if provided
            val initialBalance = state.initialBalanceText.toDoubleOrNull() ?: 0.0
            if (initialBalance > 0) {
                val initialTx = TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    amount = initialBalance,
                    type = "INCOME",
                    categoryId = "cat_general",
                    note = "Initial Balance / Starting Capital",
                    timestamp = System.currentTimeMillis()
                )
                repository.addTransaction(initialTx)
            }

            // 3. Image storage location
            repository.setImageStorageLocation(state.imageStorageLocation)

            // 4. Mark walkthrough completed
            repository.setWalkthroughCompleted(true)
            setState { copy(isLoading = false) }
            sendEffect(OnboardingUiEffect.NavigateToDashboard)
        }
    }
}
