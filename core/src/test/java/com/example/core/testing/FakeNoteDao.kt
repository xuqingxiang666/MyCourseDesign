package com.example.core.testing

import com.example.core.data.dao.NoteDao
import com.example.core.data.entity.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * 内存版 [NoteDao]，供单元测试使用。
 *
 * 【为什么可以这么做】
 * `NoteDao` 是**接口**（Room 在编译期生成实现），所以测试里可以自己实现一份，
 * 用 `MutableMap` 当存储。这样测 Repository 时不需要真实的 SQLite 数据库，
 * 也不需要 Robolectric —— 测试跑在 JVM 上，毫秒级。
 *
 * 【行为要尽量贴近真实 DAO】
 * 排序、过滤（isArchived / isDeleted）、软删除时递增 version，都照 DAO 的 SQL 实现，
 * 否则测试通过了但真机上行为不一致，反而更危险。
 */
class FakeNoteDao : NoteDao {

    private val storage = LinkedHashMap<Long, Note>()
    private var nextId = 1L

    /** 测试断言用：记录插入顺序。 */
    val insertCalls = mutableListOf<Note>()

    private val state = MutableStateFlow<List<Note>>(emptyList())

    private fun publish() {
        state.value = storage.values.toList()
    }

    /** 直接放一条数据进去（测试准备用）。 */
    fun seed(vararg notes: Note) {
        notes.forEach { storage[it.id] = it }
        publish()
    }

    fun snapshot(): List<Note> = storage.values.toList()

    override fun getActiveNotes(): Flow<List<Note>> =
        state.map { list ->
            list.filter { !it.isArchived && !it.isDeleted }
                .sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.lastModified })
        }

    override fun getArchivedNotes(): Flow<List<Note>> =
        state.map { list ->
            list.filter { it.isArchived && !it.isDeleted }.sortedByDescending { it.lastModified }
        }

    override suspend fun getAllNotesOnce(): List<Note> =
        storage.values.filter { !it.isDeleted }.sortedByDescending { it.lastModified }

    override suspend fun getNoteById(id: Long): Note? = storage[id]

    override suspend fun getNoteCount(): Int = storage.values.count { !it.isDeleted }

    override fun getEncryptedNotes(): Flow<List<Note>> =
        state.map { list -> list.filter { it.isEncrypted && !it.isDeleted } }

    override suspend fun getEncryptedNoteCount(): Int =
        storage.values.count { it.isEncrypted && !it.isDeleted }

    override suspend fun insertNote(note: Note): Long {
        val id = if (note.id == 0L) nextId++ else note.id
        val stored = note.copy(id = id)
        storage[id] = stored
        insertCalls += stored
        publish()
        return id
    }

    override suspend fun updateNote(note: Note) {
        storage[note.id] = note
        publish()
    }

    override suspend fun deleteNote(note: Note) {
        storage.remove(note.id)
        publish()
    }

    override suspend fun deleteNoteById(id: Long) {
        storage.remove(id)
        publish()
    }

    override suspend fun deleteAllNotes() {
        storage.clear()
        publish()
    }

    override suspend fun getAllNotesIncludingDeleted(): List<Note> =
        storage.values.sortedByDescending { it.lastModified }

    override suspend fun softDelete(id: Long, now: Long) {
        val note = storage[id] ?: return
        if (note.isDeleted) return
        storage[id] = note.copy(
            isDeleted = true,
            deletedAt = now,
            lastModified = now,
            version = note.version + 1
        )
        publish()
    }
}
