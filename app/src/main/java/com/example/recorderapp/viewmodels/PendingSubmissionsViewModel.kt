package com.example.recorderapp.viewmodels

import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val _isPlaying = mutableStateOf(false)
    val isPlaying = _isPlaying
    private val _index = mutableIntStateOf(0)
    val index = _index
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

    fun playButton(filePath: String, index: Int) {
        val voiceFile = File(filePath)
        if (!voiceFile.exists()) {
            Log.e("PLAYBACK ERROR", "The file at $filePath cannot be found on disk!")
            return
        }
        if (_isPlaying.value && _index.intValue == index) {
            audioRepository.stopPlayback()
            _isPlaying.value = false
            _index.intValue = 0
        }
        else if (!_isPlaying.value) {

        }
    }
    fun deleteButton(id: Int) {
        viewModelScope.launch {
            dao.deleteSubmission(id)
        }
    }
}