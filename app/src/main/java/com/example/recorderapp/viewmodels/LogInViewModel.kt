package com.example.recorderapp.viewmodels

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recorderapp.models.LogInPayload
import com.example.recorderapp.models.LogInUIState
import com.example.recorderapp.repositories.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
class LogInViewModel(
    private val authRepository: AuthRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(LogInUIState())
    val uiState = _uiState.asStateFlow()

    val emailState = TextFieldState("")
    val passState = TextFieldState("")

    fun logInButton() {
        viewModelScope.launch {
            val payload = LogInPayload(emailState.text.toString(), passState.text.toString())
            val result = authRepository.accountLogin(payload)
            if (result == null) {
                _uiState.update { it.copy(isSuccess = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(errorMessage = result) }
            }
        }
    }

    fun resetSuccessState() {
        _uiState.update { currentInstance ->
            currentInstance.copy(
                isSuccess = false
            )
        }
    }
}