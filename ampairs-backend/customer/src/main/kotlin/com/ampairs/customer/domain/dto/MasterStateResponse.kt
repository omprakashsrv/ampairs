package com.ampairs.customer.domain.dto

import com.ampairs.customer.domain.model.MasterState
import java.time.Instant

data class MasterStateResponse(
    val uid: String,
    val stateCode: String,
    val name: String,
    val shortName: String,
    val countryCode: String,
    val countryName: String,
    val region: String?,
    val timezone: String?,
    val localName: String?,
    val capital: String?,
    val population: Long?,
    val areaSqKm: Double?,
    val gstCode: String?,
    val postalCodePattern: String?,
    val active: Boolean,
    val metadata: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?
)

fun MasterState.asMasterStateResponse(): MasterStateResponse {
    return MasterStateResponse(
        uid = this.uid,
        stateCode = this.stateCode,
        name = this.name,
        shortName = this.shortName,
        countryCode = this.countryCode,
        countryName = this.countryName,
        region = this.region,
        timezone = this.timezone,
        localName = this.localName,
        capital = this.capital,
        population = this.population,
        areaSqKm = this.areaSqKm,
        gstCode = this.gstCode,
        postalCodePattern = this.postalCodePattern,
        active = this.active,
        metadata = this.metadata,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

fun List<MasterState>.asMasterStateResponses(): List<MasterStateResponse> {
    return map { it.asMasterStateResponse() }
}