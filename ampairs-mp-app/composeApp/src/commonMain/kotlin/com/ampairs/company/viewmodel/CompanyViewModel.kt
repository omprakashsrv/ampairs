package com.ampairs.company.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.flower_core.Resource
import com.ampairs.common.id_generator.IdUtils
import com.ampairs.common.model.UiState
import com.ampairs.company.db.CompanyRepository
import com.ampairs.company.domain.Company
import com.ampairs.company.domain.Constants
import com.ampairs.company.domain.asDomainModel
import com.ampairs.company.ui.CompanyState
import com.ampairs.company.ui.toDomainModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CompanyViewModel(val id: String?, private val companyRepository: CompanyRepository) :
    ViewModel() {

    val companyState = mutableStateOf<UiState<Boolean>>(UiState.Empty)
    var company: CompanyState = CompanyState(Company())

    init {
        id?.let { loadCompany(it) }
    }

    private fun loadCompany(companyId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val companyEntity = companyRepository.getCompany(companyId)
            company = CompanyState(companyEntity?.asDomainModel() ?: Company())
        }
    }

    fun reSyncCompany(id: String) {
        loadCompany(id)
    }

    fun updateCompany(): String {
        val companyToUpdate = company.toDomainModel()
        if (companyToUpdate.id.isEmpty()) {
            companyToUpdate.id = IdUtils.generateUniqueId(
                Constants.COMPANY_PREFIX,
                Constants.ID_LENGTH
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            companyRepository.updateCompany(companyToUpdate).collect { response ->
                when (response.status) {
                    is Resource.Status.Loading -> {
                        companyState.value = UiState.Loading(null)
                    }

                    is Resource.Status.Success -> {
                        companyState.value = UiState.Success(true)
                    }

                    is Resource.Status.EmptySuccess -> {
                        companyState.value = UiState.Empty
                    }

                    is Resource.Status.Error -> {
                        val status = response.status
                        companyState.value = UiState.Error(status.errorMessage)
                    }
                }
            }
        }
        return companyToUpdate.id
    }

}