package com.example.recorderapp.models

import kotlinx.serialization.Serializable

@Serializable
data class LogInPayload(
    val email: String,
    val password: String
)
