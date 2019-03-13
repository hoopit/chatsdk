package io.hoopit.chatsdk.realtimeadapter.dao

import io.hoopit.chatsdk.realtimeadapter.FirebasePaths
import io.hoopit.chatsdk.realtimeadapter.repository.Message
import io.hoopit.android.firebaserealtime.paging.FirebaseDaoBase
import io.hoopit.android.firebaserealtime.paging.FirebaseDataSourceFactory

class FirebaseMessageDao private constructor() : FirebaseDaoBase<Long, Message>(
    Message::class, 10000) {

    fun getMessages(it: String): FirebaseDataSourceFactory<Long, Message> {
        return getPagedList(FirebasePaths.threadMessagesRef(it).orderByChild("date"), descending = true) {
            requireNotNull(it.date)
        }
    }

    companion object {
        // TODO: replace with DI
        val instance by lazy { FirebaseMessageDao() }
    }
}
