package com.ampairs.customer.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.id_generator.IdUtils
import com.ampairs.customer.db.CustomerRepository
import com.ampairs.customer.domain.Constants
import com.ampairs.customer.domain.Customer
import com.ampairs.customer.domain.asDomainModel
import com.ampairs.customer.ui.CustomerState
import com.ampairs.customer.ui.toDomainModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CustomerViewModel(val id: String?, private val customerRepository: CustomerRepository) :
    ViewModel() {

    var loading by mutableStateOf(false)
    var customer by mutableStateOf(CustomerState(Customer()))

    init {
        id?.let { loadCustomer(it) }
    }

    private fun loadCustomer(customerId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val customerEntity = customerRepository.getCustomer(customerId)
            customer = CustomerState(customerEntity?.asDomainModel() ?: Customer())
        }
    }

    fun reSyncCustomer(id: String) {
        loadCustomer(id)
    }

    fun updateCustomer(): String {
        loading = true
        val customerToUpdate = customer.toDomainModel()
        if (customerToUpdate.id.isEmpty()) {
            customerToUpdate.id = IdUtils.generateUniqueId(
                Constants.CUSTOMER_PREFIX,
                Constants.ID_LENGTH
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            customerRepository.updateCustomer(customerToUpdate)
            viewModelScope.launch(Dispatchers.Main) {
                loading = false
            }
        }
        return customerToUpdate.id
    }

}