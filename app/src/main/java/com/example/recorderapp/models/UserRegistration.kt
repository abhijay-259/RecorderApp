package com.example.recorderapp.models

import kotlinx.serialization.Serializable

@Serializable
data class UserRegistration(
    val name: String,
    val email: String,
    val password: String
)



