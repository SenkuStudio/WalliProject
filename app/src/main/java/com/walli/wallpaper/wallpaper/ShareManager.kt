package com.walli.wallpaper.wallpaper

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.FileProvider
import com.walli.wallpaper.util.asSafeFileName
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class ShareManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    suspend fun share(
        url: String,
        title: String,
        scale: Float = 1f,
        offsetX: Float = 0f,
        offsetY: Float = 0f,
        rotation: Float = 0f,
        applier: WallpaperApplier? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) error("Share download failed with ${response.code}")
            
            val shareDir = File(context.cacheDir, "shared").apply { mkdirs() }
            val file = File(shareDir, "${title.asSafeFileName()}.jpg")

            if (scale != 1f || offsetX != 0f || offsetY != 0f || rotation != 0f) {
                val stream = response.body?.byteStream() ?: error("Empty image body")
                val originalBitmap = BitmapFactory.decodeStream(stream) ?: error("Unable to decode bitmap")
                val transformedBitmap = applier?.transformBitmap(originalBitmap, scale, offsetX, offsetY, rotation) ?: originalBitmap
                
                file.outputStream().use { output ->
                    transformedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
                }
            } else {
                val bytes = response.body?.bytes() ?: error("Empty share payload")
                file.writeBytes(bytes)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, context.getString(com.walli.wallpaper.R.string.share_wallpaper)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Unit
        }
    }
}
