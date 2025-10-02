package com.ampairs.form.data.api

import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.model.Response
import com.ampairs.form.domain.EntityConfigSchema
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

/**
 * Implementation of ConfigApi using Ktor HTTP client
 */
class ConfigApiImpl(
    private val httpClient: HttpClient
) : ConfigApi {

    override suspend fun getConfigSchema(entityType: String): EntityConfigSchema {
        val response: Response<EntityConfigSchema> = httpClient.get(
            ApiUrlBuilder.formUrl("v1/schema")
        ) {
            parameter("entity_type", entityType)
        }.body()

        return response.data ?: throw Exception("Config schema not found for entity type: $entityType")
    }

    override suspend fun getAllConfigSchemas(): List<EntityConfigSchema> {
        val response: Response<List<EntityConfigSchema>> = httpClient.get(
            ApiUrlBuilder.formUrl("v1/schemas")
        ).body()

        return response.data ?: emptyList()
    }
}
