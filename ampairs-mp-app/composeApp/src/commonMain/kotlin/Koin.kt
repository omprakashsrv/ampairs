// import com.ampairs.inventory.inventoryModule
// import com.ampairs.invoice.invoiceModule
// import com.ampairs.order.orderModule
import com.ampairs.auth.authModule
import com.ampairs.common.theme.themeModule
import com.ampairs.customer.di.customerModule
import com.ampairs.customer.ui.components.location.locationServiceModule
import com.ampairs.product.productModule
import com.ampairs.tax.taxModule
import com.ampairs.workspace.navigation.initializeModuleProviders
import com.ampairs.workspace.workspaceModule
import org.koin.core.KoinApplication
import org.koin.core.module.Module


fun initKoin(koinApplication: KoinApplication): KoinApplication {
    // Initialize module providers for dynamic navigation
    initializeModuleProviders()
    koinApplication.modules(
        listOf(
            themeModule,
            platformModule,
            awsModule,
            // Platform-specific Room database modules
            authPlatformModule,
            workspacePlatformModule,
            customerPlatformModule,
            productPlatformModule,
            taxPlatformModule,
            // Temporarily disabled platform modules pending updates
            // orderPlatformModule,
            // invoicePlatformModule,
            // inventoryPlatformModule,
            // tallyPlatformModule,
            // Common feature modules
            authModule(),
            workspaceModule(),
            customerModule,
            locationServiceModule,
            productModule(),
            taxModule,
            // Temporarily disabled modules pending customer integration updates
            // inventoryModule(),
            // orderModule(),
            // invoiceModule(),
        )
    )
    return koinApplication
}

expect val platformModule: Module
expect val awsModule: Module
expect val authPlatformModule: Module
expect val workspacePlatformModule: Module
expect val customerPlatformModule: Module
expect val productPlatformModule: Module
expect val taxPlatformModule: Module
// Temporarily commented out pending customer integration updates
// expect val orderPlatformModule: Module
// expect val invoicePlatformModule: Module
// expect val inventoryPlatformModule: Module
// expect val tallyPlatformModule: Module

