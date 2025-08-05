package com.ampairs.company.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.map
import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.flower_core.Resource
import com.ampairs.common.model.UiState
import com.ampairs.company.db.CompanyRepository
import com.ampairs.company.domain.Company
import com.ampairs.company.domain.asDomainModel
import com.ampairs.customer.viewmodel.PAGE_SIZE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class CompanyListViewModel(
    private val companyRepository: CompanyRepository,
    private val tokenRepository: TokenRepository
) : ViewModel() {

    init {
        getCompanies()
    }

    val companyState = mutableStateOf<UiState<Boolean>>(UiState.Empty)

    fun getCompanies() {
        viewModelScope.launch(Dispatchers.IO) {
            companyRepository.getCompanyResource().collect { response ->
                viewModelScope.launch(Dispatchers.Main) {
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
        }
    }

    fun selectCompany(company: Company?) {
        tokenRepository.setCompanyId(company?.id ?: "")
    }


    val companies = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            prefetchDistance = 10,
            initialLoadSize = PAGE_SIZE,
        ), pagingSourceFactory = {
            companyRepository.getCompanyPaging()
        }).flow.map { pagingData -> pagingData.map { it.asDomainModel() } }
        .cachedIn(viewModelScope)

}