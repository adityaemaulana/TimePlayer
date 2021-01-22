package com.example.timeplayer.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.timeplayer.MainActivity
import com.example.timeplayer.R
import com.example.timeplayer.service.MediaService.Companion.ACTION_NOTIFICATION_PLAY
import com.example.timeplayer.service.MediaService.Companion.TIMER_BROADCAST

object NotificationUtil {
    private const val CHANNEL_DEFAULT_IMPORTANCE = "channel_timer"
    const val KEY_NOTIFICATION_ID = 100

    fun createNotification(context: Context, currentTime: String, isPlay: Boolean): Notification {
        val contentIntent = Intent(context, MainActivity::class.java)
        contentIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val contentPendingIntent = PendingIntent.getActivity(context, 0, contentIntent, 0)

        val broadcastIntent = Intent(TIMER_BROADCAST)
        broadcastIntent.action = ACTION_NOTIFICATION_PLAY
        val playPendingIntent = PendingIntent.getBroadcast(
            context, 0, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        val playActionString =
            if (isPlay) context.getString(R.string.pause) else context.getString(R.string.resume)
        val playAction =
            NotificationCompat.Action.Builder(0, playActionString, playPendingIntent).build()

        val notification = NotificationCompat.Builder(context, CHANNEL_DEFAULT_IMPORTANCE)
            .setContentText(currentTime)
            .setSmallIcon(R.drawable.ic_notification_music)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .addAction(playAction)
            .build()

        val mNotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_DEFAULT_IMPORTANCE, "TimePlayer",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.setShowBadge(false)
            channel.setSound(null, null)
            mNotificationManager.createNotificationChannel(channel)
        }

        return notification
    }

    fun updateNotification(context: Context, currentTime: String, isPlay: Boolean) {
        val notification = createNotification(context, currentTime, isPlay)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(KEY_NOTIFICATION_ID, notification)
    }
}