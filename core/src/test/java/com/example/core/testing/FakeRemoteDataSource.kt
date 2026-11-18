package com.example.core.testing

import com.example.core.data.entity.RemoteNote
import com.example.core.data.remote.RemoteDataSource

/** 内存版"云端"，供同步测试使用。 */
class FakeRemoteDataSource : RemoteDataSource {

    private val storage = LinkedHashMap<Long, RemoteNote>()

    fun seed(vararg notes: RemoteNote) {
        notes.forEach { storage[it.id] = it }
    }

    fun snapshot(): List<RemoteNote> = storage.values.toList()

    override suspend fun fetchAll(): List<RemoteNote> = storage.values.toList()

    override suspend fun upsert(note: RemoteNote) {
        storage[note.id] = note
    }

    override suspend fun count(): Int = storage.size

    override suspend fun clear() = storage.clear()
}
