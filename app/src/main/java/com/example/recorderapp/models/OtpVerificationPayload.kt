package com.example.recorderapp.models

import kotlinx.serialization.Serializable

@Serializable
data class OtpVerificationPayload(
    val email: String,
    val otp_code: String
)