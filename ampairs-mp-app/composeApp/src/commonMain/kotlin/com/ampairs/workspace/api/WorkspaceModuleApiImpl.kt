package com.ampairs.workspace.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.delete
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.post
import com.ampairs.common.put
import com.ampairs.network.model.Response
import com.ampairs.workspace.api.model.WorkspaceModuleApiModel
import io.ktor.client.engine.HttpClientEngine

const val WORKSPACE_MODULE_ENDPOINT = "http://localhost:8080"

/**
 * Implementation of WorkspaceModuleApi using Ktor HTTP Client
 */
class WorkspaceModuleApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository
) : WorkspaceModuleApi {

    private val client = httpClient(engine, tokenRepository)

    companion object {
        private const val BASE_URL = "/workspace/v1/modules"
        private const val MASTER_MODULES_URL = "/workspace/v1/master-modules"
    }

    override suspend fun getModuleOverview(): Result<WorkspaceModuleApiModel.ModuleOverviewResponse> {
        return try {
            val response: Response<WorkspaceModuleApiModel.ModuleOverviewResponse> = get(
                client,
                WORKSPACE_MODULE_ENDPOINT + BASE_URL
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception(response.error?.message ?: "Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getModule(moduleId: String): Result<WorkspaceModuleApiModel.ModuleDetailResponse> {
        return try {
            val response: Response<WorkspaceModuleApiModel.ModuleDetailResponse> = get(
                client,
                WORKSPACE_MODULE_ENDPOINT + "$BASE_URL/$moduleId"
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception(response.error?.message ?: "Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchInstalledModules(
        query: String?,
        category: String?,
        status: String?,
        enabled: Boolean?,
        featured: Boolean?,
        sortBy: String?,
        sortDirection: String?,
        page: Int,
        size: Int,
    ): Result<WorkspaceModuleApiModel.ModuleSearchResponse> {
        return try {
            val parameters = buildMap<String, Any> {
                query?.let { put("query", it) }
                category?.let { put("category", it) }
                status?.let { put("status", it) }
                enabled?.let { put("enabled", it) }
                featured?.let { put("featured", it) }
                sortBy?.let { put("sort_by", it) }
                sortDirection?.let { put("sort_direction", it) }
                put("page", page)
                put("size", size)
            }
            val response: Response<WorkspaceModuleApiModel.ModuleSearchResponse> = get(
                client,
                WORKSPACE_MODULE_ENDPOINT + "$BASE_URL/search",
                parameters
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception(response.error?.message ?: "Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAvailableModules(
        businessType: String?,
    ): Result<WorkspaceModuleApiModel.AvailableModulesResponse> {
        return try {
            val parameters = buildMap<String, Any> {
                businessType?.let { put("business_type", it) }
            }
            val response: Response<WorkspaceModuleApiModel.AvailableModulesResponse> = get(
                client,
                WORKSPACE_MODULE_ENDPOINT + "$MASTER_MODULES_URL/available",
                if (parameters.isNotEmpty()) parameters else null
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception(response.error?.message ?: "Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchMasterModules(
        query: String?,
        category: String?,
        featured: Boolean?,
        sortBy: String?,
        sortDirection: String?,
        page: Int,
        size: Int,
    ): Result<WorkspaceModuleApiModel.MasterModuleSearchResponse> {
        return try {
            val parameters = buildMap<String, Any> {
                query?.let { put("query", it) }
                category?.let { put("category", it) }
                featured?.let { put("featured", it) }
                sortBy?.let { put("sort_by", it) }
                sortDirection?.let { put("sort_direction", it) }
                put("page", page)
                put("size", size)
            }
            val response: Response<WorkspaceModuleApiModel.MasterModuleSearchResponse> = get(
                client,
                WORKSPACE_MODULE_ENDPOINT + "$MASTER_MODULES_URL/search",
                parameters
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception(response.error?.message ?: "Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMasterModule(moduleId: String): Result<WorkspaceModuleApiModel.MasterModule> {
        return try {
            val response: Response<WorkspaceModuleApiModel.MasterModule> = get(
                client,
                WORKSPACE_MODULE_ENDPOINT + "$MASTER_MODULES_URL/$moduleId"
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception(response.error?.message ?: "Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun performModuleAction(
        moduleId: String,
        action: String,
        parameters: Map<String, Any>?,
    ): Result<WorkspaceModuleApiModel.ModuleActionResponse> {
        return try {
            val requestBody = buildMap<String, Any> {
                put("action", action)
                parameters?.let { put("parameters", it) }
            }
            val response: Response<WorkspaceModuleApiModel.ModuleActionResponse> = post(
                client,
                WORKSPACE_MODULE_ENDPOINT + "$BASE_URL/$moduleId/action",
                requestBody
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception(response.error?.message ?: "Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun installModule(
        installRequest: WorkspaceModuleApiModel.ModuleInstallationRequest,
    ): Result<WorkspaceModuleApiModel.ModuleOperationResponse> {
        return try {
            val response: Response<WorkspaceModuleApiModel.ModuleOperationResponse> = post(
                client,
                WORKSPACE_MODULE_ENDPOINT + "$BASE_URL/install",
                installRequest
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception(response.error?.message ?: "Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uninstallModule(
        moduleId: String,
        preserveData: Boolean,
    ): Result<WorkspaceModuleApiModel.ModuleOperationResponse> {
        return try {
            val parameters = mapOf("preserve_data" to preserveData)
            val response: Response<WorkspaceModuleApiModel.ModuleOperationResponse> = delete(
                client,
                WORKSPACE_MODULE_ENDPOINT + "$BASE_URL/$moduleId",
                parameters
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception(response.error?.message ?: "Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateModuleConfiguration(
        moduleId: String,
        configRequest: WorkspaceModuleApiModel.ModuleConfigurationRequest,
    ): Result<WorkspaceModuleApiModel.ModuleOperationResponse> {
        return try {
            val response: Response<WorkspaceModuleApiModel.ModuleOperationResponse> = put(
                client,
                WORKSPACE_MODULE_ENDPOINT + "$BASE_URL/$moduleId/configuration",
                configRequest
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception(response.error?.message ?: "Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun performBulkOperation(
        bulkRequest: WorkspaceModuleApiModel.BulkOperationRequest,
    ): Result<WorkspaceModuleApiModel.BulkOperationResponse> {
        return try {
            val response: Response<WorkspaceModuleApiModel.BulkOperationResponse> = post(
                client,
                WORKSPACE_MODULE_ENDPOINT + "$BASE_URL/bulk",
                bulkRequest
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception(response.error?.message ?: "Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getModuleDashboard(): Result<WorkspaceModuleApiModel.ModuleDashboardResponse> {
        return try {
            val response: Response<WorkspaceModuleApiModel.ModuleDashboardResponse> = get(
                client,
                WORKSPACE_MODULE_ENDPOINT + "$BASE_URL/dashboard"
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception(response.error?.message ?: "Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getModuleAnalytics(
        moduleId: String,
        period: String,
    ): Result<WorkspaceModuleApiModel.ModuleAnalyticsResponse> {
        return try {
            val parameters = mapOf("period" to period)
            val response: Response<WorkspaceModuleApiModel.ModuleAnalyticsResponse> = get(
                client,
                WORKSPACE_MODULE_ENDPOINT + "$BASE_URL/$moduleId/analytics",
                parameters
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception(response.error?.message ?: "Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportModuleConfiguration(
        moduleIds: List<String>?,
    ): Result<WorkspaceModuleApiModel.ModuleConfigurationExportResponse> {
        return try {
            val requestBody = moduleIds?.let { mapOf("module_ids" to it) }
            val response: Response<WorkspaceModuleApiModel.ModuleConfigurationExportResponse> = post(
                client,
                WORKSPACE_MODULE_ENDPOINT + "$BASE_URL/export",
                requestBody
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception(response.error?.message ?: "Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importModuleConfiguration(
        configData: Map<String, Any>,
    ): Result<WorkspaceModuleApiModel.ModuleOperationResponse> {
        return try {
            val response: Response<WorkspaceModuleApiModel.ModuleOperationResponse> = post(
                client,
                WORKSPACE_MODULE_ENDPOINT + "$BASE_URL/import",
                configData
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception(response.error?.message ?: "Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getModuleCategories(): Result<List<String>> {
        return try {
            val response: Response<List<String>> = get(
                client,
                WORKSPACE_MODULE_ENDPOINT + "$MASTER_MODULES_URL/categories"
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception(response.error?.message ?: "Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkForUpdates(): Result<WorkspaceModuleApiModel.ModuleUpdatesResponse> {
        return try {
            val response: Response<WorkspaceModuleApiModel.ModuleUpdatesResponse> = get(
                client,
                WORKSPACE_MODULE_ENDPOINT + "$BASE_URL/updates"
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception(response.error?.message ?: "Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getModuleHealthStatus(moduleId: String): Result<WorkspaceModuleApiModel.ModuleHealthResponse> {
        return try {
            val response: Response<WorkspaceModuleApiModel.ModuleHealthResponse> = get(
                client,
                WORKSPACE_MODULE_ENDPOINT + "$BASE_URL/$moduleId/health"
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception(response.error?.message ?: "Unknown error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleModuleStatus(
        moduleId: String,
        enabled: Boolean,
    ): Result<WorkspaceModuleApiModel.ModuleActionResponse> {
        val action = if (enabled) "enable" else "disable"
        return performModuleAction(moduleId, action)
    }

    override suspend fun updateModule(moduleId: String): Result<WorkspaceModuleApiModel.ModuleActionResponse> {
        return performModuleAction(moduleId, "update")
    }

    override suspend fun resetModuleConfiguration(moduleId: String): Result<WorkspaceModuleApiModel.ModuleActionResponse> {
        return performModuleAction(moduleId, "reset")
    }

    override suspend fun runModuleDiagnostics(moduleId: String): Result<WorkspaceModuleApiModel.ModuleActionResponse> {
        return performModuleAction(moduleId, "diagnose")
    }
}