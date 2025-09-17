package com.ampairs.product.ui.product

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.id_generator.IdUtils
import com.ampairs.product.db.TaxRepository
import com.ampairs.product.domain.Constants
import com.ampairs.product.domain.Group
import com.ampairs.product.domain.Product
import com.ampairs.product.domain.TaxCode
import com.ampairs.product.domain.asDomainModel
import com.ampairs.product.ui.group.GroupType
import com.ampairs.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProductEditViewModel(
    val id: String?,
    val productRepository: ProductRepository,
    val taxRepository: TaxRepository,
) : ViewModel() {

    var loading by mutableStateOf(false)
    var productState by mutableStateOf(ProductState(Product()))

    var taxCodes by mutableStateOf<List<TaxCode>>(emptyList())
    var groups by mutableStateOf<List<Group>>(emptyList())
    var categories by mutableStateOf<List<Group>>(emptyList())
    var subCategories by mutableStateOf<List<Group>>(emptyList())
    var brands by mutableStateOf<List<Group>>(emptyList())

    init {
        viewModelScope.launch {
            // Load product if id is provided
            if (id != null) {
                productState = ProductState(productRepository.getProduct(id) ?: Product())
            }
            
            // Load all data asynchronously
            taxCodes = taxRepository.getTaxCodes().asDomainModel()
            groups = productRepository.getGroups(GroupType.GROUP)
            categories = productRepository.getGroups(GroupType.CATEGORY)
            subCategories = productRepository.getGroups(GroupType.SUBCATEGORY)
            brands = productRepository.getGroups(GroupType.BRAND)
            
            // Update selections after all data is loaded
            updateSelections()
        }
    }
    
    private fun updateSelections() {
        val selectedTaxCodeIndex = taxCodes.indexOfFirst { it.code == productState.product.taxCode }
        val selectedGroupIndex = groups.indexOfFirst { it.id == productState.product.groupId }
        val selectedCategoryIndex = categories.indexOfFirst { it.id == productState.product.categoryId }
        val selectedSubCategoryIndex = subCategories.indexOfFirst { it.id == productState.product.subCategoryId }
        val selectedBrandIndex = brands.indexOfFirst { it.id == productState.product.brandId }

        productState.taxCode =
            if (selectedTaxCodeIndex != -1) taxCodes[selectedTaxCodeIndex] else null
        productState.group = if (selectedGroupIndex != -1) groups[selectedGroupIndex] else null
        productState.category =
            if (selectedCategoryIndex != -1) categories[selectedCategoryIndex] else null
        productState.subCategory =
            if (selectedSubCategoryIndex != -1) subCategories[selectedSubCategoryIndex] else null
        productState.brand = if (selectedBrandIndex != -1) brands[selectedBrandIndex] else null
    }

    fun updateProduct(): String {
        loading = true
        val productToUpdate = productState.toDomainModel()
        if (productToUpdate.id.isEmpty()) {
            productToUpdate.id = IdUtils.generateUniqueId(
                Constants.PRODUCT_PREFIX,
                Constants.ID_LENGTH
            )
        }
        viewModelScope.launch(DispatcherProvider.io) {
            productRepository.updateProduct(productToUpdate)
            viewModelScope.launch(Dispatchers.Main) {
                loading = false
            }
        }
        return productToUpdate.id
    }

    fun reSyncProductState(id: String) {
        viewModelScope.launch {
            productState = ProductState(productRepository.getProduct(id) ?: Product())
            updateSelections()
        }
    }
}