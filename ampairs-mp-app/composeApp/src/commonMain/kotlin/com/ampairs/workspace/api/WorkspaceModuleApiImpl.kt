package com.ampairs.workspace.api

import com.ampairs.core.network.ApiClient
import com.ampairs.workspace.api.model.WorkspaceModuleApiModel
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * Implementation of WorkspaceModuleApi using Ktor HTTP Client
 */
class WorkspaceModuleApiImpl(
    private val apiClient: ApiClient,
) : WorkspaceModuleApi {

    companion object {
        private const val BASE_URL = "/workspace/v1/modules"
        private const val MASTER_MODULES_URL = "/workspace/v1/master-modules"
    }

    override suspend fun getModuleOverview(): Result<WorkspaceModuleApiModel.ModuleOverviewResponse> {
        return apiClient.safeRequest {
            get(BASE_URL)
        }
    }

    override suspend fun getModule(moduleId: String): Result<WorkspaceModuleApiModel.ModuleDetailResponse> {
        return apiClient.safeRequest {
            get("$BASE_URL/$moduleId")
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
        return apiClient.safeRequest {
            get("$BASE_URL/search") {
                parameter("query", query)
                parameter("category", category)
                parameter("status", status)
                parameter("enabled", enabled)
                parameter("featured", featured)
                parameter("sort_by", sortBy)
                parameter("sort_direction", sortDirection)
                parameter("page", page)
                parameter("size", size)
            }
        }
    }

    override suspend fun getAvailableModules(
        businessType: String?,
    ): Result<WorkspaceModuleApiModel.AvailableModulesResponse> {
        return apiClient.safeRequest {
            get("$MASTER_MODULES_URL/available") {
                parameter("business_type", businessType)
            }
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
        return apiClient.safeRequest {
            get("$MASTER_MODULES_URL/search") {
                parameter("query", query)
                parameter("category", category)
                parameter("featured", featured)
                parameter("sort_by", sortBy)
                parameter("sort_direction", sortDirection)
                parameter("page", page)
                parameter("size", size)
            }
        }
    }

    override suspend fun getMasterModule(moduleId: String): Result<WorkspaceModuleApiModel.MasterModule> {
        return apiClient.safeRequest {
            get("$MASTER_MODULES_URL/$moduleId")
        }
    }

    override suspend fun performModuleAction(
        moduleId: String,
        action: String,
        parameters: Map<String, Any>?,
    ): Result<WorkspaceModuleApiModel.ModuleActionResponse> {
        return apiClient.safeRequest {
            post("$BASE_URL/$moduleId/action") {
                parameter("action", action)
                if (parameters != null) {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("parameters" to parameters))
                }
            }
        }
    }

    override suspend fun installModule(
        installRequest: WorkspaceModuleApiModel.ModuleInstallationRequest,
    ): Result<WorkspaceModuleApiModel.ModuleOperationResponse> {
        return apiClient.safeRequest {
            post("$BASE_URL/install") {
                contentType(ContentType.Application.Json)
                setBody(installRequest)
            }
        }
    }

    override suspend fun uninstallModule(
        moduleId: String,
        preserveData: Boolean,
    ): Result<WorkspaceModuleApiModel.ModuleOperationResponse> {
        return apiClient.safeRequest {
            delete("$BASE_URL/$moduleId") {
                parameter("preserve_data", preserveData)
            }
        }
    }

    override suspend fun updateModuleConfiguration(
        moduleId: String,
        configRequest: WorkspaceModuleApiModel.ModuleConfigurationRequest,
    ): Result<WorkspaceModuleApiModel.ModuleOperationResponse> {
        return apiClient.safeRequest {
            put("$BASE_URL/$moduleId/configuration") {
                contentType(ContentType.Application.Json)
                setBody(configRequest)
            }
        }
    }

    override suspend fun performBulkOperation(
        bulkRequest: WorkspaceModuleApiModel.BulkOperationRequest,
    ): Result<WorkspaceModuleApiModel.BulkOperationResponse> {
        return apiClient.safeRequest {
            post("$BASE_URL/bulk") {
                contentType(ContentType.Application.Json)
                setBody(bulkRequest)
            }
        }
    }

    override suspend fun getModuleDashboard(): Result<WorkspaceModuleApiModel.ModuleDashboardResponse> {
        return apiClient.safeRequest {
            get("$BASE_URL/dashboard")
        }
    }

    override suspend fun getModuleAnalytics(
        moduleId: String,
        period: String,
    ): Result<WorkspaceModuleApiModel.ModuleAnalyticsResponse> {
        return apiClient.safeRequest {
            get("$BASE_URL/$moduleId/analytics") {
                parameter("period", period)
            }
        }
    }

    override suspend fun exportModuleConfiguration(
        moduleIds: List<String>?,
    ): Result<WorkspaceModuleApiModel.ModuleConfigurationExportResponse> {
        return apiClient.safeRequest {
            post("$BASE_URL/export") {
                if (moduleIds != null) {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("module_ids" to moduleIds))
                }
            }
        }
    }

    override suspend fun importModuleConfiguration(
        configData: Map<String, Any>,
    ): Result<WorkspaceModuleApiModel.ModuleOperationResponse> {
        return apiClient.safeRequest {
            post("$BASE_URL/import") {
                contentType(ContentType.Application.Json)
                setBody(configData)
            }
        }
    }

    override suspend fun getModuleCategories(): Result<List<String>> {
        return apiClient.safeRequest {
            get("$MASTER_MODULES_URL/categories")
        }
    }

    override suspend fun checkForUpdates(): Result<WorkspaceModuleApiModel.ModuleUpdatesResponse> {
        return apiClient.safeRequest {
            get("$BASE_URL/updates")
        }
    }

    override suspend fun getModuleHealthStatus(moduleId: String): Result<WorkspaceModuleApiModel.ModuleHealthResponse> {
        return apiClient.safeRequest {
            get("$BASE_URL/$moduleId/health")
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