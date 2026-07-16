package com.walli.wallpaper.wallpaper

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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
class WallpaperDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    suspend fun download(
        url: String,
        title: String,
        scale: Float = 1f,
        offsetX: Float = 0f,
        offsetY: Float = 0f,
        rotation: Float = 0f,
        applier: WallpaperApplier? = null
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val fileName = "${title.asSafeFileName()}-${System.currentTimeMillis()}.jpg"

            val finalBitmap = if (scale != 1f || offsetX != 0f || offsetY != 0f || rotation != 0f) {
                // If there are transformations, we need to process the bitmap
                val request = Request.Builder().url(url).build()
                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) error("Download failed with ${response.code}")
                val stream = response.body?.byteStream() ?: error("Empty image body")
                val originalBitmap = BitmapFactory.decodeStream(stream) ?: error("Unable to decode bitmap")
                
                applier?.transformBitmap(originalBitmap, scale, offsetX, offsetY, rotation) ?: originalBitmap
            } else {
                null
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/Walli",
                    )
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values,
                ) ?: error("Unable to create MediaStore entry")

                context.contentResolver.openOutputStream(uri)?.use { output ->
                    if (finalBitmap != null) {
                        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
                    } else {
                        val request = Request.Builder().url(url).build()
                        val response = okHttpClient.newCall(request).execute()
                        response.body?.byteStream()?.use { it.copyTo(output) }
                    }
                } ?: error("Unable to write download stream")
                uri
            } else {
                val downloadsDir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    .resolve("Walli")
                    .apply { mkdirs() }
                val file = File(downloadsDir, fileName)
                file.outputStream().use { output ->
                    if (finalBitmap != null) {
                        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
                    } else {
                        val request = Request.Builder().url(url).build()
                        val response = okHttpClient.newCall(request).execute()
                        response.body?.byteStream()?.use { it.copyTo(output) }
                    }
                }
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    arrayOf("image/jpeg"),
                    null,
                )
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
            }
        }
    }
}
