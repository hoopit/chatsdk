package io.hoopit.chatsdk.realtimeadapter.resource

import androidx.lifecycle.LiveData

interface IResource<T> {

    val data: LiveData<T>

}
