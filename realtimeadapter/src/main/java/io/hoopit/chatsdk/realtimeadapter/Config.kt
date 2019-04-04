package io.hoopit.chatsdk.realtimeadapter

import android.app.Activity
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import kotlin.reflect.KClass

object Config {

    var databaseUrl: String? = null
    var rootPath: String = ""
    lateinit var pushActivity: KClass<out Activity>

    @DrawableRes
    var pushNotificationIcon: Int? = null

    @ColorInt
    var notificationLedColor: Int? = null

    var chatSdkMessageChannel = "io.hoopit.chatsdk"

    const val EXTRA_THREAD_ID = "io.hoopit.chatsdk.thread_id"
}
