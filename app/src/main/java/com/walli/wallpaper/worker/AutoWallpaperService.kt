package com.walli.wallpaper.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.walli.wallpaper.R
import com.walli.wallpaper.domain.model.WallpaperSort
import com.walli.wallpaper.domain.repository.WallpaperRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.net.URL
import javax.inject.Inject

@AndroidEntryPoint
class AutoWallpaperService : Service() {

    @Inject
    lateinit var repository: WallpaperRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startWallpaperRotation()
        return START_STICKY
    }

    private fun startWallpaperRotation() {
        job?.cancel()
        job = serviceScope.launch {
            while (isActive) {
                try {
                    changeWallpaper()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(4 * 60 * 60 * 1000L) // 4 hours
            }
        }
    }

    private suspend fun changeWallpaper() {
        val targetPage = (1..10).random()
        var wallpapersResult = repository.getWallpapers(
            page = targetPage,
            limit = 20,
            categoryId = null,
            query = null,
            sort = WallpaperSort.RANDOM
        )

        var wallpapers = wallpapersResult.getOrNull()?.items ?: emptyList()

        if (wallpapers.isEmpty() && targetPage != 1) {
            wallpapersResult = repository.getWallpapers(
                page = 1,
                limit = 20,
                categoryId = null,
                query = null,
                sort = WallpaperSort.RANDOM
            )
            wallpapers = wallpapersResult.getOrNull()?.items ?: return
        }

        if (wallpapers.isEmpty()) return

        val randomWallpaper = wallpapers.random()
        val url = URL(randomWallpaper.imageUrl)
        val connection = url.openConnection()
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        val input = connection.getInputStream()
        val bitmap = BitmapFactory.decodeStream(input)

        if (bitmap != null) {
            val wallpaperManager = WallpaperManager.getInstance(applicationContext)
            wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Auto Wallpaper Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Auto Wallpaper")
            .setContentText("Rotating wallpapers every 4 hours")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your app icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "auto_wallpaper_channel"
    }
}
