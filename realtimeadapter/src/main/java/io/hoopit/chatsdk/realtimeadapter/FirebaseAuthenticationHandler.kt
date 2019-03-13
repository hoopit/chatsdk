package io.hoopit.chatsdk.realtimeadapter

import io.hoopit.chatsdk.realtimeadapter.service.ChatService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FirebaseAuthenticationHandler {

    init {
        // Handle out-of-band login/logout
        FirebaseAuth.getInstance().addAuthStateListener {
            it.currentUser?.let {
                GlobalScope.launch {
                    authenticateWithUser(it)
                }
            }
//                ?: internalLogout()
        }
    }

//    private var authCompletable: Completable? = null
//    private var authEmitter: CompletableEmitter? = null
//
//    private var logoutCompletable: Completable? = null
//    private var logoutEmitter: CompletableEmitter? = null

//    override fun authenticateWithCachedToken(): Completable {
//        // Handled automatically by FirebaseAuth if the user hasn't logged out
//        return if (FirebaseAuth.getInstance().currentUser != null) {
//            // Return existing task if internal auth in progress, otherwise complete immediately
//            authCompletable ?: Completable.complete()
//        } else {
//            Completable.error {
//                ChatError.getError(
//                    ChatError.Code.NO_AUTH_DATA,
//                    "No auth bundle found"
//                )
//            }
//        }
//    }
//
//    override fun authenticate(details: AccountDetails): Completable {
//        val completable = authCompletable ?: Completable
//            .create { emitter ->
//                authEmitter = emitter
//                if (isAuthenticating) {
//                    emitter.onError(
//                        ChatError.getError(
//                            ChatError.Code.AUTH_IN_PROCESS,
//                            "Can't execute two auth in parallel"
//                        )
//                    )
//                    return@create
//                }
//
//                authStatus = AuthStatus.AUTH_WITH_MAP
//
//                when (details.type) {
//                    AccountDetails.Type.Username -> FirebaseAuth.getInstance()
//                        .signInWithEmailAndPassword(details.username, details.password)
//                        .addOnFailureListener(emitter::onError)
//
//                    AccountDetails.Type.Register -> FirebaseAuth.getInstance()
//                        .createUserWithEmailAndPassword(details.username, details.password)
//                        .addOnFailureListener(emitter::onError)
//
//                    AccountDetails.Type.Anonymous -> FirebaseAuth.getInstance()
//                        .signInAnonymously()
//                        .addOnFailureListener(emitter::onError)
//
//                    AccountDetails.Type.Custom -> FirebaseAuth.getInstance()
//                        .signInWithCustomToken(details.token)
//                        .addOnFailureListener(emitter::onError)
//
//                    // Should be handled by Social Login Module
//                    AccountDetails.Type.Facebook, AccountDetails.Type.Twitter -> emitter.onError(
//                        ChatError.getError(
//                            ChatError.Code.NO_LOGIN_TYPE,
//                            "No matching login type was found"
//                        )
//                    )
//                    else -> emitter.onError(
//                        ChatError.getError(
//                            ChatError.Code.NO_LOGIN_TYPE,
//                            "No matching login type was found"
//                        )
//                    )
//                }
//            }
//            .doOnError { onLogoutComplete() }
//            .subscribeOn(Schedulers.single())
//        authCompletable = completable
//        return completable
//    }

    private suspend fun authenticateWithUser(user: FirebaseUser) {
        ChatService.instance.setOnline()

//        authenticatedThisSession = true
//        loginInfo = mapOf(AuthKeys.CurrentUserID to user.uid)
//        authStatus = AuthStatus.HANDLING_F_USER

//        val userWrapper = UserWrapper.initWithAuthData(user)

//        val sub = userWrapper.once().subscribe({
//            userWrapper.model.update()
//
//                        FirebaseEventHandler.shared().currentUserOn(userWrapper.model.entityID)
//
//            ChatSDK.hook()?.executeHook(
//                    BaseHookHandler.UserAuthFinished,
//                    hashMapOf(BaseHookHandler.UserAuthFinished_User to userWrapper.model)
//            )
//
//
//
//            userWrapper.push().subscribe(
//                    { authEmitter?.onComplete() },
//                    { authEmitter?.onError(it) }
//            )
//        }, {
//            authEmitter?.onError(it)
//        })
    }

//    override fun userAuthenticated(): Boolean {
//        return FirebaseAuth.getInstance().currentUser != null
//    }

//    override fun changePassword(
//        email: String,
//        oldPassword: String,
//        newPassword: String
//    ): Completable {
//        return Completable.create { emitter ->
//            FirebaseAuth.getInstance().currentUser?.updatePassword(newPassword)
//                ?.addOnCompleteListener {
//                    if (it.isSuccessful) {
//                        emitter.onComplete()
//                    } else {
//                        emitter.onError(getFirebaseError(DatabaseError.fromException(it.exception)))
//                    }
//                }
//        }.subscribeOn(Schedulers.single())
//    }

//    override fun logout(): Completable {
//        val completable = logoutCompletable ?: Completable.create {
//            logoutEmitter = it
//            // AuthStateListener handles actual logout
//            FirebaseAuth.getInstance().signOut()
//        }
//        logoutCompletable = completable
//        return completable
//    }

//    private fun internalLogout() {
//        val user = ChatSDK.currentUser()
//        // Stop listening to user related alerts. (added message or thread.)
////        FirebaseEventHandler.shared().userOff(user.entityID)
//        val sub = ChatSDK.core().setUserOffline()
//            .doOnTerminate(::onLogoutComplete)
//            .subscribe({
//                //                FirebaseAuth.getInstance().signOut()
//
//                removeLoginInfo(AuthKeys.CurrentUserID)
//                ChatSDK.events().source().onNext(NetworkEvent.logout())
//
//                ChatSDK.socialLogin()?.logout()
//
//                ChatSDK.hook()?.executeHook(
//                    BaseHookHandler.Logout,
//                    hashMapOf(BaseHookHandler.Logout_User to user)
//                )
//                authenticatedThisSession = false
//                logoutEmitter?.onComplete()
//            }, {
//                logoutEmitter?.tryOnError(it)
//            })
//    }

//    private fun onAuthComplete() {
//        authEmitter = null
//        logoutCompletable = null
//        logoutEmitter = null
//    }
//
//    private fun onLogoutComplete() {
//        logoutEmitter = null
//        authCompletable = null
//        authEmitter = null
//    }

//    override fun sendPasswordResetMail(email: String): Completable {
//        return Completable.create { emitter ->
//            FirebaseAuth.getInstance()
//                .sendPasswordResetEmail(email)
//                .addOnCompleteListener { task ->
//                    if (task.isSuccessful) {
//                        emitter.onComplete()
//                    } else {
//                        emitter.onError(
//                            getFirebaseError(
//                                DatabaseError.fromException(
//                                    task.exception
//                                )
//                            )
//                        )
//                    }
//                }
//        }.subscribeOn(Schedulers.single())
//    }

    // TODO: Allow users to turn anonymous login off or on in settings
//    override fun accountTypeEnabled(type: AccountDetails.Type): Boolean? {
//        return if (type == AccountDetails.Type.Anonymous) {
//            ChatSDK.config().anonymousLoginEnabled
//        } else if (type == AccountDetails.Type.Username || type == AccountDetails.Type.Register) {
//            true
//        } else if (ChatSDK.socialLogin() != null) {
//            ChatSDK.socialLogin().accountTypeEnabled(type)
//        } else {
//            false
//        }
//    }
}
