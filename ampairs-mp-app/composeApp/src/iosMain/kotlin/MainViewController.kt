import androidx.compose.ui.window.ComposeUIViewController
import coil3.compose.setSingletonImageLoaderFactory
import initKoin
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController {
    // Initialize Koin for iOS
    if (org.koin.mp.KoinPlatform.getKoinOrNull() == null) {
        val koinApplication = startKoin { }
        initKoin(koinApplication)
    }

    // Initialize Coil ImageLoader
    setSingletonImageLoaderFactory { context ->
        generateImageLoader()
    }

    App({})
}