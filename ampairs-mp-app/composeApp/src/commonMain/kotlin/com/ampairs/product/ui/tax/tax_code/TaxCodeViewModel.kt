package com.ampairs.product.ui.tax.tax_code

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.id_generator.IdUtils
import com.ampairs.product.db.TaxRepository
import com.ampairs.product.domain.Constants
import com.ampairs.product.domain.TaxCode
import com.ampairs.product.domain.TaxInfo
import com.ampairs.product.domain.asDomainModel
import com.ampairs.product.domain.asTaxInfoDomainModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaxCodeViewModel(val id: String?, private val taxRepository: TaxRepository) : ViewModel() {

    var loading by mutableStateOf(false)
    var taxCodeState by mutableStateOf(TaxCodeState(TaxCode()))
    var taxInfoList by mutableStateOf<List<TaxInfo>>(emptyList())
    
    init {
        // Load data asynchronously
        viewModelScope.launch(DispatcherProvider.io) {
            val taxCode = id?.let { taxRepository.getTaxCode(it)?.asDomainModel() } ?: TaxCode()
            val taxInfos = taxRepository.getTaxInfos().asTaxInfoDomainModel()
            
            viewModelScope.launch(Dispatchers.Main) {
                taxCodeState = TaxCodeState(taxCode)
                taxInfoList = taxInfos
            }
        }
    }

    fun reSyncTaxInfo(id: String) {
        viewModelScope.launch(DispatcherProvider.io) {
            val taxCode = taxRepository.getTaxCode(id)?.asDomainModel() ?: TaxCode()
            
            viewModelScope.launch(Dispatchers.Main) {
                taxCodeState = TaxCodeState(taxCode)
            }
        }
    }

    fun updateTaxInfo(): String {
        loading = true
        val taxCodeToUpdate = taxCodeState.toDomainModel()
        if (taxCodeToUpdate.id.isEmpty()) {
            taxCodeToUpdate.id = IdUtils.generateUniqueId(
                Constants.TAX_INFO_PREFIX,
                Constants.ID_LENGTH
            )
        }
        viewModelScope.launch(DispatcherProvider.io) {
            taxRepository.updateTaxCode(taxCodeToUpdate)
            viewModelScope.launch(Dispatchers.Main) {
                loading = false
            }
        }
        return taxCodeToUpdate.id
    }
}