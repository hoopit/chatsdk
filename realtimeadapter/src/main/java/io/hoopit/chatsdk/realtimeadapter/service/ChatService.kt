package io.hoopit.chatsdk.realtimeadapter.service

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.PropertyName
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import io.hoopit.android.common.mediatorLiveData
import io.hoopit.android.common.switchMap
import io.hoopit.android.firebaserealtime.lifecycle.FirebaseListLiveData
import io.hoopit.chatsdk.realtimeadapter.FirebasePaths
import io.hoopit.chatsdk.realtimeadapter.getUserId
import io.hoopit.chatsdk.realtimeadapter.repository.Thread
import io.hoopit.chatsdk.realtimeadapter.requireUserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ChatService {

    companion object {
        val instance = ChatService()
    }

    fun send(payload: String, threadId: String) {
        if (payload.isBlank()) return
        val message = NewMessage(
            userFirebaseId = requireUserId(),
            payload = payload
        )
        FirebasePaths.threadMessagesRef(threadId).push().setValue(message)
    }

    fun getUnreadThreadCount(): LiveData<Int> {
        val list = FirebaseListLiveData(
            FirebasePaths.userThreadsRef(requireUserId()),
            Thread::class,
            10000
        )
        return list.switchMap { threadList ->
            val map = mutableMapOf<String, MediatorLiveData<Boolean>>()
            val numUnread = mediatorLiveData(0)

            threadList.forEach { thread ->
                val unreadLiveData = map.getOrPut(thread.entityId) {
                    val live = mediatorLiveData(false)
                    numUnread.addSource(live) {
                        numUnread.value = map.values.sumBy {
                            if (it.value == true) 1 else 0
                        }
                    }
                    live
                }
                unreadLiveData.addSource(thread.isUnread) {
                    unreadLiveData.value = it == true
                }
            }
            return@switchMap numUnread
        }
    }

    fun setOffline(scope: CoroutineScope) = scope.launch {
        val user = FirebaseAuth.getInstance().currentUser ?: return@launch
        launch {
            FirebasePaths.userOnlineRef(user.uid).setValue(false)
        }
        launch {
            FirebasePaths.onlineRef(user.uid).apply {
                removeValue()
            }
        }
    }

    fun setOnline(scope: CoroutineScope) = scope.launch {
        val user = FirebaseAuth.getInstance().currentUser ?: return@launch
        launch {
            FirebasePaths.userOnlineRef(user.uid).apply {
                onDisconnect().setValue(false)
                safeSetValue(true)
            }
        }

        val onlineData = mapOf(
            "time" to ServerValue.TIMESTAMP,
            "uid" to user.uid
        )

        launch {
            FirebasePaths.onlineRef(user.uid).apply {
                onDisconnect().removeValue()
                safeSetValue(onlineData)
            }
        }
    }

    suspend fun createThread(userIds: Collection<String>, threadName: String? = null): String? {
        val userSet = userIds.toHashSet()
        userSet.add(requireUserId())
//        require(userSet.size > 1) { "Need at least two users to create a thread." }
        return when (userSet.size) {
            1 -> null
            2 -> createPrivateThread(userSet.toList())
            else -> createGroupThread(userSet.toList(), requireNotNull(threadName))
        }
    }

    private suspend fun createGroupThread(toList: List<String>, threadName: String): String {
        val newId = pushThread(NewThread(threadName, type = 1))
        addUsers(newId, toList.toHashSet().toList())
        return newId
    }

    private suspend fun createPrivateThread(userIds: List<String>): String {
        val otherUserId = userIds.firstOrNull { it != getUserId() }
            ?: throw IllegalArgumentException("Cannot create thread with self")
        val existingThreadId = tryFindExistingThread(otherUserId)
        if (existingThreadId != null) return existingThreadId
        val newId = pushThread((NewThread("")))
        addUsers(newId, userIds)
        return newId
    }

    suspend fun addUsers(threadId: String, userIds: List<String>): List<String> {
        userIds.forEach {
            addUser(threadId, it)
        }
        return userIds
    }

    private suspend fun addUser(threadId: String, userId: String, status: String? = null): String {
        return suspendCoroutine { c ->
            val realStatus = status ?: if (requireUserId() == userId) "owner" else "member"
            FirebasePaths.threadUsersRef(threadId)
                .updateChildren(
                    mapOf(userId to ThreadParticipant(realStatus))
                ) { databaseError: DatabaseError?, _: DatabaseReference ->
                    databaseError?.let {
                        c.resumeWithException(it.toException())
                    } ?: c.resume(userId)
                }
        }
    }

    private suspend fun pushThread(thread: NewThread): String {
        val threadRef = FirebasePaths.threadRef().push()
        return suspendCoroutine { continuation ->
            threadRef.setValue(thread).addOnCompleteListener {
                if (it.isSuccessful) continuation.resume(requireNotNull(threadRef.key))
                else continuation.resumeWithException(requireNotNull(it.exception))
            }
        }
    }

    private suspend fun tryFindExistingThread(otherUserId: String): String? =
        suspendCoroutine { continuation ->

            // TODO: refactor with extension functions
            FirebasePaths.userThreadsRef(requireUserId()).orderByChild("type").endAt(0.0)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) {
                        Timber.w(error.toException())
                    }

                    override fun onDataChange(ownThreads: DataSnapshot) {
                        if (!ownThreads.hasChildren()) {
                            continuation.resume(null)
                            return
                        }
                        FirebasePaths.userThreadsRef(otherUserId).orderByChild("type").endAt(0.0)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onCancelled(error: DatabaseError) {
                                    Timber.w(error.toException())
                                }

                                override fun onDataChange(otherThreads: DataSnapshot) {
                                    val exists = ownThreads.children.firstOrNull {
                                        otherThreads.hasChild(requireNotNull(it.key))
                                    }
                                    continuation.resume(exists?.key)
                                }
                            })
                    }
                })
        }

    fun deleteMessage(entityId: String) {
    }
}

data class ThreadInvitation(val invitedById: String)

data class ThreadParticipant(val status: String)

@Suppress("unused")
class NewThread(name: String, type: Int = 0) {
    val details = ThreadDetails(name, type)
    val users = listOf<String>()
    val updated = Updated()

    data class Updated(
        val users: MutableMap<String, String?> = ServerValue.TIMESTAMP,
        val details: MutableMap<String, String?> = ServerValue.TIMESTAMP
    )

    data class ThreadDetails(val name: String, val type: Int) {

        @get:PropertyName("type_v4")
        val typeV4: Int = if (type == 0) 2 else 1

        @get:PropertyName("creator-entity-id")
        val creatorEntityId = requireUserId()

        val creationDate: MutableMap<String, String?> = ServerValue.TIMESTAMP
    }
}

suspend fun DatabaseReference.safeSetValue(value: Any?) = suspendCoroutine<Boolean> {
    setValue(value) { error, _ ->
        it.resume(error == null)
    }
}

suspend fun DatabaseReference.trySetValue(value: Any?) = suspendCoroutine<Unit> {
    setValue(value) { error, _ ->
        if (error == null) it.resume(Unit)
        else it.resumeWithException(error.toException())
    }
}

