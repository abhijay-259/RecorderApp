package com.example.recorderapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recorderapp.connectivity.ConnectivityObserver
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

import com.example.recorderapp.repositories.AudioRepository
import com.example.recorderapp.repositories.AuthRepository
import com.example.recorderapp.room.SubmissionDao
import com.example.recorderapp.room.SubmissionDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlin.time.Duration.Companion.seconds

enum class UploadState {
    RECORDING,
    IDLE,
    UPLOADING,
    CANCELLED,
    COMPLETE,
}

class RUViewModel(
    private val dao: SubmissionDao,
    private val connectivityObserver: ConnectivityObserver,
    private val audioRepository: AudioRepository,
    private val authRepository: AuthRepository
): ViewModel() {
    val isConnected = connectivityObserver
        .isConnected
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),   //wont observe if the app is in bg for more than 5s
            false
            )
    var uploadJob: Job? = null
    var currentFileName: String? = null
    val currentUser = authRepository.currentUserSession
    private val _status = MutableStateFlow(UploadState.IDLE)
    val status = _status.asStateFlow()
    fun onPermissionGranted(isGranted: Boolean) {
        if (isGranted) {
            // The user tapped "Allow"!
            _status.value = UploadState.RECORDING
            audioRepository.startRecording()
        } else {
            // The user tapped "Deny"
            _status.value = UploadState.IDLE
        }
    }
    fun startRecordingButton(hasMicPermission: Boolean) {
        if (status.value == UploadState.IDLE) {
            if (hasMicPermission) {
                currentFileName = null
                _status.value = UploadState.RECORDING
                currentFileName = audioRepository.startRecording()
            }
        }
        else if (status.value == UploadState.RECORDING) {
            audioRepository.stopRecording()
            _status.value = UploadState.IDLE
        }
    }

    fun startUploadButton() {
        uploadJob = viewModelScope.launch {
            _status.value = UploadState.UPLOADING
            val successStatus = audioRepository.startUploading(
                currentUser,
                isConnected.value,
                dao,
                currentFileName!!
            )
            if (successStatus) {
                _status.value = UploadState.COMPLETE
                currentFileName = null
                delay(3.seconds)
                _status.value = UploadState.IDLE
            }
            else {
                _status.value = UploadState.CANCELLED
                delay(3.seconds)
                _status.value = UploadState.IDLE
            }
        }
    }

    fun cancelUploadButton() {
        if (status.value == UploadState.UPLOADING) {
            uploadJob?.cancel()
                viewModelScope.launch {
                    _status.value = UploadState.CANCELLED
                    delay(3.seconds)
                    _status.value = UploadState.IDLE
                }

        }
    }
}