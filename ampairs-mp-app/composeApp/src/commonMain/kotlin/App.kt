
import androidx.compose.runtime.Composable
import com.ampairs.ui.theme.PlatformAmpairsTheme

@Composable
fun App(onLoggedIn: (Boolean) -> Unit) {
    PlatformAmpairsTheme {
        AppNavigation()
    }
}

expect fun getPlatformName(): String
