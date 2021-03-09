package hu.bme.aut.android.hiketracker.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import hu.bme.aut.android.hiketracker.MainActivity
import hu.bme.aut.android.hiketracker.R

class PositionCheckerService : Service() {

    private val NOTIF_FOREGROUND_ID = 8
    private var enabled = false
    private val NOTIFICATION_CHANNEL_ID = "hik_tracker_notifications"
    private val NOTIFICATION_CHANNEL_NAME = "Hike Tracker notifications"

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_FOREGROUND_ID,
            createNotification("Return to app"))

        //TODO logic

        return START_STICKY
    }

    private fun createNotification(text: String): Notification? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        val notificationIntent = Intent(this, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(this,
            NOTIF_FOREGROUND_ID,
            notificationIntent,
            PendingIntent.FLAG_CANCEL_CURRENT)

        return NotificationCompat.Builder(
            this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Hike tracking is in progress")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_stat_directions_walk)
            .setVibrate(longArrayOf(1000))
            .setContentIntent(contentIntent).build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

        notificationManager?.createNotificationChannel(channel)
    }
}