import androidx.compose.runtime.Composable

actual fun getPlatformName(): String = "Desktop"

@Composable
fun MainView(
    onLoggedIn: (Boolean) -> Unit,
    onNavigationServiceReady: ((com.ampairs.workspace.navigation.DynamicModuleNavigationService?) -> Unit)? = null
) = App(onLoggedIn, onNavigationServiceReady)