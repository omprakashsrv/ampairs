package com.ampairs.workspace

import com.ampairs.workspace.api.WorkspaceApi
import com.ampairs.workspace.api.WorkspaceApiImpl
import com.ampairs.workspace.db.WorkspaceRepository
import com.ampairs.workspace.db.OfflineFirstWorkspaceRepository
import com.ampairs.workspace.manager.WorkspaceDataManager
import com.ampairs.workspace.viewmodel.WorkspaceCreateViewModel
import com.ampairs.workspace.viewmodel.WorkspaceListViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

fun workspaceModule() = module {

    // Database (provided by platform-specific modules)
    single { get<com.ampairs.workspace.db.WorkspaceRoomDatabase>().workspaceDao() }

    // API
    singleOf(::WorkspaceApiImpl) bind WorkspaceApi::class
    
    // Repositories
    single { WorkspaceRepository(get(), get(), get()) } // Legacy repository
    single { OfflineFirstWorkspaceRepository(get(), get(), get()) } // Simplified offline-first repository
    
    // Data manager for offline-first workspace synchronization
    single { WorkspaceDataManager(get(), get()) }

    // ViewModels
    factory { WorkspaceListViewModel(get(), get(), get(), get()) }
    factoryOf(::WorkspaceCreateViewModel)
}