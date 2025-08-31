package com.ampairs.workspace.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.delete
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.put
import com.ampairs.network.model.Response
import com.ampairs.workspace.api.model.MemberApiModel
import com.ampairs.workspace.api.model.MemberDetailsResponse
import com.ampairs.workspace.api.model.PagedMemberResponse
import com.ampairs.workspace.api.model.UpdateMemberRequest
import com.ampairs.workspace.api.model.UserRoleResponse
import io.ktor.client.engine.HttpClientEngine

/**
 * Implementation of WorkspaceMemberApi using Ktor HTTP client
 */
class WorkspaceMemberApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
) : WorkspaceMemberApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getWorkspaceMembers(
        workspaceId: String,
        page: Int,
        size: Int,
        sortBy: String,
        sortDir: String,
    ): Response<PagedMemberResponse> {
        val params = mapOf(
            "page" to page,
            "size" to size,
            "sortBy" to sortBy,
            "sortDir" to sortDir
        )
        return get(client, "$WORKSPACE_ENDPOINT/workspace/v1/$workspaceId/members", params)
    }

    override suspend fun getMemberDetails(
        workspaceId: String,
        memberId: String,
    ): Response<MemberDetailsResponse> {
        return get(client, "$WORKSPACE_ENDPOINT/workspace/v1/$workspaceId/members/$memberId")
    }

    override suspend fun updateMember(
        workspaceId: String,
        memberId: String,
        request: UpdateMemberRequest,
    ): Response<MemberDetailsResponse> {
        return put(client, "$WORKSPACE_ENDPOINT/workspace/v1/$workspaceId/members/$memberId", request)
    }

    override suspend fun removeMember(
        workspaceId: String,
        memberId: String,
    ): Response<String> {
        return delete(client, "$WORKSPACE_ENDPOINT/workspace/v1/$workspaceId/members/$memberId")
    }

    override suspend fun getMyRole(workspaceId: String): Response<UserRoleResponse> {
        return get(client, "$WORKSPACE_ENDPOINT/workspace/v1/$workspaceId/my-role")
    }
}