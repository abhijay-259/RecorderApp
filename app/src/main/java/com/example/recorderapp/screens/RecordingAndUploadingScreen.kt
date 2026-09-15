package com.example.recorderapp.screens

import android.text.Layout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recorderapp.R
import com.example.recorderapp.viewmodels.RUViewModel
import com.example.recorderapp.viewmodels.UploadState
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.milliseconds


@OptIn(ExperimentalMaterial3Api::class)
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
    var showSaveDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var confirmUpload by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text("Top app bar")
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = "Bottom app bar"
                )
            }
        },
//        floatingActionButton = {
//            FloatingActionButton(onClick = {}) {
//                Icon(
//                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
//                    contentDescription = "some button"
//                    )
//            }
//        }
    ) { innerPadding ->
        val saveButtonText = if (status == UploadState.UPLOADING) "CANCEL" else "SAVE"
//                        SAVE ALERT DIALOG
        if (showSaveDialog) {
            AlertDialog(
                title = { Text("Save Recording") },
                text = { Text("Are you sure you want to $saveButtonText this ${if (saveButtonText == "SAVE") "recording" else "upload"}?") },
                onDismissRequest = { showSaveDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.saveOrCancelButton()
                        showSaveDialog = false
                    }) {
                        Text("Confirm")
                    }
                }

            )
        }

        if (showCancelDialog) {
            AlertDialog(
                title = { Text("Cancel Recording")},
                text = { Text("Are you sure you want to cancel this recording?")},
                onDismissRequest = { showCancelDialog = false },
                confirmButton = {
                    viewModel.cancelRecordingButton()
                    showCancelDialog = false
                }
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val displayMessage = when (status) {
                UploadState.IDLE -> "-"
                UploadState.UPLOADING -> "Uploading"
                UploadState.COMPLETE -> "Upload Completed"
                UploadState.CANCELLED -> "Upload Cancelled"
                UploadState.RECORDING -> "Recording"
            }
//                                     HEADING
            Text(
                text = "Karya",
                fontFamily = FontFamily.Cursive,
                fontSize = 30.sp,
                modifier = Modifier.
                    padding(20.dp)
            )
            Row(){
//                                    CLOSE BUTTON
                Box(
                    modifier = Modifier
                        .clickable(onClick = {
                            showCancelDialog = true
                        })
                        .weight(0.2f)
                        .size(30.dp, 30.dp)
                        .padding(20.dp)
                        .drawBehind {
                            drawLine(
                                color = Color.Gray,
                                start = Offset(3.dp.toPx(), 3.dp.toPx()),
                                end = Offset(27.dp.toPx(), 27.dp.toPx()),
                                strokeWidth = 2.dp.toPx()
                            )
                            drawLine(
                                color = Color.Gray,
                                start = Offset(3.dp.toPx(), 27.dp.toPx()),
                                end = Offset(27.dp.toPx(), 3.dp.toPx()),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                )
//                               TASK NAME
                Text(
                    "Task Name",
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(0.6f)
                        .padding(20.dp)
                )
                Spacer(
                    modifier = Modifier
                        .weight(0.2f)
                        .padding(20.dp)
                )
            }
            val rotation = remember {Animatable(0f)}

            LaunchedEffect(status) {
                delay(500.milliseconds)
                if (status == UploadState.RECORDING) {
                    rotation.animateTo(
                        targetValue = rotation.value + 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 20000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        )
                    )
                } else {
                    rotation.stop()
                    rotation.snapTo(0f)
                }
            }
//                              ANIMATION PLAYED DURING RECORDING
            Canvas(
                modifier = Modifier
                    .size(280.dp)
                    .graphicsLayer { rotationZ = rotation.value % 360 }
            ) {
                val radius = size.minDimension / 2f
                val center = this.center

                // Outer white disc
                drawCircle(color = Color.White, radius = radius, center = center)

                // Off-center dumbbell: two dark circles + connecting bar
                val dotRadius = radius * 0.28f
                val offsetX = radius * 0.45f
                val leftCenter = Offset(center.x - offsetX, center.y)
                val rightCenter = Offset(center.x + offsetX, center.y)

                drawLine(
                    color = Color(0xFF2B2B2B),
                    start = leftCenter + Offset(0f,-18f),
                    end = rightCenter + Offset(0f,-18f),
                    strokeWidth = 6.dp.toPx()
                )
                drawCircle(color = Color(0xFF2B2B2B), radius = dotRadius, center = leftCenter + Offset(0f,-18f))
                drawCircle(color = Color(0xFF2B2B2B), radius = dotRadius, center = rightCenter + Offset(0f,-18f))

                // Center spindle hole
                drawCircle(color = Color.Black, radius = radius * 0.02f, center = center)
            }
            val borderRadius by animateIntAsState(
                targetValue = if(status == UploadState.RECORDING) 32 else 24,
                animationSpec = tween(easing = LinearEasing)
            )
            Row() {
//                         LIKE A SPACER
                Box(
                    modifier = Modifier
                        .padding(30.dp)
                        .size(60.dp)
                )
//                              START/STOP RECORDING BUTTON
                Box(
                    modifier = Modifier
                        .padding(30.dp)
                        .size(60.dp, 60.dp)
                        .drawBehind {
                            drawCircle(
                                color = Color.White,
                                center = this.center,
                                radius = 30.dp.toPx(),
                                style = Stroke(
                                    width = 3.dp.toPx()
                                )
                            )
                            drawCircle(
                                color = Color.Red,
                                center = this.center,
                                radius = borderRadius.dp.toPx()
                            )
                        }
                        .clickable(onClick = {
                            if (status == UploadState.IDLE && !hasMicPermission) {
                                permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            } else {
                                viewModel.startRecordingButton(hasMicPermission)
                            }
                        })
                )
                val backGroundColor = MaterialTheme.colorScheme.primaryContainer
                val textColor = MaterialTheme.colorScheme.primary
                val coroutineScope = rememberCoroutineScope()
//                          SAVE/CANCEL BUTTON
                Box(
                    modifier = Modifier
                        .padding(30.dp, 50.dp, 30.dp, 30.dp)
                        .size(60.dp)
                        .clip(RoundedCornerShape(30))
                        .clickable(onClick = {
                            showSaveDialog = true
                        })
                        .drawBehind {
                            drawRect(
                                color = backGroundColor,
                                topLeft = Offset(0f, 0f),
                                size = size
                            )
                        }

                ) {
                    Text(
                        text = saveButtonText,
                        color = textColor,
                        modifier = Modifier
                            .align(Alignment.Center)
                    )
                }
            }
            Text(
                when (isConnected) {
                    true -> "Connected to the internet"
                    false -> "Not Connected to the internet"
                }
            )
            Button(onClick = { onNavToSubmissionsClicked() }) {
                Text("Check Pending Submissions")
            }
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