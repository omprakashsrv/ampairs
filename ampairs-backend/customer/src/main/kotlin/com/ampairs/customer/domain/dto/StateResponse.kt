package com.ampairs.customer.domain.dto

import com.ampairs.customer.domain.model.State

data class StateResponse(
    var id: String,
    var name: String,
    var gstCode: Int,
    var active: Boolean,
)

fun List<State>.asStatesResponse(): List<StateResponse> {
    return map {
        StateResponse(
            id = it.uid,
            name = it.name,
            gstCode = it.gstCode,
            active = it.active
        )
    }
}