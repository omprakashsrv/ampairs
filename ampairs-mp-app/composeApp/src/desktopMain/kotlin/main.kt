import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.input.key.key
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.ampairs.menu.AppNavigator
import com.ampairs.product.ui.group.GroupType
import com.ampairs.tally.TallyApp
import com.seiko.imageloader.LocalImageLoader
import org.koin.compose.koinInject

import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.get

fun main() = application {
    if (GlobalContext.getOrNull() == null) {
        val koinApplication = startKoin {}
        initKoin(koinApplication)
    }
    val applicationState = remember { ApplicationState() }
    applicationState.windows
    for (window in applicationState.windows) {
        key(window) {
            if (window.title == "Main") {
                MainWindow(window)
            } else if (window.title == "Tally") {
                TallyWindow(window)
            }
        }
    }
}


@Composable
private fun ApplicationScope.TallyWindow(
    state: AppWindowState,
) = Window(onCloseRequest = state::close, title = state.title,
    onKeyEvent = {
        false
    }) {
    CompositionLocalProvider(
        LocalImageLoader provides remember { generateImageLoader() },
    ) {
        TallyApp()
    }
}

@Composable
private fun ApplicationScope.MainWindow(state: AppWindowState) =
    Window(onCloseRequest = ::exitApplication,
        state = WindowState(placement = WindowPlacement.Maximized),
        title = "Ampairs",
        onKeyEvent = {
            if (it.key == Key.Escape) {
                val appNavigator = get<AppNavigator>(AppNavigator::class.java)
                appNavigator.goBack()
                return@Window true
            }
            return@Window false
        }) {

        var loggedIn by remember { mutableStateOf(false) }
        CompositionLocalProvider(
            LocalImageLoader provides remember { generateImageLoader() },
        ) {
            MainView {
                loggedIn = it
            }
        }
        val appNavigator = koinInject<AppNavigator>()
        MenuBar {
            if (loggedIn) {
                Menu("Product", mnemonic = 'P') {
                    Item(
                        "Group",
                        onClick = { appNavigator.navigate(ProductRoute.Group(type = GroupType.GROUP.name, edit = true).toString()) },
                        shortcut = KeyShortcut(Key.G, ctrl = true)
                    )
                    Item(
                        "Category",
                        onClick = { appNavigator.navigate(ProductRoute.Group(type = GroupType.CATEGORY.name, edit = true).toString()) },
                        shortcut = KeyShortcut(Key.P, ctrl = true)
                    )
                    Item(
                        "Sub Category",
                        onClick = { appNavigator.navigate(ProductRoute.Group(type = GroupType.SUBCATEGORY.name, edit = true).toString()) },
                        shortcut = KeyShortcut(Key.P, ctrl = true)
                    )
                    Item(
                        "Brand",
                        onClick = { appNavigator.navigate(ProductRoute.Group(type = GroupType.BRAND.name, edit = true).toString()) },
                        shortcut = KeyShortcut(Key.P, ctrl = true)
                    )
                    Item(
                        "All Products",
                        onClick = { appNavigator.navigate(ProductRoute.Products.toString()) },
                        shortcut = KeyShortcut(Key.P, ctrl = true)
                    )
                }
                Menu("Tax", mnemonic = 'T') {
                    Item(
                        "Tax Info",
                        onClick = { appNavigator.navigate(ProductRoute.TaxInfo.toString()) },
                        shortcut = KeyShortcut(Key.G, ctrl = true)
                    )
                    Item(
                        "Tax Codes",
                        onClick = { appNavigator.navigate(ProductRoute.TaxCode.toString()) },
                        shortcut = KeyShortcut(Key.P, ctrl = true)
                    )
                }
                Menu("Customer", mnemonic = 'F') {
                    Item(
                        "States",
                        onClick = { appNavigator.navigate(Route.Customer.toString()) },
                        shortcut = KeyShortcut(Key.V, ctrl = true)
                    )
                    Item(
                        "All Customers",
                        onClick = { appNavigator.navigate(CustomerRoute.CustomerView.toString()) },
                        shortcut = KeyShortcut(Key.C, ctrl = true)
                    )
                }
                Menu("Order", mnemonic = 'O') {
                    Item(
                        "New Order",
                        onClick = { appNavigator.navigate(Route.Customer.toString()) },
                        shortcut = KeyShortcut(Key.O, ctrl = true)
                    )
                    Item(
                        "All Orders",
                        onClick = { appNavigator.navigate(OrderRoute.Orders.toString()) },
                        shortcut = KeyShortcut(Key.O, ctrl = true)
                    )
                }
                Menu("Tally", mnemonic = 'O') {
                    Item(
                        "Open Tally Window",
                        onClick = { state.openNewWindow() },
                        shortcut = KeyShortcut(Key.O, ctrl = true)
                    )
                }
            }
        }
    }

private class ApplicationState {
    val windows = mutableStateListOf<AppWindowState>()

    init {
        windows += AppWindowState(title = "Main")
    }

    fun openNewWindow() {
        windows += AppWindowState(title = "Tally")
    }

    fun exit() {
        windows.clear()
    }

    private fun AppWindowState(
        title: String,
    ) = AppWindowState(
        title,
        openNewWindow = ::openNewWindow,
        exit = ::exit,
        windows::remove
    )
}

private class AppWindowState(
    val title: String,
    val openNewWindow: () -> Unit,
    val exit: () -> Unit,
    private val close: (AppWindowState) -> Unit,
) {
    fun close() = close(this)
}