package io.hoopit.chatsdk.realtimeadapter.repository

import io.hoopit.android.common.liveData
import io.hoopit.android.firebaserealtime.ext.orderByChildProperty
import io.hoopit.android.firebaserealtime.lifecycle.FirebaseListLiveData
import io.hoopit.chatsdk.realtimeadapter.FirebasePaths
import io.hoopit.chatsdk.realtimeadapter.requireUserId
import io.hoopit.chatsdk.realtimeadapter.resource.FirebaseResource
import io.hoopit.chatsdk.realtimeadapter.resource.IResource

class ThreadRepository {

    companion object {
        val instance = ThreadRepository()
    }

    fun getPrivateThreads(): IResource<List<Thread>> {
        val list = FirebaseListLiveData(
            FirebasePaths.userThreadsRef(requireUserId()),
            Thread::class,
            10000
        ).orderByChildProperty(
            { it.lastMessage },
            { requireNotNull(it.date) }
        )
        return FirebaseResource(list)
//        return FirebaseResource(dao.getUserThreads(requireNotNull(FirebaseAuth.getInstance().uid)))
    }

    fun getThread(it: String): IResource<Thread> {
        val thread = Thread()
            .apply { with(FirebasePaths.threadRef(it)) }
        return FirebaseResource(liveData(thread))

//        return FirebaseResource(FirebaseValueLiveData(FirebasePaths.threadRef(it), Thread::class, 10000))
//        return FirebaseResource(dao.getThread(it))
    }
}

