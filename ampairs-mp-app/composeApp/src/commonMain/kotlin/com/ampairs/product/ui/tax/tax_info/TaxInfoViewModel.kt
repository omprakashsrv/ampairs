package com.ampairs.product.ui.tax.tax_info

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.id_generator.IdUtils
import com.ampairs.product.db.TaxRepository
import com.ampairs.product.domain.Constants
import com.ampairs.product.domain.TaxInfo
import com.ampairs.product.domain.asTaxInfoDomainModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaxInfoViewModel(val id: String?, private val taxRepository: TaxRepository) : ViewModel() {

    var loading by mutableStateOf(false)
    var taxInfoState by mutableStateOf(TaxInfoState(TaxInfo()))
    
    init {
        // Load data asynchronously
        viewModelScope.launch(DispatcherProvider.io) {
            val taxInfo = id?.let { taxRepository.getTaxInfo(it)?.asTaxInfoDomainModel() } ?: TaxInfo()
            
            viewModelScope.launch(Dispatchers.Main) {
                taxInfoState = TaxInfoState(taxInfo)
            }
        }
    }

    fun reSyncTaxInfo(id: String) {
        viewModelScope.launch(DispatcherProvider.io) {
            val taxInfo = taxRepository.getTaxInfo(id)?.asTaxInfoDomainModel() ?: TaxInfo()
            
            viewModelScope.launch(Dispatchers.Main) {
                taxInfoState = TaxInfoState(taxInfo)
            }
        }
    }

    fun updateTaxInfo(): String {
        loading = true
        val taxInfoToUpdate = taxInfoState.toDomainModel()
        if (taxInfoToUpdate.id.isEmpty()) {
            taxInfoToUpdate.id = IdUtils.generateUniqueId(
                Constants.TAX_INFO_PREFIX,
                Constants.ID_LENGTH
            )
        }
        viewModelScope.launch(DispatcherProvider.io) {
            taxRepository.updateTaxInfo(taxInfoToUpdate)
            viewModelScope.launch(Dispatchers.Main) {
                loading = false
            }
        }
        return taxInfoToUpdate.id
    }
}