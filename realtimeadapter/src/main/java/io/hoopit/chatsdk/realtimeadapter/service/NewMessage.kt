package io.hoopit.chatsdk.realtimeadapter.service

import com.google.firebase.database.PropertyName
import com.google.firebase.database.ServerValue
import io.hoopit.chatsdk.realtimeadapter.repository.Message

class NewMessage(
    @get:PropertyName("user-firebase-id")
    @set:PropertyName("user-firebase-id")
    var userFirebaseId: String,
    val payload: String
) {
    val type = 0
    val date: MutableMap<String, String> = ServerValue.TIMESTAMP
    val json_v2 = mapOf("text" to payload)
    val read = mapOf(
        userFirebaseId to Message.ReadStatus(Message.ReadStatus.READ)
    )
}
