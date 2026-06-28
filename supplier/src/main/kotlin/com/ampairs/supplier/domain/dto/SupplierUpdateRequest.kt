package com.ampairs.supplier.domain.dto

import com.ampairs.core.domain.model.Address
import com.ampairs.core.validation.*
import com.ampairs.supplier.domain.model.Supplier
import jakarta.validation.constraints.*
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel

data class SupplierUpdateRequest(
    @field:SafeString(maxLength = 50, message = "ID contains invalid characters")
    @field:Size(max = 50, message = "ID cannot exceed 50 characters")
    var uid: String?,

    @field:SafeString(maxLength = 50, message = "Reference ID contains invalid characters")
    @field:Size(max = 50, message = "Reference ID cannot exceed 50 characters")
    var refId: String?,

    @field:NotNull(message = "Name is required")
    @field:NotBlank(message = "Name cannot be blank")
    @field:SafeString(maxLength = 100, message = "Name contains invalid characters")
    @field:Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    var name: String,

    @field:ValidGstin
    var gstin: String?,

    val countryCode: Int? = 91,

    @field:ValidPhone
    var phone: String?,

    @field:SafeString(maxLength = 15, message = "Landline contains invalid characters")
    @field:Pattern(regexp = "^[0-9\\-+()\\s]*$", message = "Invalid landline format")
    var landline: String?,

    @field:ValidEmail
    var email: String?,

    @field:ValidPincode
    var pincode: String?,

    var supplierType: String?,

    var supplierGroup: String?,

    var gstNumber: String?,

    var panNumber: String?,

    @field:DecimalMin(value = "0.0", message = "Credit limit must be non-negative")
    var creditLimit: Double?,

    @field:Min(value = 0, message = "Credit days must be non-negative")
    var creditDays: Int?,

    var status: String?,

    var attributes: Map<String, Any>?,

    @field:SafeString(maxLength = 500, message = "Address contains invalid characters")
    var address: String?,

    @field:SafeString(maxLength = 100, message = "State contains invalid characters")
    @field:Size(max = 100, message = "State cannot exceed 100 characters")
    var state: String?,

    @field:SafeString(maxLength = 200, message = "Street contains invalid characters")
    @field:Size(max = 200, message = "Street cannot exceed 200 characters")
    var street: String? = null,

    @field:SafeString(maxLength = 200, message = "Street2 contains invalid characters")
    @field:Size(max = 200, message = "Street2 cannot exceed 200 characters")
    var street2: String? = null,

    @field:SafeString(maxLength = 100, message = "City contains invalid characters")
    @field:Size(max = 100, message = "City cannot exceed 100 characters")
    var city: String? = null,

    @field:SafeString(maxLength = 100, message = "Country contains invalid characters")
    @field:Size(max = 100, message = "Country cannot exceed 100 characters")
    var country: String? = null,

    var billingAddress: Address? = Address(),
    var shippingAddress: Address? = Address(),

    @field:DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @field:DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    val latitude: Double?,

    @field:DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @field:DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    val longitude: Double?,

    val active: Boolean? = true,
    val softDeleted: Boolean? = false,
)

fun SupplierUpdateRequest.toSupplier(): Supplier {
    val supplier = Supplier()
    supplier.uid = this.uid ?: ""
    supplier.refId = this.refId ?: ""
    supplier.name = this.name
    supplier.countryCode = this.countryCode ?: 91
    supplier.phone = this.phone ?: ""
    supplier.landline = this.landline ?: ""
    supplier.email = this.email ?: ""
    supplier.pincode = this.pincode ?: ""
    supplier.supplierType = this.supplierType ?: "REGULAR"
    supplier.supplierGroup = this.supplierGroup ?: "REGULAR"
    supplier.gstNumber = this.gstNumber
    supplier.panNumber = this.panNumber
    supplier.creditLimit = this.creditLimit ?: 0.0
    supplier.creditDays = this.creditDays ?: 0
    supplier.address = this.address ?: ""
    supplier.state = this.state ?: ""
    supplier.street = this.street ?: ""
    supplier.street2 = this.street2 ?: ""
    supplier.city = this.city ?: ""
    supplier.country = this.country ?: ""
    supplier.billingAddress = this.billingAddress ?: Address()
    supplier.shippingAddress = this.shippingAddress ?: Address()
    supplier.location = if (this.latitude != null && this.longitude != null) {
        // JTS Point creation using GeometryFactory with SRID 4326 (WGS84 for GPS coordinates)
        val geometryFactory = GeometryFactory(PrecisionModel(), 4326)
        geometryFactory.createPoint(Coordinate(this.longitude, this.latitude))
    } else null
    supplier.status = this.status ?: "ACTIVE"
    supplier.attributes = this.attributes
    return supplier
}

fun List<SupplierUpdateRequest>.toSuppliers(): List<Supplier> {
    return map { it.toSupplier() }
}
