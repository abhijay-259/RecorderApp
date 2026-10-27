package com.example.recorderapp

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.recorderapp.connectivity.AndroidConnectivityObserver
import com.example.recorderapp.repositories.AudioRepository
import com.example.recorderapp.repositories.AuthRepository
import com.example.recorderapp.room.SubmissionDatabase
import com.example.recorderapp.screens.ConfirmAccountScreen
import com.example.recorderapp.screens.LogInScreen
import com.example.recorderapp.screens.PendingSubmissionsScreen
import com.example.recorderapp.screens.RecordingAndUploadingScreen
import com.example.recorderapp.screens.SignInScreen
import com.example.recorderapp.viewmodels.ConfirmAccountViewModel
import com.example.recorderapp.viewmodels.LogInViewModel
import com.example.recorderapp.viewmodels.PendingSubmissionsViewModel
import com.example.recorderapp.viewmodels.RUViewModel
import com.example.recorderapp.viewmodels.SignInViewModel
import kotlinx.serialization.Serializable

// KOTLIN TYPE SAFE ROUTES
@Serializable object SignIn
@Serializable object LogIn
@Serializable object ConfirmAccount
@Serializable object RecordUpload
@Serializable object PendingSubmissions
@Composable
fun AppNav(
    navController: NavHostController,
    authRepository: AuthRepository,
    audioRepository: AudioRepository,
    db: SubmissionDatabase,
    applicationContext: Context
    ) {
    val startDestination: Any = remember {
        if (authRepository.currentUserSession != null) RecordUpload else SignIn
    }
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<SignIn> {
            val signInViewModel = viewModel<SignInViewModel>(
                factory = object: ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return SignInViewModel(
                            authRepository
                        ) as T
                    }
                }
            )
            SignInScreen(
                viewModel = signInViewModel,
                onRegistrationSuccess = {
                    navController.navigate(RecordUpload)
                },
                onNavToLogInClicked = {
                    navController.navigate(LogIn) {
                        popUpTo(SignIn) {inclusive = true}
                    }
                }
            )
        }
        composable<LogIn> {
            val logInViewModel = viewModel<LogInViewModel>(
                factory = object: ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return LogInViewModel(
                            authRepository
                        ) as T
                    }
                }
            )
            LogInScreen(
                viewModel = logInViewModel,
                onAuthSuccess = {navController.navigate(RecordUpload)},
                onNavToSignInClicked = {navController.navigate(SignIn) {
                    popUpTo(LogIn) {inclusive = true}
                } }
            )
        }
        composable<ConfirmAccount> {
            val confirmAccountViewModel = viewModel<ConfirmAccountViewModel>(
                factory = object: ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return ConfirmAccountViewModel(
                            authRepository
                        ) as T
                    }
                }
            )
            ConfirmAccountScreen(
                onConfirmButtonClicked = {navController.navigate(RecordUpload){
                    popUpTo(ConfirmAccount) {inclusive = true}
                } }
            )
        }
        composable<RecordUpload> {
            val ruViewModel = viewModel<RUViewModel>(
                factory = object: ViewModelProvider.Factory {
                    override fun <T: ViewModel> create(modelClass: Class<T>): T {
                        return RUViewModel(
                            db.dao,
                            AndroidConnectivityObserver(
                                context = applicationContext
                            ),
                            audioRepository,
                            authRepository
                        ) as T
                    }
                }
            )
            RecordingAndUploadingScreen(
                viewModel = ruViewModel,
                onNavToSubmissionsClicked = {navController.navigate(
                    PendingSubmissions)}
            )
        }
        composable<PendingSubmissions> {
            val pendingSubmissionsViewModel = viewModel<PendingSubmissionsViewModel>(
                factory = object: ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return PendingSubmissionsViewModel(
                            db.dao,
                            AndroidConnectivityObserver(applicationContext),
                            audioRepository,
                            authRepository
                        ) as T
                    }
                }
            )
            PendingSubmissionsScreen(
                pendingSubmissionsViewModel
            )
        }
    }
}