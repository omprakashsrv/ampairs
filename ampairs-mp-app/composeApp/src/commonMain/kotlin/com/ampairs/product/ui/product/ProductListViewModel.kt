package com.ampairs.product.ui.product

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.map
import com.ampairs.common.flower_core.Resource
import com.ampairs.common.model.UiState
import com.ampairs.product.domain.asDomainModel
import com.ampairs.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


class ProductListViewModel(private val productRepository: ProductRepository) : ViewModel() {

    var searchText by mutableStateOf("")
    val productsState = mutableStateOf<UiState<Boolean>>(UiState.Empty)

    init {
        syncProducts()
    }

    private fun syncProducts() {
        viewModelScope.launch(Dispatchers.IO) {
            productRepository.getProductResource().collect { response ->
                viewModelScope.launch(Dispatchers.Main) {
                    when (response.status) {
                        is Resource.Status.Loading -> {
                            productsState.value = UiState.Loading(false)
                        }

                        is Resource.Status.Success -> {
                            productsState.value = UiState.Success(true)
                        }

                        is Resource.Status.EmptySuccess -> {
                            productsState.value = UiState.Empty
                        }

                        is Resource.Status.Error -> {
                            val status = response.status
                            productsState.value = UiState.Error(status.errorMessage)
                        }
                    }
                }
            }
        }
    }

    val products = Pager(config = PagingConfig(
        pageSize = PAGE_SIZE,
        prefetchDistance = 10,
        initialLoadSize = PAGE_SIZE,
    ), pagingSourceFactory = {
        productRepository.getProductPaging(searchText)
    }).flow.map { pagingData -> pagingData.map { it.asDomainModel() } }
        .cachedIn(viewModelScope)

}