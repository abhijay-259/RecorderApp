package com.example.recorderapp.viewmodels

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recorderapp.models.LogInPayload
import com.example.recorderapp.models.UserRegistration
import com.example.recorderapp.repositories.AudioRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

import com.example.recorderapp.repositories.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.milliseconds

data class SignInUIState(
    val signInError: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)
class SignInViewModel(
    private val authRepository: AuthRepository
): ViewModel() {

    private val _uiState = MutableStateFlow(SignInUIState())
    val uiState = _uiState.asStateFlow()
    val nameState = TextFieldState("")
    val emailState = TextFieldState("")
    var passwordState = TextFieldState("")
    var confirmPasswordState = TextFieldState("")


    fun isPasswordMatching(pass: String, confirmPass: String): Boolean {
        return pass == confirmPass
    }

    fun createAccountButton() {
        val currentPassword = passwordState.text.toString()
        val currentConfirmPassword = confirmPasswordState.text.toString()
        val userData = UserRegistration(
            name = nameState.text.toString(),
            email = emailState.text.toString(),
            password = passwordState.text.toString()
        )

        if (isPasswordMatching(currentPassword, currentConfirmPassword)) {

            viewModelScope.launch {
                val payload = LogInPayload(userData.email, userData.password)
                val error = authRepository.createAccount(userData)
                val result = authRepository.accountLogin(payload)
                if (error == null && result == null) {
                    _uiState.update { it.copy(signInError = false, isSuccess = true, errorMessage = null) }
                } else {
                    _uiState.update { it.copy(signInError = true, errorMessage = error) }
                }
            }

        } else {
            _uiState.update { currentInstance ->
                currentInstance.copy(
                    signInError = true, errorMessage = "The two passwords do not match"
                )
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