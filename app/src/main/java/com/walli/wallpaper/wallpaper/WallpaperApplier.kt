package com.walli.wallpaper.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.os.Build
import com.walli.wallpaper.domain.model.WallpaperTarget
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class WallpaperApplier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    suspend fun apply(
        url: String,
        target: WallpaperTarget,
        scale: Float = 1f,
        offsetX: Float = 0f,
        offsetY: Float = 0f,
        rotation: Float = 0f
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val wallpaperManager = WallpaperManager.getInstance(context)
            if (!wallpaperManager.isWallpaperSupported) {
                error("Wallpapers are not supported on this device")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !wallpaperManager.isSetWallpaperAllowed) {
                error("Wallpaper changes are restricted on this device")
            }

            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) error("Image fetch failed with ${response.code}")
            val stream = response.body?.byteStream() ?: error("Empty image body")
            val originalBitmap = BitmapFactory.decodeStream(stream) ?: error("Unable to decode bitmap")

            val transformedBitmap = transformBitmap(originalBitmap, scale, offsetX, offsetY, rotation)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val flags = when (target) {
                    WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
                    WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
                    WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                }
                wallpaperManager.setBitmap(transformedBitmap, null, true, flags)
            } else {
                wallpaperManager.setBitmap(transformedBitmap)
            }
            Unit
        }
    }

    fun transformBitmap(
        bitmap: Bitmap,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        rotation: Float
    ): Bitmap {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val result = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val matrix = Matrix()

        // 1. Calculate initial scale to fill screen (Center Crop)
        val scaleX = screenWidth.toFloat() / bitmap.width
        val scaleY = screenHeight.toFloat() / bitmap.height
        val baseScale = maxOf(scaleX, scaleY)

        matrix.postScale(baseScale, baseScale)

        // 2. Center the image initially
        val centeredX = (screenWidth - bitmap.width * baseScale) / 2
        val centeredY = (screenHeight - bitmap.height * baseScale) / 2
        matrix.postTranslate(centeredX, centeredY)

        // 3. Apply user transformations
        // Transformations from PreviewScreen are already in pixels
        matrix.postScale(scale, scale, screenWidth / 2f, screenHeight / 2f)
        matrix.postRotate(rotation, screenWidth / 2f, screenHeight / 2f)
        matrix.postTranslate(offsetX, offsetY)

        canvas.drawBitmap(bitmap, matrix, null)
        return result
    }
}
