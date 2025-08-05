package com.ampairs.product.ui.product

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.ampairs.aws.s3.S3Client
import com.ampairs.common.flower_core.Resource
import com.ampairs.common.model.UiState
import com.ampairs.inventory.db.InventoryRepository
import com.ampairs.product.domain.Group
import com.ampairs.product.domain.Product
import com.ampairs.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


val PAGE_SIZE = 40

class ProductCategoryViewModel(
    private val productRepository: ProductRepository,
    private val inventoryRepository: InventoryRepository,
    private val groupId: String?,
    val cartProducts: MutableList<Product>,
    val s3Client: S3Client,
) : ViewModel() {

    var group by mutableStateOf<Group?>(null)
    var selectedCategory by mutableStateOf<Group?>(null)
    var tabIndex by mutableStateOf(0)
    val productCategories = mutableStateOf<UiState<List<Group>>>(UiState.Empty)
    var onProductQtyChanged: (() -> Unit)? = null

    init {
        viewModelScope.launch {
            group = groupId?.let { productRepository.getGroup(it) }
        }
        if (!groupId.isNullOrEmpty()) {
            syncCategories()
        }
    }

    private fun syncCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            productRepository.getCategoryResource(groupId ?: "").collect { response ->
                viewModelScope.launch(Dispatchers.Main) {
                    when (response.status) {
                        is Resource.Status.Loading -> {
                            val status = response.status
                            val data = status.data
                            if (!data.isNullOrEmpty()) {
                                productCategories.value = UiState.Success(data)
                            } else {
                                productCategories.value = UiState.Loading(null)
                            }
                        }

                        is Resource.Status.Success -> {
                            val status = response.status
                            productCategories.value = UiState.Success(status.data!!)
                        }

                        // EmptySuccess is for potentially body-less successful HTTP responses like 201, 204
                        is Resource.Status.EmptySuccess -> {
                            productCategories.value = UiState.Empty
                        }

                        is Resource.Status.Error -> {
                            val status = response.status
//                            groups.value = UiState.Error(status.errorMessage)
                        }
                    }
                }
            }
        }
    }

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

    var loading by mutableStateOf(true)

    val products = Pager(config = PagingConfig(
        pageSize = PAGE_SIZE,
        prefetchDistance = 10,
        initialLoadSize = PAGE_SIZE,
    ), pagingSourceFactory = {
        productRepository.getProductPagingByGroupAndCategory(
            groupId ?: "",
            selectedCategory?.id ?: ""
        )
    }).flow.map { pagingData ->
        pagingData.toPagingProduct(
            cartProducts,
            productRepository,
            inventoryRepository,
            s3Client,
            onProductQtyChanged
        )
    }
        .cachedIn(viewModelScope)


}