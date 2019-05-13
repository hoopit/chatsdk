package io.hoopit.chatsdk.realtimeadapter.repository

import androidx.lifecycle.LiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName
import io.hoopit.android.common.map
import io.hoopit.android.common.mapUpdate
import io.hoopit.android.common.switchMap
import io.hoopit.android.firebaserealtime.core.FirebaseCompositeResource
import io.hoopit.android.firebaserealtime.core.FirebaseScopedResource
import io.hoopit.android.firebaserealtime.model.firebaseList
import io.hoopit.android.firebaserealtime.model.firebaseValue
import io.hoopit.chatsdk.realtimeadapter.FirebasePaths
import io.hoopit.chatsdk.realtimeadapter.requireUserId

// TODO: map of Type -> DatabaseRef
open class Thread : FirebaseCompositeResource(10000) {

    val details by firebaseValue<ThreadDetails> { FirebasePaths.threadDetailsRef(entityId) }

//    val lastMessage by firebaseList<Message> {
//        FirebasePaths.threadMessagesRef(entityId).orderByChild("date").limitToLast(1)
//    }.map { messages -> messages.map { it.firstOrNull() } }

    val lastMessage by firebaseValue<Message> { FirebasePaths.threadLastMessageRef(entityId) }

    val users by firebaseList<ThreadUser> {
        FirebasePaths.threadUsersRef(entityId).orderByKey()
    }

    val otherUser by lazy { users.mapUpdate { list -> list.firstOrNull { !it.isSelf() } } }

    val userStatus by lazy { otherUser.switchMap { otherUser -> otherUser?.onlineStatus?.map { it != null } } }

    val lastActive by lazy { otherUser.switchMap { otherUser -> otherUser?.onlineStatus?.map { it?.time } } }

    fun getDisplayName(): LiveData<String> {
        return users.switchMap { list ->
            list.firstOrNull { !it.isSelf() }?.meta?.map { it?.name ?: "Unknown" }
        }
    }

    fun isTyping() = otherUser.map { it?.typing ?: false }

    fun setTyping(typing: Boolean) {
        ref.child("users")
            .child(requireNotNull(FirebaseAuth.getInstance().uid)).apply {
                updateChildren(mapOf("typing" to typing))
                onDisconnect().updateChildren(mapOf("typing" to false))
            }
    }
}

// TODO: implement paged list delegate
//    val messages by lazy {
//        FirebasePagedListResource(
//                FirebasePaths.threadMessagesRef(requireNotNull(entityId)).orderByChild("date").limitToLast(50),
//                Message::class.java
//        ) {
//            it.entityId ?: throw IllegalStateException("Creation date must be set")
//        }
//    }

class ThreadUser : User() {
    var status: String? = null
    var typing: Boolean = false

    object OrderBy {
        fun key(item: ThreadUser) = item.entityId
    }
}

class ThreadDetails {
    var creationDate: Long? = null
    var name: String? = null
    var type: Int? = null
    var typeV4: Int? = null
}

@IgnoreExtraProperties
class Message : FirebaseScopedResource(10000) {

    companion object {
        const val JSON = "json_v2"
        const val USER_ID = "user-firebase-id"
    }

    @get:PropertyName(JSON)
    @set:PropertyName(JSON)
    var json: JsonPayload? = null

    var date: Long? = null

    val payload: String?
        get() = json?.text

    var read: Map<String, ReadStatus>? = null

    class JsonPayload {
        val text: String? = null
    }

    /**
     * The message type
     */
    var type: Int? = null

    @get:PropertyName(USER_ID)
    @set:PropertyName(USER_ID)
    lateinit var userFirebaseId: String

    val sender by firebaseValue<User> {
        FirebasePaths.userRef(userFirebaseId)
    }

    override fun toString(): String {
        return payload ?: super.toString()
    }

    fun isFromSelf() = userFirebaseId == requireUserId()

    data class ReadStatus(var status: Int? = null) {

        companion object {
            const val HIDE = -1
            const val NONE = 0
            const val DELIVERED = 1
            const val READ = 2
        }
    }

    object OrderBy {
        fun date(item: Message) = requireNotNull(item.date)
    }

    fun markAsRead() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (isFromSelf()) return
        val newStatus = if (isFromSelf()) ReadStatus.READ else ReadStatus.READ
        if (read?.get(uid)?.status == ReadStatus.READ) return
        if (read?.get(uid)?.status == ReadStatus.DELIVERED) return
        ref.child("read")
            .updateChildren(
                mapOf(
                    uid to ReadStatus(
                        newStatus
                    )
                )
            )
    }

    fun isReadByOthers(): Boolean {
        return getReadBy().isNotEmpty()
    }

    fun getReadBy(): List<String> {
        return read?.filter { (key, value) -> key != requireUserId() && value.status == ReadStatus.READ }?.keys
            ?.toList() ?: listOf()
    }
}

@IgnoreExtraProperties
open class User : FirebaseScopedResource(10000) {

    //    val online by firebaseValue<Boolean> { FirebasePaths.userOnlineRef(entityId) }
    val onlineStatus by firebaseValue<OnlineStatus> { FirebasePaths.onlineRef(entityId) }
    val meta by firebaseValue<Meta> { FirebasePaths.userMetaRef(entityId) }

//    val threads by firebaseList<UserThread> { FirebasePaths.userThreadsRef(entityId) }

    fun isSelf() = entityId == requireUserId()

//    class UserThread : Thread() {
//        var invitedBy: String? = null
//
//        object OrderBy {
//            fun key(item: UserThread) = item.entityId
//        }
//    }
}

@IgnoreExtraProperties
class OnlineStatus {

    var time: Long? = null
    var uid: String? = null
}

@IgnoreExtraProperties
class Meta {

    var id: Int? = null
    var name: String? = null

    @PropertyName("last-online")
    var lastOnline: String? = null

}