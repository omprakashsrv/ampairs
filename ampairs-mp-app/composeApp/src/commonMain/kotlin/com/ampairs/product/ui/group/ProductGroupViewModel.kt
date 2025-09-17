package com.ampairs.product.ui.group

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.flower_core.Resource
import com.ampairs.common.model.UiState
import com.ampairs.product.domain.Group
import com.ampairs.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ProductGroupViewModel(
    private val productRepository: ProductRepository,
    private val groupType: GroupType = GroupType.GROUP,
) : ViewModel() {

    val groups = mutableStateOf<UiState<List<Group>>>(UiState.Empty)

    init {
        syncGroups()
    }

    private fun syncGroups() {
        viewModelScope.launch(DispatcherProvider.io) {
            productRepository.getGroupResource(groupType).collect { response ->
                viewModelScope.launch(Dispatchers.Main) {
                    when (response.status) {
                        is Resource.Status.Loading -> {
                            val status = response.status
                            val data = status.data?.filter { it.active }
                            if (!data.isNullOrEmpty()) {
                                groups.value = UiState.Success(data)
                            } else {
                                groups.value = UiState.Loading(null)
                            }
                        }

                        is Resource.Status.Success -> {
                            val status = response.status
                            groups.value = UiState.Success(status.data!!.filter { it.active })
                        }

                        // EmptySuccess is for potentially body-less successful HTTP responses like 201, 204
                        is Resource.Status.EmptySuccess -> {
                            groups.value = UiState.Empty
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


}