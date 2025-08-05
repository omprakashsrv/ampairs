package com.ampairs.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Otp(onValueChange: (String) -> Unit) {
    var otp by remember {
        mutableStateOf("")
    }

    Column(modifier = Modifier.fillMaxWidth(0.5f)) {
        OutlinedTextField(
            value = otp,
            onValueChange = {
                otp = it
                onValueChange(it)
            },
            maxLines = 1,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            label = { Text("OTP") },
            supportingText = { Text("Enter 6-digit OTP, received on phone") })
    }

}
