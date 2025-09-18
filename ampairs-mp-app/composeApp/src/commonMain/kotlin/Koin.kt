import com.ampairs.auth.authModule
import com.ampairs.common.theme.themeModule
import com.ampairs.workspace.workspaceModule
import com.ampairs.customer.di.customerModule
import com.ampairs.home.homeModule
// import com.ampairs.inventory.inventoryModule
// import com.ampairs.invoice.invoiceModule
import com.ampairs.menu.menuModule
// import com.ampairs.order.orderModule
import com.ampairs.product.productModule
import org.koin.core.KoinApplication
import org.koin.core.module.Module


fun initKoin(koinApplication: KoinApplication): KoinApplication {
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
            // Temporarily disabled platform modules pending updates
            // orderPlatformModule,
            // invoicePlatformModule,
            // inventoryPlatformModule,
            // tallyPlatformModule,
            // Common feature modules
            menuModule(),
            authModule(),
            workspaceModule(),
            customerModule,
            homeModule(),
            productModule(),
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
// Temporarily commented out pending customer integration updates
// expect val orderPlatformModule: Module
// expect val invoicePlatformModule: Module
// expect val inventoryPlatformModule: Module
// expect val tallyPlatformModule: Module

