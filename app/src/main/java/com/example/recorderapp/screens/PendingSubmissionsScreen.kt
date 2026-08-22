package com.example.recorderapp.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.recorderapp.R
import com.example.recorderapp.repositories.AuthRepository
import com.example.recorderapp.room.SubmissionDao

@Composable
fun PendingSubmissionsScreen(
    viewModel: PendingSubmissionsViewModel = viewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 60.dp, bottom = 20.dp, start = 40.dp, end = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            "Hello ${viewModel.currentUser?.name.toString()}",
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.padding(10.dp))
        Text("Pending Submissions", fontSize = 20.sp)
        Spacer(Modifier.padding(20.dp))
        val submissionList = viewModel.submissions.collectAsState().value

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(submissionList.size) { index ->
                val currentSubmission = submissionList[index]
                PendingSubmission(
                    taskID = currentSubmission.task_id.toString(),
                    date = "Sample date",
                    function = {viewModel.playButton(currentSubmission.filePath)}
                )
            }
        }
        Button(
            onClick = {
                Log.println(Log.ASSERT, "Entered", "submit all pressed")
                viewModel.submitPendingSubmissions()
            }
        ) {
            Text("Submit all")
        }
        Button(onClick = {
            viewModel.deleteAllSubmissions()
        }) {
            Text("Delete all submissions")
        }
    }
}

@Preview
@Composable
fun PendingSubmissionsScreenPreview() {
    PendingSubmissionsScreendc(
//        viewModel = viewModel()
    )
}

@Composable
fun PendingSubmission(taskID: String, date: String, function: () -> Unit) {
    Row() {
        Text("1")
        Column(
            modifier = Modifier
                .weight(0.8f)
        ) {
            Text(taskID)
            Text(date)
        }
        Icon(
            painter = painterResource(id = R.drawable.ic_play),
            contentDescription = "Play",
            modifier = Modifier
                .clickable(onClick = function)
                .weight(0.2f)
        )
    }
}

@Composable
fun PendingSubmissionsScreendc() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 60.dp, bottom = 20.dp, start = 40.dp, end = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            "Hello User",
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.padding(10.dp))
        Text("Pending Submissions", fontSize = 20.sp)
        Spacer(Modifier.padding(20.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(20) { index ->
                PendingSubmission(
                    taskID = "103",
                    date = "Sample date",
                    function = {}
                )
            }
        }
        Button(
            onClick = { }
        ) {
            Text("Submit all")
        }
        Button(onClick = {}) {
            Text("Delete all submissions")
        }
    }
}