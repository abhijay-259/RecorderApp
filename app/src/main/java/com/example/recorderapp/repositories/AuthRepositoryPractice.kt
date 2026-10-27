package com.example.recorderapp.repositories

import android.service.autofill.UserData
import com.example.recorderapp.models.LogInPayload
import com.example.recorderapp.models.UserRegistration
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GetTokenResult
import kotlinx.coroutines.tasks.await

class AuthRepositoryPractice {
    var auth: FirebaseAuth = FirebaseAuth.getInstance()
    suspend fun createAccount(userRegistration: UserRegistration): String? {
        try {
            val task = auth.createUserWithEmailAndPassword(userRegistration.email, userRegistration.password)
            val result = task.await()
            return null
        } catch (e: Exception) {
            e.printStackTrace()
            return e.message
        }
    }
    suspend fun accountLogin(payload: LogInPayload): String? {
        try {
            val task = auth.signInWithEmailAndPassword(payload.email, payload.password)
            val result = task.await()
            return null
        } catch (e: Exception) {
            e.printStackTrace()
            return e.message
        }
    }
    suspend fun getIdToken(): String? {
        val task = auth.currentUser?.getIdToken(false)
        val result = task?.await()
        return result?.token
    }
}