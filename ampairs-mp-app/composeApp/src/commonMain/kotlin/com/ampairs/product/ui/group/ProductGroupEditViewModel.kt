package com.ampairs.product.ui.group

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.flower_core.Resource
import com.ampairs.common.id_generator.IdUtils
import com.ampairs.common.model.UiState
import com.ampairs.product.db.Constants
import com.ampairs.product.domain.Group
import com.ampairs.repository.ProductRepository
import com.darkrockstudios.libraries.mpfilepicker.MPFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ProductGroupEditViewModel(
    private val productRepository: ProductRepository,
    private val groupType: GroupType = GroupType.GROUP,
) : ViewModel() {

    var groups: MutableList<GroupState> = arrayListOf()
    val groupsState = mutableStateOf<UiState<List<GroupState>>>(UiState.Loading(null))
    var showFilePicker by mutableStateOf(false)
    var group: GroupState? = null

    init {
        syncGroups()
    }

    fun syncGroups() {
        viewModelScope.launch(Dispatchers.IO) {
            productRepository.getGroupResource(groupType).collect { response ->
                viewModelScope.launch(Dispatchers.Main) {
                    when (response.status) {
                        is Resource.Status.Loading -> {
                            val status = response.status
                            val data = status.data
                            if (!data.isNullOrEmpty()) {
                                groups = data.toGroupState().toMutableList()
                                addGroup()
                            } else {
                                groupsState.value = UiState.Loading(null)
                            }
                        }

                        is Resource.Status.Success -> {
                            val status = response.status
                            groups = status.data?.toGroupState()!!.toMutableList()
                            addGroup()
                        }

                        // EmptySuccess is for potentially body-less successful HTTP responses like 201, 204
                        is Resource.Status.EmptySuccess -> {
                            groupsState.value = UiState.Empty
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

    fun uploadImage(files: MPFile<Any>?) {
        if (files != null) {
            viewModelScope.launch(Dispatchers.IO) {
                group?.uploading = true
                val file = files.getFileByteArray()
                val uploadedImage =
                    productRepository.uploadImage(files.path, file, "/" + groupType.name)
                group?.uploading = false
                group?.changed = true
                viewModelScope.launch(Dispatchers.Main) {
                    group?.image = uploadedImage
                }

            }
        }
    }

    suspend fun saveGroups() {
        val groups = groups.filter { it.changed }
        if (groups.isNotEmpty()) {
            groups.forEach {
                it.group.name = it.name
                it.group.active = it.active
                it.group.image = it.image
            }
            productRepository.saveGroups(groups.map { it.group }, groupType)
            viewModelScope.launch(Dispatchers.IO) {
                productRepository.updateGroups(groupType)
                syncGroups()
            }
        }
    }

    fun addGroup() {
        groups = groups.toMutableList()
        groups.add(
            GroupState(
                Group(
                    id = IdUtils.generateUniqueId(
                        when (groupType) {
                            GroupType.GROUP -> Constants.PRODUCT_GROUP_PREFIX
                            GroupType.CATEGORY -> Constants.PRODUCT_CATEGORY_PREFIX
                            GroupType.SUBCATEGORY -> Constants.PRODUCT_SUB_CATEGORY_PREFIX
                            GroupType.BRAND -> Constants.PRODUCT_BRAND_PREFIX
                        },
                        Constants.ID_LENGTH
                    )
                )
            )
        )
        groupsState.value = UiState.Success(groups)
    }

}