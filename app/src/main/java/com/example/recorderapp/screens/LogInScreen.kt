package com.example.recorderapp.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recorderapp.viewmodels.LogInViewModel

@Composable
fun LogInScreen(
    viewModel: LogInViewModel = viewModel(),
    onAuthSuccess: () -> Unit,
    onNavToSignInClicked: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isSuccess) {
        onAuthSuccess()
        viewModel.resetSuccessState()
    }

    Column(
        modifier = Modifier
            .padding(24.dp),
        verticalArrangement =Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Karya",
            modifier = Modifier
                .padding(20.dp),
            fontFamily = FontFamily.Cursive,
            fontSize = 30.sp,
            )
        Text(
            "Login",
            modifier = Modifier.padding(24.dp)
        )
        Row() {
            Text(
                "Email:",
                modifier = Modifier.weight(0.2f),
            )
            OutlinedTextField(
                viewModel.emailState,
                modifier = Modifier
                    .weight(0.8f)
            )
        }
        Row() {
            Text(text = "Password:", modifier = Modifier.weight(0.2f))
            OutlinedTextField(
                state = viewModel.passState,
                modifier = Modifier
                    .weight(0.8f)
            )
        }
        Spacer(modifier = Modifier.padding(12.dp))
        Button(onClick = {
            viewModel.logInButton()
        }) {
            Text("Login")
        }
        Row(modifier = Modifier.padding(20.dp)) {
            Spacer(modifier = Modifier.weight(0.2f))
            Text(
                "Don't have an account?",
                modifier = Modifier
                    .clickable(onClick = {})
            )
            TextButton(
                onClick = { onNavToSignInClicked() }) {
                Text("Sign In")
            }
        }
        if (state.errorMessage != null) {
            Text(state.errorMessage.toString())
        }
    }
}

@Preview
@Composable
fun LogInScreenPreview() {
    LogInScreen(
        viewModel = viewModel(),
        onAuthSuccess = {},
        onNavToSignInClicked = {}
    )
}