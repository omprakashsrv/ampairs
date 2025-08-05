package com.ampairs.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ampairs.auth.viewmodel.LoginViewModel
import com.ampairs.ui.components.Phone
import org.koin.compose.koinInject
import org.koin.core.scope.Scope

@Composable
fun PhoneScreen(
    scope: Scope,
    viewModel: LoginViewModel = koinInject<LoginViewModel>(scope = scope),
    onAuthSuccess: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(0.dp, 360.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center
            ) {
                Phone(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    countryCode = 91,
                    readOnly = viewModel.loading,
                    phone = viewModel.phoneNumber,
                    onValueChange = { viewModel.phoneNumber = it },
                    onValidChange = { viewModel.validPhoneNumber = it })
                
                // Show reCAPTCHA status message
                if (viewModel.recaptchaMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (viewModel.recaptchaLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                        Text(
                            text = viewModel.recaptchaMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Row(modifier = Modifier.padding(vertical = 16.dp)) {
                Button(
                    onClick = { viewModel.authenticate(onAuthSuccess) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = viewModel.validPhoneNumber && !viewModel.loading
                ) {
                    if (viewModel.loading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .progressSemantics()
                                .size(24.dp)
                        )
                    } else {
                        Text("Log in")
                    }
                }
            }
        }
    }

}