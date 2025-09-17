import com.ampairs.aws.s3.IosS3Client
import com.ampairs.aws.s3.S3Client
import com.ampairs.common.DeviceService
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.database.DatabasePathProvider
import com.ampairs.common.database.IosDatabasePathProvider
import com.ampairs.common.database.WorkspaceAwareDatabaseFactory
import com.ampairs.common.theme.iosAppConfigModule
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformModule: Module = module {
    single {
        Darwin.create()
    }
    single<DeviceService> { IosDeviceService() }

    // Database path provider and factory for workspace-aware databases
    single<DatabasePathProvider> { IosDatabasePathProvider() }
    single { WorkspaceAwareDatabaseFactory(get(), DispatcherProvider.io) }

    // Include theme DataStore module for iOS
    includes(iosAppConfigModule)
}

actual val awsModule: Module = module {
    single { IosS3Client() } bind (S3Client::class)
}

actual val authPlatformModule: Module = com.ampairs.auth.authPlatformModule
actual val workspacePlatformModule: Module = com.ampairs.workspace.workspacePlatformModule
actual val customerPlatformModule: Module = com.ampairs.customer.customerPlatformModule
actual val productPlatformModule: Module = com.ampairs.product.productPlatformModule
actual val orderPlatformModule: Module = com.ampairs.order.orderPlatformModule
actual val invoicePlatformModule: Module = com.ampairs.invoice.invoicePlatformModule
actual val inventoryPlatformModule: Module = com.ampairs.inventory.inventoryPlatformModule
actual val tallyPlatformModule: Module = com.ampairs.tally.tallyPlatformModule