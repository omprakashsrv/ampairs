package com.ampairs.workspace

import com.ampairs.workspace.api.WorkspaceApi
import com.ampairs.workspace.api.WorkspaceApiImpl
import com.ampairs.workspace.api.WorkspaceMemberApi
import com.ampairs.workspace.api.WorkspaceMemberApiImpl
import com.ampairs.workspace.api.WorkspaceInvitationApi
import com.ampairs.workspace.api.WorkspaceInvitationApiImpl
import com.ampairs.workspace.api.WorkspaceModuleApi
import com.ampairs.workspace.api.WorkspaceModuleApiImpl
import com.ampairs.workspace.db.WorkspaceRepository
import com.ampairs.workspace.db.OfflineFirstWorkspaceRepository
import com.ampairs.workspace.db.WorkspaceMemberRepository
import com.ampairs.workspace.db.OfflineFirstWorkspaceMemberRepository
import com.ampairs.workspace.db.WorkspaceInvitationRepository
import com.ampairs.workspace.manager.WorkspaceDataManager
import com.ampairs.workspace.manager.WorkspaceMemberDataManager
import com.ampairs.workspace.viewmodel.WorkspaceCreateViewModel
import com.ampairs.workspace.viewmodel.WorkspaceListViewModel
import com.ampairs.workspace.viewmodel.WorkspaceMembersViewModel
import com.ampairs.workspace.viewmodel.WorkspaceInvitationsViewModel
import com.ampairs.workspace.viewmodel.WorkspaceModulesViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

fun workspaceModule() = module {

    // Database (provided by platform-specific modules)
    single { get<com.ampairs.workspace.db.WorkspaceRoomDatabase>().workspaceDao() }
    single { get<com.ampairs.workspace.db.WorkspaceRoomDatabase>().workspaceMemberDao() }

    // APIs
    singleOf(::WorkspaceApiImpl) bind WorkspaceApi::class
    singleOf(::WorkspaceMemberApiImpl) bind WorkspaceMemberApi::class
    singleOf(::WorkspaceInvitationApiImpl) bind WorkspaceInvitationApi::class
    singleOf(::WorkspaceModuleApiImpl) bind WorkspaceModuleApi::class
    
    // Repositories
    single { WorkspaceRepository(get(), get(), get()) } // Legacy repository
    single { OfflineFirstWorkspaceRepository(get(), get(), get()) } // Simplified offline-first repository
    single { WorkspaceMemberRepository(get(), get(), get()) } // Member management repository (updated with DAO)
    single { OfflineFirstWorkspaceMemberRepository(get(), get(), get()) } // Offline-first member repository
    single { WorkspaceInvitationRepository(get(), get()) } // Invitation management repository
    
    // Data managers for offline-first synchronization
    single { WorkspaceDataManager(get(), get()) }
    single { WorkspaceMemberDataManager(get(), get()) }

    // ViewModels with parameter support
    factory { WorkspaceListViewModel(get(), get(), get(), get()) }
    factoryOf(::WorkspaceCreateViewModel)

    // Member and invitation ViewModels with workspaceId parameter
    factory { (workspaceId: String) -> WorkspaceMembersViewModel(workspaceId, get()) }
    factory { (workspaceId: String) -> WorkspaceInvitationsViewModel(workspaceId, get()) }

    // Module management ViewModel
    factory { WorkspaceModulesViewModel(get()) }
}