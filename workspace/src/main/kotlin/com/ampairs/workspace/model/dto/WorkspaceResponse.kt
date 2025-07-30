package com.ampairs.workspace.model.dto

import com.ampairs.workspace.model.Workspace


class WorkspaceResponse(
    var id: String,
    var countryCode: Int,
    var name: String,
    var phone: String,
    var landline: String,
    var email: String?,
    var gstin: String?,
    var address: String?,
    var pincode: String?,
    var state: String?,
    var latitude: Double?,
    var longitude: Double?,
    var lastUpdated: Long,
    var createdAt: String?,
    var updatedAt: String?,
)

fun List<Workspace>.toWorkspaceResponse(): List<WorkspaceResponse> {
    return map {
        it.toWorkspaceResponse()
    }
}

fun Workspace.toWorkspaceResponse() = WorkspaceResponse(
    id = this.id,
    name = this.name,
    countryCode = this.countryCode,
    phone = this.phone,
    email = this.email,
    gstin = this.gstin,
    address = this.address,
    pincode = this.pincode,
    state = this.state,
    latitude = this.location?.x,
    longitude = this.location?.y,
    lastUpdated = this.lastUpdated,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
    landline = this.landline
)