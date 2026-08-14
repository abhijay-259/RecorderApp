package com.example.recorderapp.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.recorderapp.viewmodels.SignInViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SignInScreen(
    viewModel: SignInViewModel = viewModel(),
    onRegistrationSuccess: () -> Unit,
    onNavToLogInClicked: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // The with lifecycle ensures the app doesn't keep running this data collection loop
    // in the background once the screen goes down or sleeps

    if (state.isSuccess) {
        onRegistrationSuccess()
        viewModel.resetSuccessState()
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(24.dp)
    ) {
        Text("Karya",
            fontFamily = FontFamily.Cursive,
            fontStyle = FontStyle.Italic,
            fontSize = 30.sp,
            modifier = Modifier
                .padding(20.dp)
        )
        Text(
            modifier = Modifier.padding(24.dp),
            text = "Create an Account"
        )
        FieldText("Name:", viewModel.nameState, 0.8f)
        FieldText("Email:", viewModel.emailState, 0.8f)
        FieldText("Password:", viewModel.passwordState, 0.8f)
        FieldText("Confirm Password:", viewModel.confirmPasswordState, 0.8f)
        Button(
            onClick = {
                viewModel.createAccountButton()
            }) {
            Text("Create Account")
        }
        Row(modifier = Modifier
            .padding(24.dp)) {
            Text("Already have an account?")
            TextButton(onClick = {
                onNavToLogInClicked()
            }) {
                Text("Log In")
            }
        }
        if (state.errorMessage != null) {
            Text(state.errorMessage.toString())
        }


    }
}

@Composable
fun FieldText(
    name: String,
    state: TextFieldState,
    weight: Float
) {
    Row {
        Text(
            text = name,
            modifier = Modifier.weight(1-weight),
        )
        OutlinedTextField(
            state = state,
            modifier = Modifier
                .weight(weight)
        )
    }
}
@Preview
@Composable
fun SignInScreenPreview() {
    SignInScreen(
        viewModel = viewModel(),
        onRegistrationSuccess = {},
        onNavToLogInClicked = {}
    )
}
