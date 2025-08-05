package com.ampairs.customer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ampairs.order.domain.Address
import com.ampairs.ui.components.Phone


@Composable
fun AddressScreen(address: Address, onAddressChange: (Address) -> (Unit)) {

    val addressState by remember { mutableStateOf(AddressState(address)) }

    Column(
        modifier = Modifier.padding(2.dp).fillMaxWidth()
    ) {
        val inputModifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp).fillMaxWidth()

        OutlinedTextField(
            modifier = inputModifier,
            label = { Text("Name") },
            value = addressState.attention,
            maxLines = 1,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
            onValueChange = {
                addressState.attention = it
                onAddressChange(addressState.getAddress())
            },
        )
        Phone(
            countryCode = 91,
            phone = addressState.phone,
            onValueChange = { value ->
                addressState.phone = value.filter { it.isDigit() }
                onAddressChange(addressState.getAddress())
            },
            onValidChange = {

            })
        OutlinedTextField(
            modifier = inputModifier,
            label = { Text("Street") },
            value = addressState.street,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
            onValueChange = {
                addressState.street = it
                onAddressChange(addressState.getAddress())
            },
            maxLines = 1,
            textStyle = MaterialTheme.typography.labelMedium
        )
        OutlinedTextField(
            modifier = inputModifier,
            label = { Text("Street 1") },
            value = addressState.street2.toString(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
            onValueChange = {
                addressState.street2 = it
                onAddressChange(addressState.getAddress())
            },
            maxLines = 1,
            textStyle = MaterialTheme.typography.labelMedium
        )
        OutlinedTextField(
            modifier = inputModifier,
            label = { Text("Address") },
            value = addressState.address.toString(),
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
            onValueChange = {
                addressState.address = it
                onAddressChange(addressState.getAddress())
            },
            maxLines = 2,
            textStyle = MaterialTheme.typography.labelMedium
        )
        OutlinedTextField(
            modifier = inputModifier,
            label = { Text("City") },
            value = addressState.city,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
            onValueChange = {
                addressState.city = it
                onAddressChange(addressState.getAddress())
            },
            maxLines = 1,
            textStyle = MaterialTheme.typography.labelMedium
        )
        OutlinedTextField(
            modifier = inputModifier,
            label = { Text("Pincode") },
            value = addressState.zip,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            onValueChange = { value ->
                addressState.zip = value.filter { it.isDigit() }
                onAddressChange(addressState.getAddress())
            },
            maxLines = 1,
        )
        OutlinedTextField(
            modifier = inputModifier,
            label = { Text("State") },
            value = addressState.state,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
            onValueChange = {
                addressState.state = it
                onAddressChange(addressState.getAddress())
            },
            maxLines = 1,
        )
    }
}