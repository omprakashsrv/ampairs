package com.ampairs.product.ui.tax.tax_info

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.ampairs.common.flower_core.Resource
import com.ampairs.common.model.UiState
import com.ampairs.customer.viewmodel.PAGE_SIZE
import com.ampairs.product.db.TaxRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaxInfosViewModel(private val taxRepository: TaxRepository) : ViewModel() {

    var searchText by mutableStateOf("")
    val taxInfoState = mutableStateOf<UiState<Boolean>>(UiState.Empty)

    init {
        syncTaxInfos()
    }

    private fun syncTaxInfos() {
        viewModelScope.launch(Dispatchers.IO) {
            taxRepository.getTaxInfoResource().collect { response ->
                viewModelScope.launch(Dispatchers.Main) {
                    when (response.status) {
                        is Resource.Status.Loading -> {
                            taxInfoState.value = UiState.Loading(false)
                        }

                        is Resource.Status.Success -> {
                            taxInfoState.value = UiState.Success(true)
                        }

                        is Resource.Status.EmptySuccess -> {
                            taxInfoState.value = UiState.Empty
                        }

                        is Resource.Status.Error -> {
                            val status = response.status
                            taxInfoState.value = UiState.Error(status.errorMessage)
                        }
                    }
                }
            }
        }
    }

    val taxInfos = Pager(config = PagingConfig(
        pageSize = PAGE_SIZE,
        prefetchDistance = 10,
        initialLoadSize = PAGE_SIZE,
    ), pagingSourceFactory = {
        taxRepository.getTaxInfoPaging(searchText)
    }).flow
        .cachedIn(viewModelScope)
}