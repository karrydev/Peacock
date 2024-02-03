package com.karrydev.fasttouch.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Builder
import com.karrydev.fasttouch.R
import com.karrydev.fasttouch.ui.MainActivity
import com.karrydev.fasttouch.util.appContext
import com.karrydev.fasttouch.util.appName

class ForegroundService : Service() {

    private var notificationManager: NotificationManager? = null

    companion object {
        const val TAG = "ForegroundService"
        const val CHANNEL_ID = "Fast_Touch_Channel_ID"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        notificationManager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Fast Touch Service Channel", NotificationManager.IMPORTANCE_LOW)
            channel.description = "${appName}的前台守护进程"
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        // 设置通知的点击跳转
        val serviceIntent = Intent(this, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            serviceIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = Builder(this, CHANNEL_ID)
            // 设置通知的小图标
            .setSmallIcon(R.mipmap.ic_launcher)
            // 设置通知的标题
            .setContentTitle(appName)
            // 设置通知等级
            .setPriority(NotificationCompat.PRIORITY_LOW)
            // 设置通知内容
            .setContentText("进程守护中")
            // 在Android O及以上设置通知的子文本
            .setSubText("Sub Text")
            // 设置可点击跳转
            .setContentIntent(pendingIntent)
            // 设置无法划动清除。
            // 这个属性在Android 14及以上的版本已经无法禁止用户的划动清除，但还是可以避免被一键清除
            .setOngoing(true)

        addBigContentLayout(builder)

        return builder.build()
    }

    private fun addBigContentLayout(builder: Builder) {
        // 设置通知展开的内容布局
        val layout = RemoteViews(appContext.packageName, R.layout.layout_notification_expanded)
        builder.setCustomBigContentView(layout)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationManager?.cancel(NOTIFICATION_ID)
        stopSelf()
    }
}