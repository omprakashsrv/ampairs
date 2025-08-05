package com.ampairs.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.ampairs.auth.viewmodel.LoginViewModel
import com.ampairs.ui.components.Phone
import com.ampairs.ui.theme.AmpairsTheme
import org.jetbrains.compose.resources.stringResource
import ampairsapp.composeapp.generated.resources.Res
import ampairsapp.composeapp.generated.resources.login
import androidx.compose.desktop.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import org.koin.core.scope.Scope

@Composable
fun PhoneScreen(
    scope: Scope,
    viewModel: LoginViewModel = koinInject<LoginViewModel>(scope = scope),
    onAuthSuccess: (String) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Phone input section - Center aligned
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 280.dp, max = 400.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Phone(
                    countryCode = 91,
                    readOnly = viewModel.loading,
                    phone = viewModel.phoneNumber,
                    onValueChange = { viewModel.phoneNumber = it },
                    onValidChange = { viewModel.validPhoneNumber = it }
                )

                // Show reCAPTCHA status message
                if (viewModel.recaptchaMessage.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (viewModel.recaptchaLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
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
        }

        // Login button - Bottom aligned
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Button(
                onClick = { viewModel.authenticate(onAuthSuccess) },
                modifier = Modifier
                    .widthIn(min = 280.dp, max = 400.dp)
                    .fillMaxWidth(),
                enabled = viewModel.validPhoneNumber && !viewModel.loading
            ) {
                if (viewModel.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .progressSemantics()
                            .size(24.dp)
                    )
                } else {
                    Text(stringResource(Res.string.login))
                }
            }
        }
    }
}

@Composable
@Preview
fun PhoneScreenPreview() {
    AmpairsTheme {
        // Mock ViewModel state for preview
        var phoneNumber by remember { mutableStateOf("9876543210") }
        var validPhoneNumber by remember { mutableStateOf(true) }
        var recaptchaMessage by remember { mutableStateOf("Verifying you're human...") }
        var recaptchaLoading by remember { mutableStateOf(true) }
        var loading by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 280.dp, max = 400.dp)
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Phone input section
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Phone(
                        countryCode = 91,
                        readOnly = loading,
                        phone = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        onValidChange = { validPhoneNumber = it }
                    )

                    // Show reCAPTCHA status message
                    if (recaptchaMessage.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (recaptchaLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            Text(
                                text = recaptchaMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Login button
                Button(
                    onClick = { loading = !loading },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = validPhoneNumber && !loading
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .progressSemantics()
                                .size(24.dp)
                        )
                    } else {
                        Text(stringResource(Res.string.login))
                    }
                }
            }
        }
    }
}