
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.ampairs.ui.theme.PlatformAmpairsTheme
import com.ampairs.common.theme.ThemeManager

@Composable
fun App(onLoggedIn: (Boolean) -> Unit) {
    val themeManager = remember { ThemeManager.getInstance() }
    val isDarkTheme = themeManager.isDarkTheme()

    PlatformAmpairsTheme(
        darkTheme = isDarkTheme
    ) {
        AppNavigation()
    }
}

expect fun getPlatformName(): String
