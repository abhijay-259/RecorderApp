package com.example.recorderapp.models

import kotlinx.serialization.Serializable

@Serializable
data class LoginSuccessResponse(
    val status: String,
    val user: UserSessionProfile
)
