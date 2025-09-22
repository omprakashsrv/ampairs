package com.ampairs.customer.ui.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import com.ampairs.ui.components.Phone
import com.ampairs.customer.ui.components.StateAutocomplete
import com.ampairs.customer.ui.components.StringAutocomplete
import com.ampairs.customer.domain.State

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerFormScreen(
    customerId: String? = null,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CustomerFormViewModel = koinInject { parametersOf(customerId) }
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(customerId) {
        if (customerId != null) {
            viewModel.loadCustomer()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (customerId == null) "New Customer" else "Edit Customer") },
            actions = {
                TextButton(
                    onClick = {
                        viewModel.saveCustomer { onSaveSuccess() }
                    },
                    enabled = uiState.canSave && !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save")
                    }
                }
            }
        )

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                CustomerForm(
                    formState = uiState.formState,
                    onFormChange = viewModel::updateForm,
                    error = uiState.error,
                    onSave = { viewModel.saveCustomer { onSaveSuccess() } },
                    canSave = uiState.canSave && !uiState.isSaving,
                    isSaving = uiState.isSaving,
                    states = uiState.states,
                    onStateSelected = viewModel::onStateSelected,
                    cities = uiState.cities,
                    pincodes = uiState.pincodes,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun CustomerForm(
    formState: CustomerFormState,
    onFormChange: (CustomerFormState) -> Unit,
    error: String?,
    onSave: () -> Unit,
    canSave: Boolean,
    isSaving: Boolean,
    states: List<State>,
    onStateSelected: (State) -> Unit,
    cities: List<String>,
    pincodes: List<String>,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (error != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Basic Information
        FormSection(title = "Basic Information") {
            OutlinedTextField(
                value = formState.name,
                onValueChange = { onFormChange(formState.copy(name = it)) },
                label = { Text("Name *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                ),
                isError = formState.nameError != null,
                supportingText = formState.nameError?.let { { Text(it) } },
                singleLine = true
            )

            OutlinedTextField(
                value = formState.email,
                onValueChange = { onFormChange(formState.copy(email = it)) },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                ),
                isError = formState.emailError != null,
                supportingText = formState.emailError?.let { { Text(it) } },
                singleLine = true
            )

            // Phone and Landline Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Phone(
                        countryCode = formState.countryCode,
                        phone = formState.phone,
                        onValueChange = { phone ->
                            onFormChange(formState.copy(phone = phone))
                        },
                        onValidChange = { /* Validation handled in ViewModel */ }
                    )

                    formState.phoneError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = formState.landline,
                        onValueChange = { onFormChange(formState.copy(landline = it)) },
                        label = { Text("Landline") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Next) }
                        ),
                        singleLine = true,
                        isError = formState.landlineError != null,
                        supportingText = formState.landlineError?.let { error ->
                            {
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )
                }
            }

            OutlinedTextField(
                value = formState.gstin,
                onValueChange = { onFormChange(formState.copy(gstin = it)) },
                label = { Text("GSTIN") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                ),
                singleLine = true,
                isError = formState.gstinError != null,
                supportingText = formState.gstinError?.let { { Text(it) } }
            )
        }

        // Address Information
        FormSection(title = "Address") {
            OutlinedTextField(
                value = formState.address,
                onValueChange = { onFormChange(formState.copy(address = it)) },
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                )
            )

            OutlinedTextField(
                value = formState.street,
                onValueChange = { onFormChange(formState.copy(street = it)) },
                label = { Text("Street") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) }
                ),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StringAutocomplete(
                    value = formState.city,
                    onValueChange = { onFormChange(formState.copy(city = it)) },
                    suggestions = cities,
                    label = "City",
                    modifier = Modifier.weight(1f),
                    imeAction = ImeAction.Next
                )

                StringAutocomplete(
                    value = formState.pincode,
                    onValueChange = { onFormChange(formState.copy(pincode = it)) },
                    suggestions = pincodes,
                    label = "PIN Code",
                    modifier = Modifier.weight(1f),
                    imeAction = ImeAction.Next
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StateAutocomplete(
                    value = formState.state,
                    onValueChange = { onFormChange(formState.copy(state = it)) },
                    onStateSelected = onStateSelected,
                    states = states,
                    modifier = Modifier.weight(1f),
                    label = "State",
                    imeAction = ImeAction.Next
                )

                OutlinedTextField(
                    value = formState.country,
                    onValueChange = { onFormChange(formState.copy(country = it)) },
                    label = { Text("Country") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    singleLine = true
                )
            }
        }

        // Billing Address Section
        FormSection(title = "Billing Address") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = formState.useBillingAsMainAddress,
                    onCheckedChange = { onFormChange(formState.copy(useBillingAsMainAddress = it)) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Same as main address",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (!formState.useBillingAsMainAddress) {
                OutlinedTextField(
                    value = formState.billingStreet,
                    onValueChange = { onFormChange(formState.copy(billingStreet = it)) },
                    label = { Text("Billing Street") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StringAutocomplete(
                        value = formState.billingCity,
                        onValueChange = { onFormChange(formState.copy(billingCity = it)) },
                        suggestions = cities,
                        label = "Billing City",
                        modifier = Modifier.weight(1f),
                        imeAction = ImeAction.Next
                    )
                    StringAutocomplete(
                        value = formState.billingPincode,
                        onValueChange = { onFormChange(formState.copy(billingPincode = it)) },
                        suggestions = pincodes,
                        label = "Billing PIN",
                        modifier = Modifier.weight(1f),
                        imeAction = ImeAction.Next
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StateAutocomplete(
                        value = formState.billingState,
                        onValueChange = { onFormChange(formState.copy(billingState = it)) },
                        onStateSelected = { state ->
                            onFormChange(formState.copy(billingState = state.name))
                        },
                        states = states,
                        modifier = Modifier.weight(1f),
                        label = "Billing State",
                        imeAction = ImeAction.Next
                    )
                    OutlinedTextField(
                        value = formState.billingCountry,
                        onValueChange = { onFormChange(formState.copy(billingCountry = it)) },
                        label = { Text("Billing Country") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Next) }
                        ),
                        singleLine = true
                    )
                }
            }
        }

        // Shipping Address Section
        FormSection(title = "Shipping Address") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = formState.useShippingAsMainAddress,
                    onCheckedChange = { onFormChange(formState.copy(useShippingAsMainAddress = it)) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Same as main address",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (!formState.useShippingAsMainAddress) {
                OutlinedTextField(
                    value = formState.shippingStreet,
                    onValueChange = { onFormChange(formState.copy(shippingStreet = it)) },
                    label = { Text("Shipping Street") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    ),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StringAutocomplete(
                        value = formState.shippingCity,
                        onValueChange = { onFormChange(formState.copy(shippingCity = it)) },
                        suggestions = cities,
                        label = "Shipping City",
                        modifier = Modifier.weight(1f),
                        imeAction = ImeAction.Next
                    )
                    StringAutocomplete(
                        value = formState.shippingPincode,
                        onValueChange = { onFormChange(formState.copy(shippingPincode = it)) },
                        suggestions = pincodes,
                        label = "Shipping PIN",
                        modifier = Modifier.weight(1f),
                        imeAction = ImeAction.Next
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StateAutocomplete(
                        value = formState.shippingState,
                        onValueChange = { onFormChange(formState.copy(shippingState = it)) },
                        onStateSelected = { state ->
                            onFormChange(formState.copy(shippingState = state.name))
                        },
                        states = states,
                        modifier = Modifier.weight(1f),
                        label = "Shipping State",
                        imeAction = ImeAction.Done
                    )
                    OutlinedTextField(
                        value = formState.shippingCountry,
                        onValueChange = { onFormChange(formState.copy(shippingCountry = it)) },
                        label = { Text("Shipping Country") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        singleLine = true
                    )
                }
            }
        }

        // Save Button Section
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Button(
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving...")
                } else {
                    Text("Save Customer")
                }
            }
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}