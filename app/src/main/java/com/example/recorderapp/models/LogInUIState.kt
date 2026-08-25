package com.example.recorderapp.models

import kotlinx.serialization.Serializable

@Serializable
data class LogInUIState(
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)
