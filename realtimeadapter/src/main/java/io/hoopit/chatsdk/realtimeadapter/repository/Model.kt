package io.hoopit.chatsdk.realtimeadapter.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.map
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName
import io.hoopit.android.common.mapUpdate
import io.hoopit.android.common.switchMap
import io.hoopit.android.firebaserealtime.core.FirebaseCompositeResource
import io.hoopit.android.firebaserealtime.core.FirebaseScopedResource
import io.hoopit.android.firebaserealtime.model.firebaseList
import io.hoopit.android.firebaserealtime.model.firebaseValue
import io.hoopit.android.firebaserealtime.model.map
import io.hoopit.chatsdk.realtimeadapter.FirebasePaths
import io.hoopit.chatsdk.realtimeadapter.requireUserId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// TODO: map of Type -> DatabaseRef
open class Thread : FirebaseCompositeResource(10000) {

    val details by firebaseValue<ThreadDetails> { FirebasePaths.threadDetailsRef(entityId) }

    val lastMessage by firebaseList<Message> {
        FirebasePaths.threadMessagesRef(entityId).orderByChild("date").limitToLast(1)
    }.map { messages -> messages.map { it.firstOrNull() } }

//    val lastMessage by firebaseValue<Message> { FirebasePaths.threadLastMessageRef(entityId) }

    val users by firebaseList<ThreadUser> {
        FirebasePaths.threadUsersRef(entityId).orderByKey()
    }

    val isUnread by lazy {
        lastMessage.map {
            it?.isUnread() ?: false
        }
    }

    val otherUser by lazy { users.mapUpdate { list -> list.firstOrNull { !it.isSelf() } } }

    val userStatus by lazy { otherUser.switchMap { otherUser -> otherUser?.onlineStatus?.map { it != null } } }

    val lastActive by lazy { otherUser.switchMap { otherUser -> otherUser?.onlineStatus?.map { it?.time } } }

    suspend fun leaveThread() = withContext(Dispatchers.IO) {
        suspendCoroutine<Boolean> { c ->
            FirebasePaths.threadUsersRef(entityId).child(requireUserId())
                .removeValue().continueWith { task ->
                    if (task.isSuccessful) {
                        FirebasePaths.userThreadsRef(requireUserId()).child(entityId).removeValue()
                            .continueWith {
                                c.resume(it.isSuccessful)
                            }
                    }
                }
        }
    }

    val isGroupThread by lazy { users.map { it.size > 2 } }

    /**
     * The display name of the conversation.
     * Can be manually set manually, and defaults to a concatenation of all users in the conversation.
     */
    fun getDisplayName(): LiveData<String?> {
        val displayName = MediatorLiveData<String>()
        val names = mutableMapOf<String, String?>()
        val liveData = mutableListOf<LiveData<*>>()
        displayName.addSource(details.map { it?.name }) { threadName ->
            if (threadName.isNullOrBlank()) {
                // TODO: avoid removing this unnecessarily
                displayName.removeSource(users)
                displayName.addSource(users, fun(list: List<ThreadUser>) {
                    names.clear()
                    liveData.forEach { displayName.removeSource(it) }
                    liveData.clear()
                    list.filter { !it.isSelf() }
                        .forEach { entry ->
                            liveData.add(entry.meta)
                            displayName.addSource(entry.meta) {
                                names[entry.entityId] = it?.name
                                displayName.value = names.values.joinToString()
                            }
                        }
                })
            } else {
                displayName.removeSource(users)
                displayName.value = threadName
            }
        }
        return displayName
    }

    /**
     * Whether the other user is typing or not.
     */
    // TODO: return which user is typing
    fun isTyping() = otherUser.map { it?.typing ?: false }

    /**
     * Set the typing status for the current user.
     */
    fun setTyping(typing: Boolean) {
        ref.child("users")
            .child(requireNotNull(FirebaseAuth.getInstance().uid)).apply {
                updateChildren(mapOf("typing" to typing))
                onDisconnect().updateChildren(mapOf("typing" to false))
            }
    }

    /**
     * Set the conversation name.
     * Only valid for group channels.
     */
    suspend fun setName(text: String) = suspendCoroutine<Boolean> { c ->
        ref.child("details").updateChildren(mapOf("name" to text)).continueWith {
            if (it.isSuccessful) c.resume(true)
            else c.resume(false)
        }
    }
}

// TODO: implement paged list delegate
//    val messages by lazy {
//        FirebasePagedListResource(
//                FirebasePaths.threadMessagesRef(requireNotNull(entityId)).orderByChildProperty("date").limitToLast(50),
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

        val lastMessageRef =
            checkNotNull(ref.parent?.parent?.child("lastMessage/read")?.path?.toString())
        val messageRef = checkNotNull(ref.child("read").path?.toString())

        ref.root.updateChildren(
            mapOf(
                "$lastMessageRef/$uid" to ReadStatus(newStatus),
                "$messageRef/$uid" to ReadStatus(newStatus)
            )
        )
    }

    fun isUnread() = read?.contains(requireUserId())?.not() ?: false

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
