import com.ampairs.aws.s3.AwsS3Client
import com.ampairs.aws.s3.S3Client
import com.ampairs.common.DeviceService
import com.ampairs.common.ImageCacheKeyer
import com.ampairs.tally.TallyApi
import com.ampairs.tally.TallyApiImpl
import com.ampairs.tally.TallyRepository
import com.ampairs.tally.ui.TallyViewModel
import com.seiko.imageloader.ImageLoader
import com.seiko.imageloader.cache.CachePolicy
import com.seiko.imageloader.component.setupDefaultComponents
import com.seiko.imageloader.intercept.bitmapMemoryCacheConfig
import io.ktor.client.engine.okhttp.OkHttp
import okio.Path.Companion.toOkioPath
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import java.io.File

actual val platformModule: Module = module {
    single {
        OkHttp.create()
    }
    single { AwsS3Client() } bind (S3Client::class)
    single<DeviceService> { DesktopDeviceService() }
    single { TallyApiImpl(get()) } bind (TallyApi::class)
    single { TallyRepository(get()) }
    single { TallyViewModel(get(), get(), get(), get(), get()) }
}

fun generateImageLoader(): ImageLoader {
    return ImageLoader {
        options {
            memoryCachePolicy = CachePolicy.ENABLED
            diskCachePolicy = CachePolicy.ENABLED
        }
        components {
            add(ImageCacheKeyer())
            setupDefaultComponents()
        }
        interceptor {
            bitmapMemoryCacheConfig {
                maxSize(32 * 1024 * 1024) // 32MB
            }
            diskCacheConfig {
                directory(getCacheDir().toOkioPath().resolve("image_cache"))
                maxSizeBytes(512L * 1024 * 1024) // 512MB
            }
        }
    }
}

// about currentOperatingSystem, see app
private fun getCacheDir(): File {
    val ApplicationName = "ampairs"
    return when (currentOperatingSystem) {
        OperatingSystem.Windows -> File(System.getenv("AppData"), "$ApplicationName/cache")
        OperatingSystem.Linux -> File(System.getProperty("user.home"), ".cache/$ApplicationName")
        OperatingSystem.MacOS -> File(
            System.getProperty("user.home"),
            "Library/Caches/$ApplicationName"
        )

        else -> throw IllegalStateException("Unsupported operating system")
    }
}

fun getDatabaseDir(): File {
    val ApplicationName = "ampairs"
    return when (currentOperatingSystem) {
        OperatingSystem.Windows -> File(System.getenv("AppData"), "$ApplicationName")
        OperatingSystem.Linux -> File(System.getProperty("user.home"), "$ApplicationName")
        OperatingSystem.MacOS -> File(System.getProperty("user.home"), "$ApplicationName")
        else -> throw IllegalStateException("Unsupported operating system")
    }
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