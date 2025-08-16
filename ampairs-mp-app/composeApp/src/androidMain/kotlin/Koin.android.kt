import com.ampairs.aws.s3.AwsS3Client
import com.ampairs.aws.s3.S3Client
import com.ampairs.common.DeviceService
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformModule: Module = module {
    this.single {
        OkHttp.create()
    }
    single<DeviceService> { AndroidDeviceService(androidContext()) }
}

actual val awsModule: Module = module {
    single { AwsS3Client() } bind (S3Client::class)
}

actual val authPlatformModule: Module = com.ampairs.auth.authPlatformModule
actual val workspacePlatformModule: Module = com.ampairs.workspace.workspacePlatformModule
actual val customerPlatformModule: Module = com.ampairs.customer.customerPlatformModule
actual val productPlatformModule: Module = com.ampairs.product.productPlatformModule
actual val orderPlatformModule: Module = com.ampairs.order.orderPlatformModule
actual val invoicePlatformModule: Module = com.ampairs.invoice.invoicePlatformModule
actual val inventoryPlatformModule: Module = com.ampairs.inventory.inventoryPlatformModule
actual val tallyPlatformModule: Module = com.ampairs.tally.tallyPlatformModule

