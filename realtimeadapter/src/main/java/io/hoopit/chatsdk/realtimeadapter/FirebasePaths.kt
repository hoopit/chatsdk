package io.hoopit.chatsdk.realtimeadapter

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

object FirebasePaths {

    private const val UsersPath = "users"
    private const val MessagesPath = "messages"
    private const val ThreadsPath = "threads"
    private const val PublicThreadsPath = "public-threads"
    private const val DetailsPath = "details"
    private const val IndexPath = "searchIndex"
    private const val OnlinePath = "online"
    private const val MetaPath = "meta"
    private const val FollowersPath = "followers"
    private const val FollowingPath = "follows"
    private const val Image = "imaeg"
    private const val Thumbnail = "thumbnail"
    private const val UpdatedPath = "updated"
    private const val LastMessagePath = "lastMessage"
    private const val TypingPath = "typing"
    //    private const val ReadPath = Keys.Read
    private const val LocationPath = "location"

    fun firebaseInstance(): FirebaseDatabase {
        val url = Config.databaseUrl
        return if (url != null)
            FirebaseDatabase.getInstance(url)
        else
            FirebaseDatabase.getInstance()
    }

    fun firebaseRawRef(): DatabaseReference {
        return firebaseInstance().reference
    }

    fun firebaseRef(): DatabaseReference {
        return firebaseRawRef()
            .child(Config.rootPath)
    }

    fun connectedRef() = firebaseRawRef().child(".info/connected")

    /* Users */
    /** @return The users main ref.
     */
    fun usersRef(): DatabaseReference {
        return firebaseRef()
            .child(UsersPath)
    }

    /** @return The user ref for given id.
     */
    fun userRef(firebaseId: String): DatabaseReference {
        return usersRef().child(firebaseId)
    }

    /** @return The user threads ref.
     */
    fun userThreadsRef(firebaseId: String): DatabaseReference {
        return usersRef()
            .child(firebaseId).child(ThreadsPath)
    }

    /** @return The user meta ref for given id.
     */
    fun userMetaRef(firebaseId: String): DatabaseReference {
        return usersRef()
            .child(firebaseId).child(MetaPath)
    }

    fun userOnlineRef(firebaseId: String): DatabaseReference {
        return userRef(firebaseId)
            .child(OnlinePath)
    }

    fun userFollowingRef(firebaseId: String): DatabaseReference {
        return userRef(firebaseId)
            .child(FollowingPath)
    }

    fun userFollowersRef(firebaseId: String): DatabaseReference {
        return userRef(firebaseId)
            .child(FollowersPath)
    }

    /* Threads */
    /** @return The thread main ref.
     */
    fun threadRef(): DatabaseReference {
        return firebaseRef()
            .child(ThreadsPath)
    }

    /** @return The thread ref for given id.
     */
    fun threadRef(firebaseId: String): DatabaseReference {
        return threadRef().child(firebaseId)
    }

    fun threadUsersRef(firebaseId: String): DatabaseReference {
        return threadRef().child(firebaseId).child(
            UsersPath
        )
    }

    fun threadDetailsRef(firebaseId: String): DatabaseReference {
        return threadRef().child(firebaseId).child(
            DetailsPath
        )
    }

    fun threadUpdatedRef(firebaseId: String): DatabaseReference {
        return threadRef().child(firebaseId).child(
            UpdatedPath
        )
    }

    fun threadLastMessageRef(firebaseId: String): DatabaseReference {
        return threadRef().child(firebaseId).child(
            LastMessagePath
        )
    }

    fun threadMessagesRef(firebaseId: String): DatabaseReference {
        return threadRef(firebaseId)
            .child(MessagesPath)
    }

    fun threadMetaRef(firebaseId: String): DatabaseReference {
        return threadRef(firebaseId)
            .child(MetaPath)
    }

    fun publicThreadsRef(): DatabaseReference {
        return firebaseRef()
            .child(PublicThreadsPath)
    }

    fun onlineRef(userEntityID: String): DatabaseReference {
        return firebaseRef()
            .child(OnlinePath).child(userEntityID)
    }

    /* Index */
    fun indexRef(): DatabaseReference {
        return firebaseRef()
            .child(IndexPath)
    }
}
