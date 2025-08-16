package com.ampairs.workspace

import com.ampairs.workspace.api.WorkspaceApi
import com.ampairs.workspace.api.WorkspaceApiImpl
import com.ampairs.workspace.db.WorkspaceRepository
import com.ampairs.workspace.db.getWorkspaceDatabase
import com.ampairs.workspace.viewmodel.WorkspaceCreateViewModel
import com.ampairs.workspace.viewmodel.WorkspaceListViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val workspaceModule = module {

    // Database
    single {
        val databasePath = get<String>(qualifier = org.koin.core.qualifier.named("workspaceDatabasePath"))
        getWorkspaceDatabase(databasePath)
    }

    single { get<com.ampairs.workspace.db.WorkspaceRoomDatabase>().workspaceDao() }

    // API
    singleOf(::WorkspaceApiImpl) bind WorkspaceApi::class

    // Repository
    singleOf(::WorkspaceRepository)

    // ViewModels
    factoryOf(::WorkspaceListViewModel)
    factoryOf(::WorkspaceCreateViewModel)
}