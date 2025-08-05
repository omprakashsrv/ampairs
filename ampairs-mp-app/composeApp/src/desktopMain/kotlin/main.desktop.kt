import androidx.compose.runtime.Composable

actual fun getPlatformName(): String = "Desktop"

@Composable
fun MainView(onLoggedIn: (Boolean) -> Unit) = App(onLoggedIn)