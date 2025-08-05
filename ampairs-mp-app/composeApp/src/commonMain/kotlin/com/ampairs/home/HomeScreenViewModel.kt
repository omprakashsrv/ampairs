package com.ampairs.home

import CustomerRoute
import InventoryRoute
import InvoiceRoute
import OrderRoute
import ProductRoute
import Route
import androidx.lifecycle.ViewModel

class HomeScreenViewModel : ViewModel() {

    val navItems = listOf(
        NavItem(title = "Product", navPath = ProductRoute.Products),
        NavItem(title = "Customer", navPath = CustomerRoute.CustomerView),
        NavItem(title = "Inventory", navPath = InventoryRoute.Inventory),
        NavItem(title = "New Order", navPath = Route.Customer, routePath = OrderRoute.Root()),
        NavItem(title = "Orders", navPath = OrderRoute.Orders),
        NavItem(title = "Invoices", navPath = InvoiceRoute.Invoices),
        NavItem(title = "New Invoice", navPath = Route.Customer, routePath = InvoiceRoute.Root()),
    )
}