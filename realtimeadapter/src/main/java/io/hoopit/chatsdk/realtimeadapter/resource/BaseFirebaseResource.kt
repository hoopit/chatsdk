package io.hoopit.chatsdk.realtimeadapter.resource

import androidx.lifecycle.LiveData

abstract class BaseFirebaseResource<RemoteType> :
    IResource<RemoteType> {
}


class FirebaseResource<T>(override val data: LiveData<T>) : BaseFirebaseResource<T>()
