package io.hoopit.chatsdk.realtimeadapter.service

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.PropertyName
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import io.hoopit.android.firebaserealtime.ext.updateChildren
import io.hoopit.chatsdk.realtimeadapter.FirebasePaths
import io.hoopit.chatsdk.realtimeadapter.getUserId
import io.hoopit.chatsdk.realtimeadapter.repository.Message
import io.hoopit.chatsdk.realtimeadapter.requireUserId
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ChatService {

    companion object {
        val instance = ChatService()
    }

    class NewMessage(
        @get:PropertyName("user-firebase-id")
        @set:PropertyName("user-firebase-id")
        var userFirebaseId: String,
        payload: String
    ) {
        val type = 0
        val date: MutableMap<String, String> = ServerValue.TIMESTAMP
        val json_v2 = mapOf("text" to payload)
        val read = mapOf(
            userFirebaseId to Message.ReadStatus(Message.ReadStatus.READ)
        )
    }

    fun send(payload: String, threadId: String) {
        if (payload.isBlank()) return
        val message = NewMessage(
            userFirebaseId = requireUserId(),
            payload = payload
        )
        FirebasePaths.threadMessagesRef(threadId).push().setValue(message)
    }

    suspend fun setOffline() = coroutineScope {
        val user = FirebaseAuth.getInstance().currentUser ?: return@coroutineScope
        launch {
            FirebasePaths.userOnlineRef(user.uid).apply {
                removeValue()
            }
        }
        launch {
            FirebasePaths.onlineRef(user.uid).apply {
                removeValue()
            }
        }
    }

    suspend fun setOnline() = coroutineScope {
        val user = FirebaseAuth.getInstance().currentUser ?: return@coroutineScope
        launch {
            FirebasePaths.userOnlineRef(user.uid).apply {
                onDisconnect().removeValue()
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

    suspend fun createThread(userIds: Collection<String>, threadName: String? = null): String {
        // TODO: add user to list
        val userSet = userIds.toHashSet()
        userSet.add(requireUserId())
        return if (userSet.size == 2) createPrivateThread(userSet.toList())
        else createGroupThread(userSet.toList(), threadName)
    }

    private suspend fun createGroupThread(toList: List<String>, threadName: String?): String {
        TODO("not implemented")
    }

    private suspend fun createPrivateThread(userIds: List<String>): String {
        val existingThreadId = getExistingThreadId(userIds)
        if (existingThreadId != null) return existingThreadId
        val newId = pushThread((CreateThread("")))
        addUsers(newId, userIds)
        return newId
    }

    private suspend fun addUsers(threadId: String, userIds: List<String>): List<String> {
        userIds.forEach {
            addUser(threadId, it)
        }
        return userIds
    }

    private suspend fun addUser(threadId: String, userId: String, status: String? = null): String {
        return suspendCoroutine { c ->
            val realStatus = status ?: if (requireUserId() == userId) "owner" else "member"

            FirebasePaths.firebaseRawRef().updateChildren(
                FirebasePaths.userThreadsRef(userId).child(threadId) to ThreadInvitation(
                    requireUserId()
                ),
                FirebasePaths.threadUsersRef(threadId).child(userId) to ThreadParticipant(
                    realStatus
                )
            ).addOnCompleteListener {
                if (it.isSuccessful) {
                    c.resume(userId)
                } else {
                    c.resumeWithException(requireNotNull(it.exception))
                }
            }
        }
    }

    private suspend fun pushThread(thread: CreateThread): String {
        val threadRef = FirebasePaths.threadRef().push()
        return suspendCoroutine { continuation ->
            threadRef.setValue(thread).addOnCompleteListener {
                if (it.isSuccessful) continuation.resume(requireNotNull(threadRef.key))
                else continuation.resumeWithException(requireNotNull(it.exception))
            }
        }
    }

    private suspend fun getExistingThreadId(users: List<String>): String? {
        val otherUserId = users.firstOrNull { it != getUserId() }
            ?: throw IllegalArgumentException("Cannot create thread with self")

        return suspendCoroutine { continuation ->
            FirebasePaths.userThreadsRef(requireUserId())
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) {
                        Timber.w(error.toException())
                    }

                    override fun onDataChange(ownThreads: DataSnapshot) {
                        if (!ownThreads.hasChildren()) {
                            continuation.resume(null)
                            return
                        }
                        FirebasePaths.userThreadsRef(otherUserId)
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
    }

    fun deleteThread(id: String) {
        TODO("not implemented")
    }

    fun deleteMessage(entityId: String) {
    }
}

data class ThreadInvitation(val invitedById: String)

data class ThreadParticipant(val status: String)

class CreateThread(name: String) {
    val details = ThreadDetails(name)
    val users = listOf<String>()
    val updated = Updated()

    class Updated {
        var users = ServerValue.TIMESTAMP
        var details = ServerValue.TIMESTAMP
    }

    class ThreadDetails(var name: String) {
        val type = 0
        val type_v4 = 2

        @get:PropertyName("creator-entity-id")
        @set:PropertyName("creator-entity-id")
        var creatorEntityId = requireUserId()

        var creationDate = ServerValue.TIMESTAMP
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

