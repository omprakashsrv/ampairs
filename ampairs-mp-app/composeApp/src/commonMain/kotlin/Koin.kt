import com.ampairs.auth.authModule
import com.ampairs.company.companyModule
import com.ampairs.customer.customerModule
import com.ampairs.inventory.inventoryModule
import com.ampairs.invoice.invoiceModule
import com.ampairs.menu.menuModule
import com.ampairs.order.orderModule
import com.ampairs.product.productModule
import org.koin.core.KoinApplication
import org.koin.core.module.Module


fun initKoin(koinApplication: KoinApplication): KoinApplication {
    koinApplication.modules(
        listOf(
            platformModule,
            awsModule,
            // Platform-specific Room database modules
            authPlatformModule,
            companyPlatformModule,
            customerPlatformModule,
            productPlatformModule,
            orderPlatformModule,
            invoicePlatformModule,
            inventoryPlatformModule,
            tallyPlatformModule,
            // Common feature modules
            menuModule(),
            authModule(),
            companyModule(),
            customerModule(),
            productModule(),
            inventoryModule(),
            orderModule(),
            invoiceModule(),
        )
    )
    return koinApplication
}

expect val platformModule: Module
expect val awsModule: Module
expect val authPlatformModule: Module
expect val companyPlatformModule: Module
expect val customerPlatformModule: Module
expect val productPlatformModule: Module
expect val orderPlatformModule: Module
expect val invoicePlatformModule: Module
expect val inventoryPlatformModule: Module
expect val tallyPlatformModule: Module

