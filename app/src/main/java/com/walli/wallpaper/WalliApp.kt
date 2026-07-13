package com.walli.wallpaper

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import okio.Path.Companion.toPath
import java.io.File
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class WalliApp : Application(), SingletonImageLoader.Factory, Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var okHttpClient: OkHttpClient

    override fun onCreate() {
        super.onCreate()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(
                if (::workerFactory.isInitialized) workerFactory
                else throw IllegalStateException("HiltWorkerFactory not initialized")
            )
            .build()

    override fun newImageLoader(context: android.content.Context): ImageLoader {
        val imageCacheDir = File(context.cacheDir, "coil_image_cache").apply { mkdirs() }
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(imageCacheDir.absolutePath.toPath())
                    .maxSizeBytes(250L * 1024 * 1024) // Increase to 250MB for better persistence
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
