package com.example.recorderapp.viewmodels

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recorderapp.repositories.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConfirmAccountUiState(
    val isLoading: Boolean = false,
    val isVerified: Boolean = false,
    // This String container will securely hold the exact error text pulled from your Python script!
    val errorMessage: String? = null
)

class ConfirmAccountViewModel(
    private val authRepository: AuthRepository
): ViewModel() {
    val otpState = TextFieldState("")
    private val _uiState = MutableStateFlow(ConfirmAccountUiState())
    val uiState = _uiState.asStateFlow()

    fun confirmOTPButton(userEmail: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val code = otpState.text.toString()
            val serverErrorResult = authRepository.verifyAccountOtp(userEmail,code)
            if (serverErrorResult == null) {
                _uiState.update { it.copy(isVerified = true, isLoading = false) }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = serverErrorResult) }
            }
        }
    }
}