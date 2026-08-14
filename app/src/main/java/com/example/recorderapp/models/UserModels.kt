package com.example.recorderapp.models

import kotlinx.serialization.Serializable

@Serializable
data class UserSessionProfile(
    val id: Int,
    val name: String,
    val email: String = ""
)
@Serializable
data class UserRegistration(
    val name: String,
    val email: String,
    val password: String
)
@Serializable
data class OtpVerificationPayload(
    val email: String,
    val otp_code: String
)
@Serializable
data class FastApiErrorResponse(val detail: String)
@Serializable
data class LogInPayload(
    val email: String,
    val password: String
)
@Serializable
data class LoginSuccessResponse(
    val status: String,
    val user: UserSessionProfile
)