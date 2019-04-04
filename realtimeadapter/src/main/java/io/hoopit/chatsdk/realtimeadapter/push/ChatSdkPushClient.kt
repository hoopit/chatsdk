package io.hoopit.chatsdk.realtimeadapter.push

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.hoopit.chatsdk.realtimeadapter.Config
import io.hoopit.chatsdk.realtimeadapter.Config.chatSdkMessageChannel
import io.hoopit.chatsdk.realtimeadapter.R

class ChatSdkPushClient {

    /**
     * Set the currently active thread to avoid showing notifications for this thread
     */
    var activeThreadId: String? = null

    fun handleMessage(application: Application, data: Map<String, String>): Boolean {
        if (!isChatPush(data)) return false
//        if (!AppBackgroundMonitor.shared().inBackground()) return true
        val threadId = data[PUSH_THREAD_ID]
        if (threadId == activeThreadId) return true
        // TODO: Filter on sender ?
        val senderId = data[PUSH_USER_ID]
        val title = data[PUSH_TITLE] ?: ""
        val body = data[PUSH_BODY] ?: ""
        val intent = Intent(application, Config.pushActivity.java)
        intent.putExtra(Config.EXTRA_THREAD_ID, threadId)
//        intent.action = threadId
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        createMessageNotification(
            application,
            intent,
            threadId.hashCode(),
            senderId,
            title,
            body
        )
        return true
    }

    private fun isChatPush(data: Map<String, String>) = data.containsKey(PUSH_BODY)

    fun createMessageNotification(
        context: Context,
        resultIntent: Intent,
        messageNotificationId: Int,
        senderId: String?,
        title: String,
        message: String
    ) {

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createNotificationChannel(context)

        val pendingIntent =
            PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        val builder = NotificationCompat.Builder(context, Config.chatSdkMessageChannel)
            .setContentTitle(title)
            .setContentText(message)
            .setVibrate(longArrayOf(0, 250, 100, 250))
            .setSound(alarmSound)
//            .setNumber(number)
            .setContentIntent(pendingIntent)
            .setTicker("$title: $message")
            .setAutoCancel(true)
//            .setColor(ContextCompat.getColor(context, pushNotificationColor))

        Config.pushNotificationIcon?.let { builder.setSmallIcon(it) }
        Config.notificationLedColor?.let { builder.setLights(it, 5000, 500) }

        // TODO: set senders profile picture
        //                val subscribe = ImageBuilder.bitmapForURL(context, user.avatarURL).subscribe { bitmap, throwable ->
//                    if (throwable != null) {
//                        ChatSDK.logError(throwable)
//                    }
//                    handler.createAlertNotification(
//                        context,
//                        resultIntent,
//                        title,
//                        message,
//                        bitmap ?: largePushIcon,
//                        smallPushIcon,
//                        alarmSound,
//                        -1
//                    )
//                }

        Config.pushNotificationIcon?.let {
            val largePushIcon = BitmapFactory.decodeResource(context.resources, it)
            if (largePushIcon != null) {
//            builder.setLargeIcon(
//                ImageUtils.scaleImage(
//                    largeIcon,
//                    (context.resources.displayMetrics.density * 48).toInt()
//                )
//            )
            }
        }

        val notification = builder.build()

        notification.flags = Notification.FLAG_AUTO_CANCEL

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(messageNotificationId, notification)

        // TODO: check this
//        wakeScreen(context)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createNotificationChannel(
        context: Context
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(chatSdkMessageChannel) != null) return

        val name = context.getString(R.string.chatsdk_notification_channel_name)
        val description = context.getString(R.string.chatsdk_notification_channel_description)

        val channel = NotificationChannel(chatSdkMessageChannel, name, NotificationManager.IMPORTANCE_DEFAULT)
        channel.enableVibration(true)
        channel.enableLights(true)
        channel.description = description

        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Waking up the screen
     * * *  */
    private fun wakeScreen(context: Context) {

        // Waking the screen so the user will see the notification
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        val isScreenOn: Boolean

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT_WATCH)
            isScreenOn = pm.isScreenOn
        else
            isScreenOn = pm.isInteractive

        if (!isScreenOn) {

            val wl = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK
                    or PowerManager.ON_AFTER_RELEASE
                    or PowerManager.ACQUIRE_CAUSES_WAKEUP, "chat-sdk:MyLock"
            )

            wl.acquire(5000)
            wl.release()
        }
    }

    companion object {
        private const val PUSH_USER_ID = "chat_sdk_user_entity_id"
        private const val PUSH_THREAD_ID = "chat_sdk_thread_entity_id"
        private const val PUSH_TITLE = "chat_sdk_push_title"
        private const val PUSH_BODY = "chat_sdk_push_body"
    }
}
