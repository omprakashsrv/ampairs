package com.ampairs.workspace.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.post
import com.ampairs.common.put
import com.ampairs.common.model.Response
import com.ampairs.workspace.api.model.CreateWorkspaceRequest
import com.ampairs.workspace.api.model.UpdateWorkspaceRequest
import com.ampairs.workspace.api.model.WorkspaceApiModel
import com.ampairs.workspace.api.model.PagedWorkspaceResponse
import io.ktor.client.engine.HttpClientEngine

const val WORKSPACE_ENDPOINT = "http://localhost:8080"
const val WORKSPACE_PATH = "$WORKSPACE_ENDPOINT/workspace/v1"

class WorkspaceApiImpl(engine: HttpClientEngine, private val tokenRepository: TokenRepository) :
    WorkspaceApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getUserWorkspaces(
        page: Int,
        size: Int,
        sortBy: String,
        sortDir: String,
    ): Response<PagedWorkspaceResponse> {
        val params = mapOf(
            "page" to page,
            "size" to size,
            "sortBy" to sortBy,
            "sortDir" to sortDir
        )
        return get(client, WORKSPACE_PATH, params)
    }

    override suspend fun getWorkspace(workspaceId: String): Response<WorkspaceApiModel> {
        return get(client, "$WORKSPACE_PATH/$workspaceId")
    }

    override suspend fun getWorkspaceBySlug(slug: String): Response<WorkspaceApiModel> {
        return get(client, "$WORKSPACE_PATH/by-slug/$slug")
    }

    override suspend fun createWorkspace(request: CreateWorkspaceRequest): Response<WorkspaceApiModel> {
        return post(client, WORKSPACE_PATH, request)
    }

    override suspend fun updateWorkspace(
        workspaceId: String,
        request: UpdateWorkspaceRequest,
    ): Response<WorkspaceApiModel> {
        return put(client, "$WORKSPACE_PATH/$workspaceId", request)
    }

    override suspend fun checkSlugAvailability(slug: String): Response<Map<String, Boolean>> {
        return get(client, "$WORKSPACE_PATH/check-slug/$slug")
    }

    override suspend fun archiveWorkspace(workspaceId: String): Response<String> {
        return post(client, "$WORKSPACE_PATH/$workspaceId/archive", null)
    }

    override suspend fun getMyRole(workspaceId: String): Response<Map<String, Any>> {
        return get(client, "$WORKSPACE_PATH/$workspaceId/my-role")
    }
}