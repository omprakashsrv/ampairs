package com.ampairs.customer.domain

import com.ampairs.customer.api.model.CustomerApiModel
import com.ampairs.customer.db.entity.CustomerEntity
import com.ampairs.domain.Location
import com.ampairs.order.domain.Address
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class Customer(
    val seqId: Long = 0,
    var id: String = "",
    var name: String = "",
    var gstin: String? = "",
    var email: String? = "",
    var address: String? = "",
    var pincode: String? = "",
    var state: String? = "",
    var countryCode: Int = 91,
    var phone: String? = "",
    var active: Boolean = true,
    var softDeleted: Boolean = false,
    var landline: String? = "",
    var companyId: String? = "",
    var location: Location? = null,
    var street: String? = "",
    var street2: String? = "",
    var city: String? = "",
    var country: String? = "India",
    var billingAddress: Address = Address(),
    var shippingAddress: Address = Address(),
    var billingSameAsRegistered: Boolean? = true,
    var shippingSameAsBilling: Boolean? = true,
)

fun List<CustomerApiModel>.asDatabaseModel(): List<CustomerEntity> {
    return map {
        CustomerEntity(
            seq_id = 0,
            id = it.id,
            name = it.name,
            company_id = it.companyId ?: "",
            gstin = it.gstin ?: "",
            address = it.address ?: "",
            country_code = it.countryCode.toLong(),
            phone = it.phone ?: "",
            landline = it.landline ?: "",
            pincode = it.pincode ?: "",
            state = it.state ?: "",
            latitude = it.latitude ?: 0.0,
            longitude = it.longitude ?: 0.0,
            email = it.email ?: "",
            last_updated = it.lastUpdated,
            created_at = it.createdAt ?: "",
            updated_at = it.updatedAt ?: "",
            active = if (it.active != false) 1 else 0,
            soft_deleted = if (it.softDeleted == true) 1 else 0,
            street = it.street ?: "",
            street2 = it.street2 ?: "",
            city = it.city ?: "",
            billing_address = if (it.billingAddress != null) Json.encodeToString(it.billingAddress) else null,
            shipping_address = if (it.shippingAddress != null) Json.encodeToString(it.shippingAddress) else null,
            country = it.country ?: "India",
            billing_same_as_registered = if (it.billingSameAsRegistered != false) 1L else 0,
            shipping_same_as_billing = if (it.shippingSameAsBilling != false) 1L else 0,
            synced = 1
        )
    }
}

fun CustomerEntity.asDomainModel(): Customer {
    return Customer(
        seqId = this.seq_id,
        id = this.id,
        name = this.name,
        email = this.email,
        gstin = this.gstin,
        address = this.address,
        pincode = this.pincode,
        state = this.state,
        location = Location(latitude, longitude),
        countryCode = this.country_code.toInt(),
        phone = this.phone,
        landline = this.landline
    )
}

@OptIn(ExperimentalTime::class)
fun Customer.asDatabaseModel(): CustomerEntity {
    return CustomerEntity(
        seq_id = this.seqId,
        id = this.id,
        name = this.name,
        email = this.email ?: "",
        gstin = this.gstin ?: "",
        address = this.address ?: "",
        pincode = this.pincode ?: "",
        state = this.state ?: "",
        latitude = this.location?.latitude ?: 0.0,
        longitude = this.location?.longitude ?: 0.0,
        company_id = this.companyId ?: "",
        phone = this.phone ?: "",
        landline = this.landline ?: "",
        country_code = this.countryCode.toLong(),
        created_at = "",
        updated_at = "",
        last_updated = Clock.System.now().toEpochMilliseconds(),
        active = if (this.active) 1 else 0,
        soft_deleted = if (this.softDeleted) 1 else 0,
        street = this.street ?: "",
        street2 = this.street2 ?: "",
        city = this.city ?: "",
        billing_address = Json.encodeToString(this.billingAddress),
        shipping_address = Json.encodeToString(this.shippingAddress),
        country = this.country ?: "India",
        synced = 0,
        billing_same_as_registered = if (billingSameAsRegistered != false) 1L else 0,
        shipping_same_as_billing = if (shippingSameAsBilling != false) 1L else 0,
    )
}

fun CustomerEntity.asApiModel(): CustomerApiModel {
    return CustomerApiModel(
        id = this.id,
        name = this.name,
        email = this.email,
        gstin = this.gstin,
        address = this.address,
        pincode = this.pincode,
        state = this.state,
        latitude = this.latitude,
        longitude = this.longitude,
        phone = this.phone,
        landline = this.landline,
        countryCode = this.country_code.toInt(),
        active = this.active == 1L,
        softDeleted = this.soft_deleted == 1L,
        street = this.street,
        street2 = this.street2,
        city = this.city,
        billingAddress = this.billing_address?.let { Json.decodeFromString(it) },
        shippingAddress = this.shipping_address?.let { Json.decodeFromString(it) },
        country = this.country,
    )
}

fun List<CustomerEntity>.asApiModel(): List<CustomerApiModel> {
    return map {
        it.asApiModel()
    }
}