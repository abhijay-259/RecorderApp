package com.example.recorderapp.repositories

import android.content.Context
import com.example.recorderapp.models.FastApiErrorResponse
import com.example.recorderapp.models.LogInPayload
import com.example.recorderapp.models.LoginSuccessResponse
import com.example.recorderapp.models.OtpVerificationPayload
import com.example.recorderapp.models.UserRegistration
import com.example.recorderapp.models.UserSessionProfile
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class AuthRepository() {

    var currentUserSession: UserSessionProfile? = null
        private set
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }
    suspend fun createAccount(registrationData: UserRegistration): String? {
        return try {
            // 1. Send a standard HTTP POST request to the exact server path
            val response: HttpResponse = client.post("http://$IP:8000/register") {
                // 2. Instruct Ktor to format the request body as a standard JSON string payload
                contentType(ContentType.Application.Json)
                // 3. Drop your clean Kotlin data class object into the body slot.
                // Ktor will automatically serialize it into JSON keys that match Python!
                setBody(registrationData)
            }
            // 4. Your FastAPI script explicitly sends 'status.HTTP_201_CREATED' (201) on success!
            if (response.status.value == 201) {
                null
            } else {
                val rawErrorJsonText =response.bodyAsText()
                val parsedError = Json.decodeFromString<FastApiErrorResponse>(rawErrorJsonText)
                parsedError.detail
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Network Connection Lost"
        }
    }
    suspend fun verifyAccountOtp(email: String, otpCode: String): String? {
        return try {
            // Construct the type-safe model envelope
            val payload = OtpVerificationPayload(email = email, otp_code = otpCode)

            val response = client.post("http://$IP:8000/verify-otp") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            if (response.status.value == 200) {
                null // Perfect success! No error text string to return.
            } else {
                // 1. EXTRACT THE ENVELOPE: Read the raw JSON error string sent by Python
                val rawErrorJsonText = response.bodyAsText()

                // 2. PARSE THE FIELD: Use your serialization tools to map it to our class
                val parsedError = Json.decodeFromString<FastApiErrorResponse>(rawErrorJsonText)

                // 3. Return the specific message (e.g. "OTP has expired!")
                parsedError.detail
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Network connection lost. Please verify your Wi-Fi link."
        }
    }
    suspend fun accountLogin(payload: LogInPayload): String? {
        return try {
            val response = client.post("http://$IP:8000/login"){
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            if (response.status.value == 200) {
                val successData = response.body<LoginSuccessResponse>()
                currentUserSession = UserSessionProfile(
                    successData.user.id,
                    successData.user.name,
                    payload.email
                    )
                null
            }
            else {
                "Invalid Credentials or Unverified Account"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Network Error"
        }
    }
    fun clearSession() {
        currentUserSession = null
    }
    companion object {
        const val IP: String = "192.168.88.2"
    }
}