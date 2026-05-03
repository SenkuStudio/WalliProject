package com.walli.wallpaper.worker

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.annotation.Keep
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.walli.wallpaper.data.settings.AutoWallpaperSource
import com.walli.wallpaper.data.settings.SettingsManager
import com.walli.wallpaper.domain.model.Wallpaper
import com.walli.wallpaper.domain.model.WallpaperSort
import com.walli.wallpaper.domain.repository.LibraryRepository
import com.walli.wallpaper.domain.repository.WallpaperRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.URL

@Keep
@HiltWorker
class AutoWallpaperWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val wallpaperRepository: WallpaperRepository,
    private val libraryRepository: LibraryRepository,
    private val settingsManager: SettingsManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val source = settingsManager.autoWallpaperSource.first()
            val categoryId = settingsManager.autoWallpaperCategoryId.first()

            val wallpaper = when (source) {
                AutoWallpaperSource.RANDOM -> getRandomWallpaper()
                AutoWallpaperSource.FAVORITES -> getFavoriteWallpaper()
                AutoWallpaperSource.CATEGORY -> getCategoryWallpaper(categoryId)
            } ?: return@withContext Result.failure()

            // 2. Download the image
            val url = URL(wallpaper.imageUrl)
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
            wallpaperManager.setBitmap(
                bitmap,
                null,
                true,
                WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            )

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun getRandomWallpaper(): Wallpaper? {
        val targetPage = (1..10).random()
        val result = wallpaperRepository.getWallpapers(
            page = targetPage,
            limit = 20,
            categoryId = null,
            query = null,
            sort = WallpaperSort.RANDOM
        ).getOrNull()

        return result?.items?.randomOrNull() ?: if (targetPage != 1) {
            wallpaperRepository.getWallpapers(
                page = 1,
                limit = 20,
                categoryId = null,
                query = null,
                sort = WallpaperSort.RANDOM
            ).getOrNull()?.items?.randomOrNull()
        } else null
    }

    private suspend fun getFavoriteWallpaper(): Wallpaper? {
        return libraryRepository.observeFavorites().first().randomOrNull()
    }

    private suspend fun getCategoryWallpaper(categoryId: Int?): Wallpaper? {
        if (categoryId == null) return getRandomWallpaper()
        val result = wallpaperRepository.getWallpapers(
            page = 1,
            limit = 50,
            categoryId = categoryId,
            query = null,
            sort = WallpaperSort.RANDOM
        ).getOrNull()
        return result?.items?.randomOrNull()
    }
}
