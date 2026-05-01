package com.walli.wallpaper.worker

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.Keep
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.walli.wallpaper.domain.model.WallpaperSort
import com.walli.wallpaper.domain.repository.WallpaperRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

@Keep
@HiltWorker
class AutoWallpaperWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: WallpaperRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // 1. Get a random wallpaper from the repository
            val targetPage = (1..10).random()
            
            var wallpapersResult = repository.getWallpapers(
                page = targetPage,
                limit = 20,
                categoryId = null,
                query = null,
                sort = WallpaperSort.RANDOM
            )

            var wallpapers = wallpapersResult.getOrNull()?.items ?: emptyList()
            
            // If random page was empty (common with small datasets), fallback to page 1
            if (wallpapers.isEmpty() && targetPage != 1) {
                wallpapersResult = repository.getWallpapers(
                    page = 1,
                    limit = 20,
                    categoryId = null,
                    query = null,
                    sort = WallpaperSort.RANDOM
                )
                wallpapers = wallpapersResult.getOrNull()?.items ?: return@withContext Result.failure()
            }

            if (wallpapers.isEmpty()) {
                return@withContext Result.failure()
            }

            val randomWallpaper = wallpapers.random()
            
            // 2. Download the image
            val url = URL(randomWallpaper.imageUrl)
            val connection = url.openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doInput = true
            connection.connect()
            val input = connection.getInputStream()
            val bitmap = BitmapFactory.decodeStream(input)

            if (bitmap == null) {
                return@withContext Result.failure()
            }

            // 3. Set the wallpaper
            val wallpaperManager = WallpaperManager.getInstance(applicationContext)
            wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
            
            // 4. Schedule next run in 10 seconds
            val nextWork = OneTimeWorkRequestBuilder<AutoWallpaperWorker>()
                .setInitialDelay(10, TimeUnit.SECONDS)
                .addTag("auto_wallpaper")
                .build()
            
            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                "AutoWallpaperWork",
                androidx.work.ExistingWorkPolicy.REPLACE,
                nextWork
            )

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
