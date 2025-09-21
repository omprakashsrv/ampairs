package com.ampairs.customer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.ampairs.customer.domain.State
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun StateAutocomplete(
    value: String,
    onValueChange: (String) -> Unit,
    onStateSelected: (State) -> Unit,
    states: List<State>,
    modifier: Modifier = Modifier,
    label: String = "State",
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Next
) {
    var expanded by remember { mutableStateOf(false) }
    var hasFocus by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Filter states based on input
    val filteredStates = remember(value, states) {
        if (value.isBlank()) {
            states.take(10) // Show top 10 states when empty
        } else {
            states.filter {
                it.name.contains(value, ignoreCase = true)
            }.take(10)
        }
    }

    Column(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded && filteredStates.isNotEmpty() && hasFocus,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    onValueChange(newValue)
                    expanded = newValue.isNotEmpty()
                },
                label = { Text(label) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .onFocusChanged { focusState ->
                        hasFocus = focusState.isFocused
                        if (focusState.isFocused && value.isNotEmpty()) {
                            expanded = true
                        } else if (!focusState.isFocused) {
                            expanded = false
                        }
                    },
                keyboardOptions = KeyboardOptions(imeAction = imeAction),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Next) },
                    onDone = { focusManager.clearFocus() }
                ),
                singleLine = true,
                isError = isError,
                supportingText = supportingText
            )

            ExposedDropdownMenu(
                expanded = expanded && filteredStates.isNotEmpty() && hasFocus,
                onDismissRequest = { expanded = false }
            ) {
                filteredStates.forEach { state ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${state.name}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            onValueChange(state.name)
                            onStateSelected(state)
                            expanded = false
                            focusManager.moveFocus(FocusDirection.Next)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StateAutocompletePreview() {
    val sampleStates = listOf(
        State(id = "1", name = "Andhra Pradesh"),
        State(id = "2", name = "Karnataka"),
        State(id = "3", name = "Kerala"),
        State(id = "4", name = "Tamil Nadu")
    )

    var selectedState by remember { mutableStateOf("") }

    StateAutocomplete(
        value = selectedState,
        onValueChange = { selectedState = it },
        onStateSelected = { state -> selectedState = state.name },
        states = sampleStates,
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    )
}