package com.ampairs.customer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ampairs.customer.domain.Customer
import com.ampairs.customer.viewmodel.CustomerViewModel
import com.ampairs.ui.components.Phone
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf


@Composable
fun CustomerScreen(modifier: Modifier = Modifier, id: String?, onCustomerUpdate: (String) -> Unit) {
    val viewModel: CustomerViewModel = koinInject { parametersOf(id) }

    if (id.isNullOrEmpty()) {
        viewModel.customer = CustomerState(Customer())
    }
    val customer = viewModel.customer
    if (!id.isNullOrEmpty() && customer.customer.id.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
        viewModel.reSyncCustomer(id)
    } else {
        val inputModifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp).fillMaxWidth()
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LazyVerticalStaggeredGrid(
                modifier = Modifier.weight(1f),
                columns = StaggeredGridCells.Adaptive(320.dp),
                verticalItemSpacing = 4.dp,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                content = {
                    items(3) { index ->
                        when (index) {
                            0 -> {
                                Column(
                                    modifier = Modifier.padding(2.dp).fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        modifier = inputModifier,
                                        label = { Text("Name") },
                                        value = customer.name,
                                        maxLines = 1,
                                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                                        onValueChange = {
                                            customer.name = it
                                        },
                                    )
                                    Phone(
                                        modifier = inputModifier,
                                        countryCode = customer.countryCode,
                                        phone = customer.phone.toString(),
                                        onValueChange = {
                                            customer.phone = it.filter { it.isDigit() }
                                        },
                                        onValidChange = {

                                        })
                                    OutlinedTextField(
                                        modifier = inputModifier,
                                        label = { Text("Landline") },
                                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Phone),
                                        value = customer.landline.toString(),
                                        onValueChange = {
                                            customer.landline = it.filter { it.isDigit() }
                                        },
                                        maxLines = 1,
                                    )
                                    OutlinedTextField(
                                        modifier = inputModifier,
                                        label = { Text("Email") },
                                        value = customer.email.toString(),
                                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
                                        onValueChange = {
                                            customer.email = it.lowercase().replace(" ", "")
                                        },
                                        maxLines = 1,
                                    )
                                    OutlinedTextField(
                                        modifier = inputModifier,
                                        label = { Text("GSTIN") },
                                        value = customer.gstin.toString(),
                                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                                        onValueChange = {
                                            customer.gstin = it.uppercase().replace(" ", "")
                                        },
                                        maxLines = 1,
                                    )
                                    Text("Registered Address", modifier = Modifier.padding(4.dp))
                                    OutlinedTextField(
                                        modifier = inputModifier,
                                        label = { Text("Street") },
                                        value = customer.street.toString(),
                                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                                        onValueChange = {
                                            customer.street = it
                                        },
                                        maxLines = 1,
                                        textStyle = MaterialTheme.typography.labelMedium
                                    )
                                    OutlinedTextField(
                                        modifier = inputModifier,
                                        label = { Text("Street 1") },
                                        value = customer.street2.toString(),
                                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                                        onValueChange = {
                                            customer.street2 = it
                                        },
                                        maxLines = 1,
                                        textStyle = MaterialTheme.typography.labelMedium
                                    )
                                    OutlinedTextField(
                                        modifier = inputModifier,
                                        label = { Text("Address") },
                                        value = customer.address.toString(),
                                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                                        onValueChange = {
                                            customer.address = it
                                        },
                                        maxLines = 2,
                                        textStyle = MaterialTheme.typography.labelMedium
                                    )
                                    OutlinedTextField(
                                        modifier = inputModifier,
                                        label = { Text("City") },
                                        value = customer.city.toString(),
                                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                                        onValueChange = {
                                            customer.city = it
                                        },
                                        maxLines = 1,
                                        textStyle = MaterialTheme.typography.labelMedium
                                    )
                                    OutlinedTextField(
                                        modifier = inputModifier,
                                        label = { Text("Pincode") },
                                        value = customer.pincode.toString(),
                                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                        onValueChange = {
                                            customer.pincode = it.filter { it.isDigit() }
                                        },
                                        maxLines = 1,
                                    )
                                    OutlinedTextField(
                                        modifier = inputModifier,
                                        label = { Text("State") },
                                        value = customer.state.toString(),
                                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                                        onValueChange = {
                                            customer.state = it
                                        },
                                        maxLines = 1,
                                    )
                                }
                            }

                            1 -> {
                                Column {
                                    Row {
                                        Text(
                                            "Billing Address",
                                            modifier = Modifier.padding(
                                                horizontal = 4.dp,
                                                vertical = 4.dp
                                            )
                                                .align(Alignment.CenterVertically).weight(1f)
                                        )
                                        Switch(
                                            checked = !customer.billingSameAsRegistered,
                                            onCheckedChange = {
                                                customer.billingSameAsRegistered = !it
                                            }
                                        )
                                    }
                                    if (!customer.billingSameAsRegistered) {
                                        AddressScreen(customer.billingAddress) { address ->
                                            customer.billingAddress = address
                                        }
                                    }
                                }
                            }

                            2 -> {
                                Column {
                                    Row {
                                        Text(
                                            "Shipping Address",
                                            modifier = Modifier.padding(
                                                horizontal = 4.dp,
                                                vertical = 4.dp
                                            )
                                                .align(Alignment.CenterVertically).weight(1f),
                                        )
                                        Switch(
                                            checked = !customer.shippingSameAsBilling,
                                            onCheckedChange = {
                                                customer.shippingSameAsBilling = !it
                                            }
                                        )
                                    }
                                    if (!customer.shippingSameAsBilling) {
                                        AddressScreen(customer.shippingAddress) { address ->
                                            customer.shippingAddress = address
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
            )



            Column(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ElevatedButton(onClick = {
                    val updatedCustomerId = viewModel.updateCustomer()
                    onCustomerUpdate(updatedCustomerId)
                }) {
                    if (viewModel.loading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .progressSemantics()
                                .size(24.dp)
                        )
                    } else {
                        Text(if (viewModel.id.isNullOrEmpty()) "Create New" else "Update")
                    }
                }
            }
        }
    }
}