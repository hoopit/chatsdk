package io.hoopit.chatsdk.realtimeadapter

import com.google.firebase.auth.FirebaseAuth

// TODO: refactor
fun requireUserId(): String {
    return requireNotNull(FirebaseAuth.getInstance().uid)
}

fun getUserId(): String? {
    return FirebaseAuth.getInstance().uid
}
