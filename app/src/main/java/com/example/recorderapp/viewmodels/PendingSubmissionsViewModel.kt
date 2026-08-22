package com.example.recorderapp.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recorderapp.connectivity.ConnectivityObserver
import com.example.recorderapp.repositories.AudioRepository
import com.example.recorderapp.repositories.AuthRepository
import com.example.recorderapp.room.SubmissionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PendingSubmissionsViewModel(
    private val dao: SubmissionDao,
    private val connectivityObserver: ConnectivityObserver,
    private val audioRepository: AudioRepository,
    private val authRepository: AuthRepository
): ViewModel() {
    private val _submissions = dao.getSubmissions().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(),
        emptyList()
    )
    val submissions = dao.getSubmissions().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val currentUser = authRepository.currentUserSession
    fun submitPendingSubmissions() {
        Log.println(Log.ASSERT, "Entered", "submitPendingSubmissions in viewmodel")
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                audioRepository.uploadPendingSubmissions()
            }
        }
    }

    fun deleteAllSubmissions() {
        viewModelScope.launch {
        }
    }

    fun playButton(filePath: String) {
        val voiceFile = File(filePath)
        if (voiceFile.exists()) {
            viewModelScope.launch {
                audioRepository.startPlayback(voiceFile)
            }
        } else {
            android.util.Log.e("PLAYBACK ERROR", "The file at $filePath has been deleted or cannot be found on disk!")
        }
    }
}