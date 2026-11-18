package com.example.core.testing

import com.example.core.data.entity.Note
import com.example.core.data.entity.RemoteNote
import com.example.core.data.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** 内存版 [NoteRepository]，供 SyncRepository 的单元测试使用。 */
class FakeNoteRepository : NoteRepository {

    private val storage = LinkedHashMap<Long, Note>()
    private var nextId = 1L
    private val state = MutableStateFlow<List<Note>>(emptyList())

    fun seed(vararg notes: Note) {
        notes.forEach { storage[it.id] = it }
        state.value = storage.values.toList()
    }

    fun snapshot(): List<Note> = storage.values.toList()

    private fun publish() {
        state.value = storage.values.toList()
    }

    override fun observeActiveNotes(): Flow<List<Note>> =
        state.map { l -> l.filter { !it.isArchived && !it.isDeleted } }

    override fun observeArchivedNotes(): Flow<List<Note>> =
        state.map { l -> l.filter { it.isArchived && !it.isDeleted } }

    override suspend fun getAllNotesOnce(): List<Note> = storage.values.filter { !it.isDeleted }

    override suspend fun getNoteById(id: Long): Note? = storage[id]

    override suspend fun getNoteCount(): Int = storage.values.count { !it.isDeleted }

    override fun observeEncryptedNotes(): Flow<List<Note>> =
        state.map { l -> l.filter { it.isEncrypted && !it.isDeleted } }

    override suspend fun getEncryptedNoteCount(): Int =
        storage.values.count { it.isEncrypted && !it.isDeleted }

    override suspend fun createNote(title: String, content: String): Long {
        val id = nextId++
        storage[id] = Note(id = id, title = title, content = content)
        publish()
        return id
    }

    override suspend fun updateNote(note: Note) {
        storage[note.id] = note
        publish()
    }

    override suspend fun deleteNote(note: Note) {
        val old = storage[note.id] ?: return
        storage[note.id] = old.copy(isDeleted = true, deletedAt = System.currentTimeMillis())
        publish()
    }

    override suspend fun deleteNoteById(id: Long) {
        storage.remove(id)
        publish()
    }

    override suspend fun setArchived(id: Long, archived: Boolean) {
        storage[id]?.let { storage[id] = it.copy(isArchived = archived) }
        publish()
    }

    override suspend fun setPinned(id: Long, pinned: Boolean) {
        storage[id]?.let { storage[id] = it.copy(isPinned = pinned) }
        publish()
    }

    override suspend fun setEncryption(id: Long, encrypted: Boolean, content: String) {
        storage[id]?.let { storage[id] = it.copy(isEncrypted = encrypted, content = content) }
        publish()
    }

    override suspend fun encryptNote(note: Note): Boolean {
        storage[note.id]?.let { storage[note.id] = it.copy(isEncrypted = true) }
        publish()
        return true
    }

    override suspend fun decryptNote(note: Note): String? =
        if (note.isEncrypted) null else note.content

    override suspend fun getAllNotesIncludingDeleted(): List<Note> = storage.values.toList()

    override suspend fun applyRemoteNote(remote: RemoteNote) {
        storage[remote.id] = Note(
            id = remote.id,
            title = remote.title,
            content = remote.content,
            isArchived = remote.isArchived,
            isPinned = remote.isPinned,
            isEncrypted = remote.isEncrypted,
            lastModified = remote.lastModified,
            version = remote.version,
            isDeleted = remote.isDeleted,
            deletedAt = remote.deletedAt
        )
        publish()
    }

    override suspend fun deleteAllNotes() {
        storage.clear()
        publish()
    }
}
