package com.ampairs.tax

import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.tax.data.db.TaxRoomDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

val taxPlatformModule: Module = module {
    single<TaxRoomDatabase> {
        val factory = get<WorkspaceAwareDatabaseFactory>()
        factory.createDatabase(
            klass = TaxRoomDatabase::class,
            moduleName = "tax"
        )
    }
}