package io.hoopit.chatsdk.realtimeadapter.repository

import androidx.paging.PagedList
import io.hoopit.chatsdk.realtimeadapter.FirebasePaths
import io.hoopit.chatsdk.realtimeadapter.dao.FirebaseMessageDao
import io.hoopit.chatsdk.realtimeadapter.resource.FirebasePagedResource
import io.hoopit.chatsdk.realtimeadapter.resource.IResource

class MessageRepository {

    val dao = FirebaseMessageDao.instance

    fun getMessagesInThread(threadId: String): IResource<PagedList<Message>> {
        return FirebasePagedResource.create(
            FirebasePaths.threadMessagesRef(threadId).orderByChild("date"),
            descending = true
        )
    }
}
  
