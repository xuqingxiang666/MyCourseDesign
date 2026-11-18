package com.example.core.data

import com.example.core.data.entity.Note
import com.example.core.data.entity.RemoteNote
import com.example.core.data.repository.NoteRepositoryImpl
import com.example.core.testing.FakeNoteCrypto
import com.example.core.testing.FakeNoteDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [NoteRepositoryImpl] 的单元测试。
 *
 * 用内存版 DAO + 假加密器，不需要 Android、不需要 SQLite，跑在 JVM 上。
 * 这一层是"业务规则"的集中地（软删除、加密失败不改库、同步覆盖保留本地元数据），
 * 所以值得逐个方法覆盖。
 */
class NoteRepositoryImplTest {

    private fun repo(
        dao: FakeNoteDao = FakeNoteDao(),
        crypto: FakeNoteCrypto = FakeNoteCrypto()
    ) = Triple(NoteRepositoryImpl(dao, crypto), dao, crypto)

    // ==================== 查询 ====================

    @Test
    fun `create note assigns id and default fields`() = runBlocking {
        val (r, dao, _) = repo()
        val id = r.createNote("标题", "内容")
        assertTrue("应回填自增 id", id > 0)
        val saved = r.getNoteById(id)!!
        assertEquals("标题", saved.title)
        assertEquals("内容", saved.content)
        assertEquals(1, saved.version)
        assertEquals(1, dao.insertCalls.size)
    }

    @Test
    fun `count ignores soft deleted notes`() = runBlocking {
        val (r, dao, _) = repo()
        dao.seed(Note(id = 1), Note(id = 2, isDeleted = true))
        assertEquals(1, r.getNoteCount())
    }

    @Test
    fun `active notes excludes archived and deleted`() = runBlocking {
        val (r, dao, _) = repo()
        dao.seed(
            Note(id = 1, isPinned = true),
            Note(id = 2, isArchived = true),
            Note(id = 3, isDeleted = true)
        )
        val active = r.observeActiveNotes().first()
        assertEquals(listOf(1L), active.map { it.id })
    }

    @Test
    fun `encrypted notes query filters deleted`() = runBlocking {
        val (r, dao, _) = repo()
        dao.seed(
            Note(id = 1, isEncrypted = true),
            Note(id = 2, isEncrypted = true, isDeleted = true)
        )
        assertEquals(1, r.getEncryptedNoteCount())
        assertEquals(listOf(1L), r.observeEncryptedNotes().first().map { it.id })
    }

    @Test
    fun `getAllNotesOnce excludes deleted`() = runBlocking {
        val (r, dao, _) = repo()
        dao.seed(Note(id = 1), Note(id = 2, isDeleted = true))
        assertEquals(1, r.getAllNotesOnce().size)
    }

    // ==================== 写操作 ====================

    @Test
    fun `deleteNote performs soft delete`() = runBlocking {
        val (r, dao, _) = repo()
        dao.seed(Note(id = 5, version = 3))
        r.deleteNote(Note(id = 5))
        val after = dao.snapshot().single()
        assertTrue("应是软删除", after.isDeleted)
        assertTrue("应记录删除时间", after.deletedAt > 0)
        assertEquals("删除也要递增版本", 4, after.version)
    }

    @Test
    fun `deleteNoteById removes row`() = runBlocking {
        val (r, dao, _) = repo()
        dao.seed(Note(id = 1))
        r.deleteNoteById(1)
        assertTrue(dao.snapshot().isEmpty())
    }

    @Test
    fun `setArchived delegates to dao default implementation`() = runBlocking {
        val (r, dao, _) = repo()
        dao.seed(Note(id = 1, version = 1))
        r.setArchived(1, true)
        assertTrue(dao.snapshot().single().isArchived)
    }

    @Test
    fun `setPinned delegates to dao default implementation`() = runBlocking {
        val (r, dao, _) = repo()
        dao.seed(Note(id = 1))
        r.setPinned(1, true)
        assertTrue(dao.snapshot().single().isPinned)
    }

    @Test
    fun `setEncryption updates content flag and timestamp`() = runBlocking {
        val (r, dao, _) = repo()
        dao.seed(Note(id = 1, content = "明文"))
        r.setEncryption(1, encrypted = true, content = "密文")
        val after = dao.snapshot().single()
        assertTrue(after.isEncrypted)
        assertEquals("密文", after.content)
        assertTrue("加密时间戳应写入", after.encryptedAt > 0)
    }

    // ==================== 加密 / 解密 ====================

    @Test
    fun `encryptNote encrypts content and marks note`() = runBlocking {
        val (r, dao, _) = repo()
        dao.seed(Note(id = 1, content = "secret"))
        val ok = r.encryptNote(Note(id = 1, content = "secret"))
        assertTrue(ok)
        val after = dao.snapshot().single()
        assertTrue(after.isEncrypted)
        assertEquals(FakeNoteCrypto.PREFIX + "secret", after.content)
    }

    @Test
    fun `encryptNote returns true when already encrypted`() = runBlocking {
        val (r, dao, _) = repo()
        dao.seed(Note(id = 1, content = "enc:x", isEncrypted = true))
        assertTrue("已加密应直接成功，避免二次加密", r.encryptNote(Note(id = 1, isEncrypted = true)))
        assertEquals("内容不应被改动", "enc:x", dao.snapshot().single().content)
    }

    @Test
    fun `encryptNote returns false and keeps db untouched when crypto unavailable`() = runBlocking {
        val crypto = FakeNoteCrypto(available = false)
        val (r, dao, _) = repo(crypto = crypto)
        dao.seed(Note(id = 1, content = "secret"))
        assertFalse(r.encryptNote(Note(id = 1, content = "secret")))
        val after = dao.snapshot().single()
        assertFalse("加密失败不能留下已加密标记", after.isEncrypted)
        assertEquals("内容不应被改", "secret", after.content)
    }

    @Test
    fun `decryptNote returns content for plain note`() = runBlocking {
        val (r, _, _) = repo()
        assertEquals("明文", r.decryptNote(Note(id = 1, content = "明文")))
    }

    @Test
    fun `decryptNote decrypts encrypted note`() = runBlocking {
        val (r, _, _) = repo()
        assertEquals(
            "明文",
            r.decryptNote(Note(id = 1, content = "enc:明文", isEncrypted = true))
        )
    }

    @Test
    fun `decryptNote returns null when crypto unavailable`() = runBlocking {
        val (r, _, _) = repo(crypto = FakeNoteCrypto(available = false))
        assertNull(r.decryptNote(Note(id = 1, content = "enc:x", isEncrypted = true)))
    }

    // ==================== 同步相关 ====================

    @Test
    fun `getAllNotesIncludingDeleted returns deleted notes too`() = runBlocking {
        val (r, dao, _) = repo()
        dao.seed(Note(id = 1), Note(id = 2, isDeleted = true))
        assertEquals(2, r.getAllNotesIncludingDeleted().size)
    }

    @Test
    fun `applyRemoteNote inserts when local missing`() = runBlocking {
        val (r, dao, _) = repo()
        r.applyRemoteNote(RemoteNote(id = 9, title = "远端", content = "c", lastModified = 100, version = 2))
        val local = dao.snapshot().single()
        assertEquals(9L, local.id)
        assertEquals("远端", local.title)
        assertEquals(2, local.version)
    }

    @Test
    fun `applyRemoteNote updates existing and keeps local encryptedAt`() = runBlocking {
        val (r, dao, _) = repo()
        dao.seed(Note(id = 1, title = "旧", encryptedAt = 555, version = 1))
        r.applyRemoteNote(RemoteNote(id = 1, title = "新", content = "cc", lastModified = 200, version = 5))
        val local = dao.snapshot().single()
        assertEquals("新", local.title)
        assertEquals(5, local.version)
        assertEquals("本机加密时间不能被云端覆盖", 555L, local.encryptedAt)
    }

    @Test
    fun `applyRemoteNote copies deleted state`() = runBlocking {
        val (r, dao, _) = repo()
        r.applyRemoteNote(RemoteNote(id = 2, isDeleted = true, deletedAt = 777))
        assertTrue(dao.snapshot().single().isDeleted)
        assertEquals(777L, dao.snapshot().single().deletedAt)
    }

    @Test
    fun `deleteAllNotes clears storage`() = runBlocking {
        val (r, dao, _) = repo()
        dao.seed(Note(id = 1), Note(id = 2))
        r.deleteAllNotes()
        assertTrue(dao.snapshot().isEmpty())
    }

    @Test
    fun `updateNote writes through`() = runBlocking {
        val (r, dao, _) = repo()
        dao.seed(Note(id = 1, title = "旧"))
        r.updateNote(Note(id = 1, title = "新"))
        assertEquals("新", dao.snapshot().single().title)
    }
}
