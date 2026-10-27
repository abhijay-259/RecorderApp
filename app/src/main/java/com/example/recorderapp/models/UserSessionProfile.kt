package com.example.recorderapp.models

import kotlinx.serialization.Serializable

@Serializable
data class UserSessionProfile(
    val id: String,
    val name: String,
    val email: String = ""
)

