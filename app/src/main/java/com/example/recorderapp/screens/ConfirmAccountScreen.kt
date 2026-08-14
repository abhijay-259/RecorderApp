package com.example.recorderapp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recorderapp.viewmodels.ConfirmAccountViewModel

@Composable
fun ConfirmAccountScreen(
    viewModel: ConfirmAccountViewModel = viewModel(),
    onConfirmButtonClicked: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(24.dp)
    ) {
        Text("A One-Time Password (OTP) has been sent to your email address",
            textAlign = TextAlign.Center)
        Text("Please enter the OTP below")
        OtpTextField(viewModel.otpState)
        Button({
            onConfirmButtonClicked()
        }) {
            Text("Confirm OTP")
        }
        state.errorMessage?.let { errorText ->
            Text(
                text = errorText, // Displays the exact server-side message string flawlessly!
                color = androidx.compose.ui.graphics.Color.Red,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun OtpTextField(otp: TextFieldState) {
    Box() {
        OutlinedTextField(
            otp,
            inputTransformation = InputTransformation {
                for (i in length -1 downTo 0) {
                    if (!(charAt(i).isDigit())) delete(i, i+1)
                }
                if (length>6) {
                    delete(6, length)
                }
            }
        )
    }
}

@Preview
@Composable
fun ConfirmAccountScreenPreview() {
    ConfirmAccountScreen(
        onConfirmButtonClicked = {}
    )
}