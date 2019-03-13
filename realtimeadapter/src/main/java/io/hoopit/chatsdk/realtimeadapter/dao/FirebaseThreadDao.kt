//package co.chatsdk.firebase.room.dao
//
//import androidx.lifecycle.LiveData
//import io.hoopit.chatsdk.realtimeadapter.FirebasePaths
//import io.hoopit.chatsdk.realtimeadapter.Thread
//import io.hoopit.android.firebaserealtime.paging.FirebaseDaoBase
//
//class FirebaseThreadDao private constructor() : FirebaseDaoBase<String, Thread>(Thread::class, 10000) {
//
//    fun getUserThreads(userId: String): LiveData<List<Thread>> {
//        val list = getList(FirebasePaths.userThreadsRef(userId).orderByKey()) { it.entityId }
//        return list.orderByChild(
//            { it.lastMessage },
//            { requireNotNull(it.date) }
//        )
//    }
//
//    fun getThread(threadId: String): LiveData<Thread?> {
//        TODO()
////        val thread = Thread().also {
////            it.entityId = threadId
////            it.init(FirebaseScope.defaultInstance, FirebasePaths.threadRef(threadId))
////        }
////        return liveData(thread)
//    }
//
//    companion object {
//
//        // TODO: replace with DI
////        val instance by lazy { FirebaseThreadDao() }
//    }
//}
