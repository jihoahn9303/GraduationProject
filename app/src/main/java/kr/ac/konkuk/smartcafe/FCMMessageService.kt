package kr.ac.konkuk.smartcafe

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMMessageService : FirebaseMessagingService() {
    override fun onMessageReceived(p0: RemoteMessage) {
        Log.d("arrive", "Message Arrive")
        Log.d("arrived_data", "${p0.data}")
        Log.d("arrived_title", "${p0.data["title"]}")
        Log.d("arrived_message", "${p0.data["message"]}")
        if(p0.data != null){
            SendPhotos.category = p0.data["message"]
            Log.d("category", "${SendPhotos.category}")
            sendNotification(p0.data["title"], p0.data["message"])
        }
    }

    private fun sendNotification(title: String?, message: String?){
        createNotificationChannel(this, NotificationManagerCompat.IMPORTANCE_DEFAULT, false,
            getString(R.string.app_name), "App notification channel")
        val channelId = "$packageName-${getString(R.string.app_name)}"
        val text = "Hello, you get $message category!"

        val intent = Intent(this@FCMMessageService, SetOptions::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this@FCMMessageService, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this@FCMMessageService, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(defaultSound)
            .setContentIntent(pendingIntent)

        val notificationManager = NotificationManagerCompat.from(this@FCMMessageService)
        notificationManager.notify(0, notificationBuilder.build())
    }

    private fun createNotificationChannel(context: Context, importance: Int, showBadge: Boolean,
                                          name: String, description: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "${context.packageName}-$name"
            val channel = NotificationChannel(channelId, name, importance)
            channel.description = description
            channel.setShowBadge(showBadge)

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}