package com.ampairs.workspace.model.dto

import com.ampairs.workspace.model.Workspace
import org.springframework.data.geo.Point


class WorkspaceRequest(
    var id: String?,
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

fun List<Workspace>.toWorkspaceRequest(): List<WorkspaceRequest> {
    return map {
        it.toWorkspaceRequest()
    }
}

fun Workspace.toWorkspaceRequest() = WorkspaceRequest(
    id = this.uid,
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


fun WorkspaceRequest.toWorkspace(): Workspace {
    val company = Workspace()
    company.uid = this.id ?: ""
    company.name = this.name
    company.countryCode = this.countryCode
    company.email = this.email ?: ""
    company.address = this.address ?: ""
    company.pincode = this.pincode ?: ""
    company.state = this.state ?: ""
    company.location = this.latitude?.let { this.longitude?.let { it1 -> Point(it, it1) } }
    company.phone = this.phone
    company.landline = this.landline
    company.gstin = this.gstin ?: ""
    return company
}