package com.example.recorderapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.recorderapp.connectivity.AndroidConnectivityObserver
import com.example.recorderapp.repositories.AudioRepository
import com.example.recorderapp.repositories.AuthRepository
import com.example.recorderapp.room.SubmissionDao
import com.example.recorderapp.room.SubmissionDatabase
import com.example.recorderapp.screens.ConfirmAccountScreen
import com.example.recorderapp.screens.LogInScreen
import com.example.recorderapp.screens.PendingSubmissionsScreen
import com.example.recorderapp.screens.RecordingAndUploadingScreen
import com.example.recorderapp.screens.SignInScreen
import com.example.recorderapp.ui.theme.RecorderAppTheme
import com.example.recorderapp.viewmodels.ConfirmAccountViewModel
import com.example.recorderapp.viewmodels.LogInViewModel
import com.example.recorderapp.viewmodels.PendingSubmissionsViewModel
import com.example.recorderapp.viewmodels.RUViewModel
import com.example.recorderapp.viewmodels.SignInViewModel
import com.example.recorderapp.workers.AutoSubmissionsWorker
import kotlinx.serialization.Serializable

// KOTLIN TYPE SAFE ROUTES
@Serializable object SignIn
@Serializable object LogIn
@Serializable object ConfirmAccount
@Serializable object RecordUpload
@Serializable object PendingSubmissions
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val workRequest = OneTimeWorkRequestBuilder<AutoSubmissionsWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "AUTOMATED_AUDIO_SYNC_TASK",
            ExistingWorkPolicy.KEEP, // Keeps the existing worker active and prevents duplicate execution chains
            workRequest
        )
        enableEdgeToEdge()
        setContent {
            RecorderAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController() //nav object
                    val db = SubmissionDatabase.getDatabase(applicationContext)
                    val audioRepository = AudioRepository(applicationContext, db.dao)
                    val authRepository = AuthRepository(applicationContext)
                    navController.addOnDestinationChangedListener { _, destination, _ ->
                        // 1. Check if the screen the user is currently looking at matches your authentication gates
                        val currentScreenRoute = destination.route ?: ""

                        // 2. If they have backed up into the Sign In or Login routes, clear the memory tracker!
                        if (currentScreenRoute.contains("SignInRoute") || currentScreenRoute.contains("LoginRoute")) {
                            authRepository.clearSession()
                            android.util.Log.d("SESSION_LOG", "User exited the app workspace. Repository session cleared successfully!")
                        }
                    }
                    // The Navigation traffic router graph
                    NavHost(
                        navController = navController,
                        startDestination = SignIn
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
                                    navController.navigate(ConfirmAccount)
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
            }
        }
    }
}

@Preview
@Composable
fun SignInScreenPreview() {
    SignInScreen(
        viewModel = viewModel(),
        onRegistrationSuccess = {},
        onNavToLogInClicked = {}
    )
}
/* JSON: JavaScript Object Notation. Despite the name, it is a universal,
 language-independent text format used to storea and exchange data
 across the internet. At its core it's just a string of text formatted
 in a specific way so that different programming languages can easily
 read and write it

 JSON is built entirely on two structures:
 1. Key/Value Pairs, exactly like a python dictionary or a Kotlin class prpoerty
 2. Arrays: An ordered list of values
 Example:
{
    "task_id": 101,
    "worker_name": "Kazuto Ken",
    "recorded_text": "Hello from my nothing phone 2a",
    "is_complete": true
}
JSON Syntax Rules:
1. Data must always be enclosed in curly braces to represent an object
2. Keys must always be wrapped in double quotes ""
3. Values can be Strings, Numbers, Booleans, or Null

WHY DO WE USE IT? (DATA BRIDGE)
1. Kotlin usees a structured data class instance
2. Python sees an assosiative dictionary allocation

Routers only understand raw bytes and text streams. JSON acts as the
common universal language

*/
