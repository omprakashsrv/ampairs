package com.ampairs.app

import MainView
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.ampairs.common.ImageCacheKeyer
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import coil3.util.DebugLogger
import io.github.vinceglb.filekit.core.FileKit
import okio.Path.Companion.toOkioPath

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()

        // Initialize FileKit for Android platform
        FileKit.init(this)
        setContent {
            setSingletonImageLoaderFactory { context ->
                generateImageLoader()
            }
            MainView()
        }
    }

    private fun generateImageLoader(): ImageLoader {
        return ImageLoader.Builder(this@MainActivity)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(this@MainActivity, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this@MainActivity.cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(50L * 1024 * 1024) // 50MB
                    .build()
            }
            .components {
                add(ImageCacheKeyer())
            }
            .crossfade(true)
            .logger(DebugLogger())
            .build()
    }
}

