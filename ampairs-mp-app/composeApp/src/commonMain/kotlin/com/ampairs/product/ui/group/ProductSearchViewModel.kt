package com.ampairs.product.ui.group

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.ampairs.aws.s3.S3Client
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.flower_core.Resource
import com.ampairs.common.model.UiState
import com.ampairs.product.domain.Product
import com.ampairs.product.ui.product.PAGE_SIZE
import com.ampairs.product.ui.product.toPagingProduct
import com.ampairs.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ProductSearchViewModel(
    val cartProducts: MutableList<Product>,
    val productRepository: ProductRepository,
    val s3Client: S3Client,
) :
    ViewModel() {

    init {
        syncProducts()
    }

    var onProductQtyChanged: (() -> Unit)? = null
    var searchText by mutableStateOf("")
    var searchActive by mutableStateOf(false)
    val productsState = mutableStateOf<UiState<Boolean>>(UiState.Empty)

    val products = Pager(config = PagingConfig(
        pageSize = PAGE_SIZE,
        prefetchDistance = 10,
        initialLoadSize = PAGE_SIZE,
    ), pagingSourceFactory = {
        productRepository.getProductPagingByName(searchText)
    }).flow.map { pagingData ->
        pagingData.toPagingProduct(
            cartProducts,
            productRepository,
            s3Client,
            onProductQtyChanged
        )
    }
        .cachedIn(viewModelScope)


    fun addCartItem(product: Product) {
        val findProduct = cartProducts.find { it.id == product.id }
        if (findProduct == null) {
            cartProducts.add(product)
        } else {
            findProduct.quantity = product.quantity
        }
    }

    fun removeCartItem(product: Product) {
        cartProducts.removeAll {
            it.id == product.id
        }
    }

    fun setQtyChangeListener(onProductQtyChanged: () -> Unit) {
        this.onProductQtyChanged = onProductQtyChanged
    }

    private fun syncProducts() {
        viewModelScope.launch(DispatcherProvider.io) {
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

}