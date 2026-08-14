package com.example.recorderapp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.util.TableInfo
import com.example.recorderapp.viewmodels.PendingSubmissionsViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.forEach
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingSubmissionsScreen(
    viewModel: PendingSubmissionsViewModel = viewModel()
) {
    Column(
        modifier = Modifier
        .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Hello ${viewModel.currentUser?.name.toString()}",
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.padding(10.dp))
        Text("Pending Submissions", fontSize = 20.sp)
        viewModel.submissions.collectAsState().value.forEach {
            Row() {
                Text("Submission: ${it.submission_id} ${it.worker_name} ")
            }
        }
        Button(
            onClick = { viewModel.submitPendingSubmissions() }
        ) {
            Text("Submit all")
        }
        Button(onClick = {viewModel.deleteAllSubmissions()}) {
            Text("Delete all submissions")
        }
    }
}

@Preview
@Composable
fun PendingSubmissionsScreenPreview() {
    PendingSubmissionsScreen()
}