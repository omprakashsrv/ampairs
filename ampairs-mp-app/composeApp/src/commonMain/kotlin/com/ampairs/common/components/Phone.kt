package com.ampairs.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ampairs.common.validation.ValidationResult
import com.ampairs.common.validation.phone.PhoneNumberValidator

@Composable
fun Phone(
    modifier: Modifier = Modifier,
    countryCode: Int,
    phone: String,
    readOnly: Boolean = false,
    onValueChange: (String) -> Unit,
    onValidChange: (Boolean) -> Unit,
) {
    var phoneNumber by remember {
        mutableStateOf(phone)
    }
    var countryCodeText by remember {
        mutableStateOf("+ $countryCode")
    }
    var valid by remember {
        mutableStateOf(true)
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        OutlinedTextField(
            modifier = Modifier
                .width(80.dp),
            value = countryCodeText,
            onValueChange = {
                countryCodeText = it
            },
            readOnly = true,
            maxLines = 1,
        )
        Column(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                readOnly = readOnly,
                value = phoneNumber,
                maxLines = 1,
                onValueChange = { phone ->
                    phoneNumber = phone.filter { it.isDigit() }
                    if (phoneNumber.isEmpty()) {
                        valid = true
                    } else {
                        try {
                            val result = PhoneNumberValidator().validate(phoneNumber)
                            valid = result is ValidationResult.Valid
                            onValueChange(phone)
                        } catch (e: Exception) {
                            valid = false
                        }
                    }
                    onValidChange(valid)
                },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                isError = !valid,
                label = { Text("Phone") })
        }

    }
}