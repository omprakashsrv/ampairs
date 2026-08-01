package com.ampairs.pricing.domain.dto

import com.ampairs.pricing.domain.model.GeoZone
import com.ampairs.pricing.domain.model.GeoZoneMembers
import com.ampairs.pricing.util.PricingJson
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class GeoZoneRequest(
    val uid: String? = null,

    @field:NotBlank(message = "Geo-zone name is required")
    @field:Size(max = 200)
    val name: String,

    val members: GeoZoneMembers = GeoZoneMembers(),

    val active: Boolean = true,

    val refId: String? = null,
)

data class GeoZoneResponse(
    val uid: String,
    val refId: String?,
    val name: String,
    val members: GeoZoneMembers,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

fun GeoZone.applyRequest(request: GeoZoneRequest): GeoZone = apply {
    request.uid?.takeIf { it.isNotBlank() }?.let { uid = it }
    name = request.name.trim()
    membersJson = PricingJson.write(request.members)
    active = request.active
    request.refId?.takeIf { it.isNotBlank() }?.let { refId = it }
}

fun GeoZone.asResponse(): GeoZoneResponse = GeoZoneResponse(
    uid = uid,
    refId = refId,
    name = name,
    members = PricingJson.read(membersJson, GeoZoneMembers()),
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun List<GeoZone>.asResponses(): List<GeoZoneResponse> = map { it.asResponse() }
