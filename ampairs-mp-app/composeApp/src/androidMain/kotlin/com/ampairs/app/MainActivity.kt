package com.ampairs.app

import MainView
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.ampairs.common.ImageCacheKeyer
import com.seiko.imageloader.ImageLoader
import com.seiko.imageloader.LocalImageLoader
import com.seiko.imageloader.cache.CachePolicy
import com.seiko.imageloader.cache.memory.maxSizePercent
import com.seiko.imageloader.component.setupDefaultComponents
import com.seiko.imageloader.intercept.imageMemoryCacheConfig
import com.seiko.imageloader.option.androidContext
import okio.Path.Companion.toOkioPath

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        setContent {
            CompositionLocalProvider(
                LocalImageLoader provides remember { generateImageLoader() },
            ) {
                MainView()
            }
        }
    }

    private fun generateImageLoader(): ImageLoader {
        return ImageLoader {
            options {
                memoryCachePolicy = CachePolicy.ENABLED
                diskCachePolicy = CachePolicy.ENABLED
            }
            options {
                androidContext(this@MainActivity)
            }
            components {
                add(ImageCacheKeyer())
                setupDefaultComponents()
            }
            interceptor {
                imageMemoryCacheConfig {
                    // Set the max size to 25% of the app's available memory.
                    maxSizePercent(this@MainActivity, 0.25)
                }
                diskCacheConfig {
                    directory(this@MainActivity.cacheDir.resolve("image_cache").toOkioPath())
                    maxSizeBytes(50L * 1024 * 1024) // 50MB
                }
            }
        }
    }
}

