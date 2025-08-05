import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable
    data object Login : Route
    
    @Serializable
    data object Company : Route
    
    @Serializable
    data object Home : Route
    
    @Serializable
    data object Customer : Route
    
    @Serializable
    data object Product : Route
    
    @Serializable
    data object Inventory : Route
    
    @Serializable
    data object Order : Route
    
    @Serializable
    data object Invoice : Route
}

// Home routes
@Serializable
sealed interface HomeRoute {
    @Serializable
    data object Root : HomeRoute
}

// Auth routes
@Serializable
sealed interface AuthRoute {
    @Serializable
    data object LoginRoot : AuthRoute
    
    @Serializable
    data object Phone : AuthRoute
    
    @Serializable
    data object Otp : AuthRoute
}

// Company routes
@Serializable
sealed interface CompanyRoute {
    @Serializable
    data object Root : CompanyRoute
    
    @Serializable
    data class Update(
        val id: String = ""
    ) : CompanyRoute
}

// Product routes
@Serializable
sealed interface ProductRoute {
    @Serializable
    data class Group(
        val type: String = "GROUP",
        val edit: Boolean = false
    ) : ProductRoute
    
    @Serializable
    data class Product(
        val groupId: String = ""
    ) : ProductRoute
    
    @Serializable
    data class ProductEdit(
        val productId: String = ""
    ) : ProductRoute
    
    @Serializable
    data object Products : ProductRoute
    
    @Serializable
    data object TaxInfo : ProductRoute
    
    @Serializable
    data object TaxCode : ProductRoute
}

// Customer routes
@Serializable
sealed interface CustomerRoute {
    @Serializable
    data object Root : CustomerRoute
    
    @Serializable
    data object CustomerView : CustomerRoute
    
    @Serializable
    data class CustomerEdit(
        val id: String = ""
    ) : CustomerRoute
    
    @Serializable
    data class Redirect(
        val fromCustomer: String = "",
        val toCustomer: String = ""
    ) : CustomerRoute
}

// Inventory routes
@Serializable
sealed interface InventoryRoute {
    @Serializable
    data object Inventory : InventoryRoute
}

// Order routes
@Serializable
sealed interface OrderRoute {
    @Serializable
    data class Root(
        val fromCustomer: String = "",
        val toCustomer: String = "",
        val id: String = ""
    ) : OrderRoute
    
    @Serializable
    data class OrderView(
        val id: String = ""
    ) : OrderRoute
    
    @Serializable
    data object Orders : OrderRoute
}

// Invoice routes
@Serializable
sealed interface InvoiceRoute {
    @Serializable
    data class Root(
        val fromCustomer: String = "",
        val toCustomer: String = "",
        val id: String = ""
    ) : InvoiceRoute
    
    @Serializable
    data class InvoiceView(
        val id: String = ""
    ) : InvoiceRoute
    
    @Serializable
    data object Invoices : InvoiceRoute
}