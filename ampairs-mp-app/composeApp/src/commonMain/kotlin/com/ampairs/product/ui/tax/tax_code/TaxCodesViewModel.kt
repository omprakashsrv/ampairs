package com.ampairs.product.ui.tax.tax_code

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.map
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.flower_core.Resource
import com.ampairs.common.model.UiState
import com.ampairs.product.domain.Constants.Companion.PAGE_SIZE
import com.ampairs.product.db.TaxRepository
import com.ampairs.product.domain.asDomainModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TaxCodesViewModel(private val taxRepository: TaxRepository) : ViewModel() {

    val taxCodesState = mutableStateOf<UiState<Boolean>>(UiState.Empty)
    var searchText by mutableStateOf("")

    init {
        syncTaxCodes()
    }

    private fun syncTaxCodes() {
        viewModelScope.launch(DispatcherProvider.io) {
            taxRepository.getTaxCodeResource().collect { response ->
                viewModelScope.launch(Dispatchers.Main) {
                    when (response.status) {
                        is Resource.Status.Loading -> {
                            taxCodesState.value = UiState.Loading(null)
                        }

                        is Resource.Status.Success -> {
                            val status = response.status
                            taxCodesState.value = UiState.Success(true)
                        }

                        is Resource.Status.EmptySuccess -> {
                            taxCodesState.value = UiState.Empty
                        }

                        is Resource.Status.Error -> {
                            val status = response.status
                            taxCodesState.value = UiState.Error(status.errorMessage)
                        }
                    }
                }
            }
        }
    }

    val taxCodes = Pager(config = PagingConfig(
        pageSize = PAGE_SIZE,
        prefetchDistance = 10,
        initialLoadSize = PAGE_SIZE,
    ), pagingSourceFactory = {
        taxRepository.getTaxCodePaging(searchText)
    }).flow.map { pagingData -> pagingData.map { it.asDomainModel() } }
        .cachedIn(viewModelScope)

}