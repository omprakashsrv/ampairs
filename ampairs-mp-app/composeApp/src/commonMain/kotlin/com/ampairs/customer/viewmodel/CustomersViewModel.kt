package com.ampairs.customer.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.map
import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.flower_core.Resource
import com.ampairs.common.model.UiState
import com.ampairs.customer.db.CustomerRepository
import com.ampairs.customer.domain.Customer
import com.ampairs.customer.domain.asDomainModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val PAGE_SIZE = 20

class CustomersViewModel(
    private val customerRepository: CustomerRepository,
    private val tokenRepository: TokenRepository
) : ViewModel() {

    var searchText by mutableStateOf("")
    var company: Customer? = null
    val customersState = mutableStateOf<UiState<Boolean>>(UiState.Empty)


    init {
        loadDefaultCustomer()
        syncCustomers()
    }

    private fun loadDefaultCustomer() {
        viewModelScope.launch(Dispatchers.IO) {
//            val workspaceId = tokenRepository.getWorkspaceId()
            val workspaceId = ""
            company = customerRepository.getDefaultCustomer(workspaceId)
                ?.asDomainModel()
        }
    }

    private fun syncCustomers() {
        viewModelScope.launch(Dispatchers.IO) {
            customerRepository.getCustomerResource().collect { response ->
                viewModelScope.launch(Dispatchers.Main) {
                    when (response.status) {
                        is Resource.Status.Loading -> {
                            customersState.value = UiState.Loading(false)
                        }

                        is Resource.Status.Success -> {
                            customersState.value = UiState.Success(true)
                        }

                        is Resource.Status.EmptySuccess -> {
                            customersState.value = UiState.Empty
                        }

                        is Resource.Status.Error -> {
                            val status = response.status
                            customersState.value = UiState.Error(status.errorMessage)
                        }
                    }
                }
                if (company == null) {
                    loadDefaultCustomer()
                }
            }
        }
    }

    val customers = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            prefetchDistance = 10,
            initialLoadSize = PAGE_SIZE,
        ), pagingSourceFactory = {
            customerRepository.getCustomerPaging(searchText)
        }).flow.map { pagingData -> pagingData.map { it.asDomainModel() } }
        .cachedIn(viewModelScope)

}