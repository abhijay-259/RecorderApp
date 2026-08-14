package com.example.recorderapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recorderapp.connectivity.ConnectivityObserver
import com.example.recorderapp.repositories.AudioRepository
import com.example.recorderapp.repositories.AuthRepository
import com.example.recorderapp.room.SubmissionDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    val submissions = _submissions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val currentUser = authRepository.currentUserSession
    fun submitPendingSubmissions() {

    }

    fun deleteAllSubmissions() {
        viewModelScope.launch {
        }
    }
}