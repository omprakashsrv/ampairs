package com.ampairs.product.ui.group

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ampairs.product.domain.Group

class GroupState(
    val group: Group,
) {
    var name by mutableStateOf(group.name)
    var active by mutableStateOf(group.active)
    var image by mutableStateOf(group.image)
    var index by mutableStateOf(group.index)
    var uploading by mutableStateOf(false)
    var changed = false
        set(value) {
            field = value && name.trim().isNotEmpty()
        }
}

fun List<Group>.toGroupState(): List<GroupState> {
    return map {
        GroupState(it)
    }
}