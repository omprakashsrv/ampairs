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
import com.ampairs.workspace.db.OfflineFirstWorkspaceInvitationRepository
import com.ampairs.workspace.db.OfflineFirstRolesPermissionsRepository
import com.ampairs.workspace.store.WorkspaceStoreFactory
import com.ampairs.workspace.store.WorkspaceMemberStoreFactory
import com.ampairs.workspace.store.WorkspaceInvitationStoreFactory
import com.ampairs.workspace.store.WorkspaceRolesStoreFactory
import com.ampairs.workspace.store.WorkspacePermissionsStoreFactory
import com.ampairs.workspace.store.WorkspaceMemberUpdateStoreFactory
import com.ampairs.workspace.store.WorkspaceStore
import com.ampairs.workspace.store.WorkspaceMemberStore
import com.ampairs.workspace.store.WorkspaceInvitationStore
import com.ampairs.workspace.store.WorkspaceRolesStore
import com.ampairs.workspace.store.WorkspacePermissionsStore
import com.ampairs.workspace.store.WorkspaceMemberUpdateStore
import com.ampairs.workspace.viewmodel.WorkspaceCreateViewModel
import com.ampairs.workspace.viewmodel.WorkspaceListViewModel
import com.ampairs.workspace.viewmodel.WorkspaceMembersViewModel
import com.ampairs.workspace.viewmodel.MemberDetailsViewModel
import com.ampairs.workspace.viewmodel.WorkspaceInvitationsViewModel
import com.ampairs.workspace.viewmodel.WorkspaceModulesViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

fun workspaceModule() = module {

    // Database (provided by platform-specific modules)
    single { get<com.ampairs.workspace.db.WorkspaceRoomDatabase>().workspaceDao() }
    single { get<com.ampairs.workspace.db.WorkspaceRoomDatabase>().workspaceMemberDao() }
    single { get<com.ampairs.workspace.db.WorkspaceRoomDatabase>().workspaceInvitationDao() }
    single { get<com.ampairs.workspace.db.WorkspaceRoomDatabase>().workspaceRoleDao() }
    single { get<com.ampairs.workspace.db.WorkspaceRoomDatabase>().workspacePermissionDao() }

    // APIs
    singleOf(::WorkspaceApiImpl) bind WorkspaceApi::class
    singleOf(::WorkspaceMemberApiImpl) bind WorkspaceMemberApi::class
    singleOf(::WorkspaceInvitationApiImpl) bind WorkspaceInvitationApi::class
    singleOf(::WorkspaceModuleApiImpl) bind WorkspaceModuleApi::class
    
    // Repositories - now powered by Store5 for offline-first architecture
    single<WorkspaceRepository> { WorkspaceRepository(get(), get(), get()) }
    single<WorkspaceMemberRepository> { WorkspaceMemberRepository(get(), get(), get()) }
    single { WorkspaceInvitationRepository(get(), get()) } // Legacy invitation management repository (kept for compatibility)
    single { OfflineFirstWorkspaceInvitationRepository(get(), get(), get(named("workspaceInvitationStore")), get()) } // Store5 invitation repository
    single { OfflineFirstRolesPermissionsRepository(get(), get(), get(), get()) } // Roles & permissions offline-first repo

    // Store5 Factories for proper offline-first architecture
    single { WorkspaceStoreFactory(get(), get()) }
    single { WorkspaceMemberStoreFactory(get(), get()) }
    single { WorkspaceInvitationStoreFactory(get(), get()) }
    single { WorkspaceRolesStoreFactory(get(), get()) }
    single { WorkspacePermissionsStoreFactory(get(), get()) }
    single { WorkspaceMemberUpdateStoreFactory(get(), get()) }
    
    // Store instances with qualifiers to prevent conflicts
    single<WorkspaceStore>(named("workspaceStore")) { get<WorkspaceStoreFactory>().create() }
    single<WorkspaceMemberStore>(named("workspaceMemberStore")) { get<WorkspaceMemberStoreFactory>().create() }
    single<WorkspaceInvitationStore>(named("workspaceInvitationStore")) { get<WorkspaceInvitationStoreFactory>().create() }
    single<WorkspaceRolesStore>(named("workspaceRolesStore")) { get<WorkspaceRolesStoreFactory>().create() }
    single<WorkspacePermissionsStore>(named("workspacePermissionsStore")) { get<WorkspacePermissionsStoreFactory>().create() }
    single<WorkspaceMemberUpdateStore>(named("workspaceMemberUpdateStore")) { get<WorkspaceMemberUpdateStoreFactory>().create() }

    // ViewModels with parameter support
    factory { WorkspaceListViewModel(get(named("workspaceStore")), get(), get(), get()) }
    factoryOf(::WorkspaceCreateViewModel)

    // Member and invitation ViewModels with workspaceId parameter
    factory { (workspaceId: String) -> WorkspaceMembersViewModel(workspaceId, get(named("workspaceMemberStore")), get(), get()) }
    factory { (workspaceId: String, memberId: String) -> MemberDetailsViewModel(workspaceId, memberId, get(named("workspaceMemberStore")), get(), get(), get(named("workspaceRolesStore")), get(named("workspacePermissionsStore")), get()) }
    factory { (workspaceId: String) -> WorkspaceInvitationsViewModel(workspaceId, get<OfflineFirstWorkspaceInvitationRepository>()) }

    // Module management ViewModel
    factory { WorkspaceModulesViewModel(get()) }
}