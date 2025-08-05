import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun App(onLoggedIn: (Boolean) -> Unit) {
    MaterialTheme {
        AppNavigation()
    }
}

expect fun getPlatformName(): String