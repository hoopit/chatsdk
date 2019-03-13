package io.hoopit.chatsdk.realtimeadapter.resource

import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.google.firebase.database.Query
import io.hoopit.android.firebaserealtime.core.FirebaseScopedResource
import io.hoopit.android.firebaserealtime.core.IFirebaseEntity
import io.hoopit.android.firebaserealtime.paging.FirebaseLivePagedListBoundaryCallback
import io.hoopit.android.firebaserealtime.paging.FirebaseLivePagedListBuilder
import io.hoopit.android.firebaserealtime.paging.FirebasePagedListBoundaryCallback
import io.hoopit.android.firebaserealtime.paging.FirebasePositionalDataSourceFactory
import io.hoopit.android.firebaserealtime.paging.IFirebaseDataSourceFactory
import kotlin.reflect.KClass

class FirebasePagedListResource<Key : Comparable<Key>, LocalType : Any, Type : FirebaseScopedResource>(
    private val factory: IFirebaseDataSourceFactory<Key, Type, LocalType>,
    disconnectDelay: Long,
    private val buildConfig: (PagedList.Config.Builder.() -> Unit) = {
        setPageSize(100)
        setInitialLoadSizeHint(20)
        setPrefetchDistance(1)
        setEnablePlaceholders(false)
    }
) : BaseFirebaseResource<PagedList<LocalType>>() {
    // TODO: assert that query has no limit

    // TODO: refactor
    override val data: LiveData<PagedList<LocalType>> by lazy {
        val pagedListConfig = defaultConfig.apply(buildConfig).build()
        val builder = FirebaseLivePagedListBuilder(factory, pagedListConfig, disconnectDelay)
        builder.setBoundaryCallback(buildBoundaryCallback(pagedListConfig))
        builder.build()
    }

    companion object {
        private val defaultConfig = PagedList.Config.Builder()
//                .setEnablePlaceholders(true)
//                .setInitialLoadSizeHint(4)
//                .setPageSize(4)
//                .setPrefetchDistance(2)

    }

    private fun buildBoundaryCallback(pagedListConfig: PagedList.Config): PagedList.BoundaryCallback<LocalType> {
        return FirebaseLivePagedListBoundaryCallback(
            factory.query,
            factory.cache,
            factory.cache.firebaseScope.getResource(factory.query),
            pagedListConfig,
            true
        )
    }
}

class FirebasePagedResource<LocalType : IFirebaseEntity> protected constructor(
    query: Query,
    clazz: KClass<LocalType>,
    descending: Boolean,
    pagedListConfig: PagedList.Config
) : BaseFirebaseResource<PagedList<LocalType>>() {

    init {
        require(!query.spec.params.hasLimit()) { "The base query cannot have a limit." }
    }

    override val data by lazy {
        // TODO: wrap livedata in delayeddisconnect and unsubscribe boundary listeners, add LiveData.delay() func
        val factory = FirebasePositionalDataSourceFactory<LocalType>(descending)
        val builder = LivePagedListBuilder(factory, pagedListConfig)
        builder.setBoundaryCallback(
            FirebasePagedListBoundaryCallback(
                query,
                pagedListConfig,
                descending,
                factory.collection,
                clazz
            )
        )
        builder.build()
    }

    companion object {
        inline fun <reified T : IFirebaseEntity> create(
            query: Query,
            descending: Boolean = false,
            pagedListConfig: PagedList.Config = PagedList.Config.Builder().apply {
                setPageSize(20)
                setInitialLoadSizeHint(20)
                setPrefetchDistance(5)
                setEnablePlaceholders(false)
            }.build()
        ): FirebasePagedResource<T> {
            return FirebasePagedResource(
                query = query,
                clazz = T::class,
                descending = descending,
                pagedListConfig = pagedListConfig
            )
        }
    }
}
