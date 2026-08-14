package com.example.recorderapp.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recorderapp.viewmodels.RUViewModel
import com.example.recorderapp.viewmodels.UploadState
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import kotlinx.coroutines.launch


@Composable
fun RecordingAndUploadingScreen(
    viewModel: RUViewModel = viewModel(),
    onNavToSubmissionsClicked: () -> Unit
) {

    var hasMicPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        viewModel.onPermissionGranted(isGranted)
    }
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
    ) {
        val displayMessage = when (status) {
            UploadState.IDLE -> "-"
            UploadState.UPLOADING -> "Uploading"
            UploadState.COMPLETE -> "Upload Completed"
            UploadState.CANCELLED -> "Upload Cancelled"
            UploadState.RECORDING -> "Recording"
        }
        Text("Hello "+viewModel.currentUser!!.name)
        Text(   //status display
            text = displayMessage,
            modifier = Modifier.
            padding(vertical = 16.dp)
        )
        Button(onClick = {     // start/stop recording button
            if (status == UploadState.IDLE) {
                permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                viewModel.startRecordingButton(hasMicPermission)
            } else {
                viewModel.startRecordingButton(hasMicPermission)
            }
        }) {
            Text(
                text = if (status == UploadState.RECORDING) {
                    "Stop Recording"
                } else {
                    "start recording"
                }
            )
        }
        // start upload button
        Button(onClick = { viewModel.startUploadButton() }) {
            Text(
                text = "Start Upload"
            )
        }
        // stop upload button
        Button(onClick = { viewModel.cancelUploadButton() }) {
            Text(
                text = "Cancel Upload"
            )
        }
        Text(when(isConnected) {
            true -> "Connected to the internet"
            false -> "Not Connected to the internet"
        })
        Button(onClick = {onNavToSubmissionsClicked()}) {
            Text("Check Pending Submissions")
        }
    }
}

@Preview
@Composable
fun RecordingAndUploadingScreenPreview() {
    RecordingAndUploadingScreen(
        viewModel = viewModel(),
        onNavToSubmissionsClicked = {}
    )
}