package com.example.core.data

import com.example.core.data.entity.Note
import com.example.core.data.entity.RemoteNote
import com.example.core.data.repository.SyncRepository
import com.example.core.domain.Winner
import com.example.core.testing.FakeNoteRepository
import com.example.core.testing.FakeRemoteDataSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [SyncRepository] 的单元测试。
 *
 * 重点验证"执行同步"的副作用：该上传的写到云端、该下载的写回本地，
 * 以及演示辅助方法（模拟另一台设备改/删）确实能造出可复现的冲突。
 */
class SyncRepositoryTest {

    private fun setup(
        local: List<Note> = emptyList(),
        remote: List<RemoteNote> = emptyList()
    ): Triple<SyncRepository, FakeNoteRepository, FakeRemoteDataSource> {
        val repo = FakeNoteRepository().apply { seed(*local.toTypedArray()) }
        val remoteDs = FakeRemoteDataSource().apply { seed(*remote.toTypedArray()) }
        return Triple(SyncRepository(repo, remoteDs), repo, remoteDs)
    }

    // ==================== plan ====================

    @Test
    fun `plan does not modify any data`() = runBlocking {
        val (sync, local, remote) = setup(
            local = listOf(Note(id = 1, content = "本地新", lastModified = 2_000)),
            remote = listOf(RemoteNote(id = 1, content = "云端旧", lastModified = 1_000))
        )
        val plan = sync.plan()
        assertEquals(1, plan.uploads.size)
        assertEquals("plan 不应改动云端", 1, remote.snapshot().size)
        assertEquals("plan 不应改动本地内容", "本地新", local.snapshot().single().content)
    }

    // ==================== sync ====================

    @Test
    fun `sync uploads local-only note to cloud`() = runBlocking {
        val (sync, _, remote) = setup(local = listOf(Note(id = 1, title = "只本地有")))
        val result = sync.sync()
        assertEquals(1, result.uploaded)
        assertEquals(0, result.downloaded)
        assertEquals(1, remote.snapshot().size)
        assertEquals("只本地有", remote.snapshot().single().title)
    }

    @Test
    fun `sync downloads cloud-only note to local`() = runBlocking {
        val (sync, local, _) = setup(remote = listOf(RemoteNote(id = 2, title = "只云端有")))
        val result = sync.sync()
        assertEquals(0, result.uploaded)
        assertEquals(1, result.downloaded)
        assertEquals("只云端有", local.snapshot().single().title)
    }

    @Test
    fun `sync applies cloud version when it is newer`() = runBlocking {
        val (sync, local, remote) = setup(
            local = listOf(Note(id = 1, content = "旧", lastModified = 1_000, version = 1)),
            remote = listOf(RemoteNote(id = 1, content = "新", lastModified = 2_000, version = 2))
        )
        val result = sync.sync()
        assertEquals(1, result.downloaded)
        assertEquals("新", local.snapshot().single().content)
        assertEquals(Winner.REMOTE, result.plan.conflicts.single().winner)
        assertEquals("新的", "新", remote.snapshot().single().content)
    }

    @Test
    fun `sync propagates local deletion to cloud`() = runBlocking {
        val (sync, _, remote) = setup(
            local = listOf(Note(id = 3, isDeleted = true, deletedAt = 5_000, lastModified = 5_000)),
            remote = listOf(RemoteNote(id = 3, lastModified = 1_000))
        )
        sync.sync()
        assertTrue("云端应被标记为删除", remote.snapshot().single().isDeleted)
    }

    @Test
    fun `sync returns summary with conflict count`() = runBlocking {
        val (sync, _, _) = setup(
            local = listOf(Note(id = 1, content = "a", lastModified = 2_000)),
            remote = listOf(RemoteNote(id = 1, content = "b", lastModified = 1_000))
        )
        val result = sync.sync()
        assertTrue(result.summary, result.summary.contains("冲突 1 条"))
    }

    // ==================== counts ====================

    @Test
    fun `counts reports alive deleted and remote`() = runBlocking {
        val (sync, _, _) = setup(
            local = listOf(Note(id = 1), Note(id = 2, isDeleted = true)),
            remote = listOf(RemoteNote(id = 1))
        )
        val (alive, deleted, remoteCount) = sync.counts()
        assertEquals(1, alive)
        assertEquals(1, deleted)
        assertEquals(1, remoteCount)
    }

    // ==================== 演示辅助 ====================

    @Test
    fun `pushAllLocalToRemote skips deleted notes`() = runBlocking {
        val (sync, _, remote) = setup(
            local = listOf(Note(id = 1), Note(id = 2, isDeleted = true))
        )
        val pushed = sync.pushAllLocalToRemote()
        assertEquals(1, pushed)
        assertEquals(1, remote.snapshot().size)
    }

    @Test
    fun `simulateRemoteEdit makes cloud newer`() = runBlocking {
        val (sync, _, remote) = setup(
            local = listOf(Note(id = 1, title = "原标题", lastModified = 1_000)),
            remote = listOf(RemoteNote(id = 1, title = "原标题", lastModified = 1_000, version = 1))
        )
        val id = sync.simulateRemoteEdit(1)
        assertEquals(1L, id)
        val after = remote.snapshot().single()
        assertTrue("云端时间应更新到未来", after.lastModified > System.currentTimeMillis())
        assertEquals(2, after.version)
    }

    @Test
    fun `simulateRemoteEdit returns null when cloud has no such note`() = runBlocking {
        val (sync, _, _) = setup(local = listOf(Note(id = 1)))
        assertNull(sync.simulateRemoteEdit(1))
    }

    @Test
    fun `simulateRemoteDelete marks cloud deleted`() = runBlocking {
        val (sync, _, remote) = setup(remote = listOf(RemoteNote(id = 1, version = 1)))
        assertNotNull(sync.simulateRemoteDelete(1))
        assertTrue(remote.snapshot().single().isDeleted)
    }

    @Test
    fun `simulateRemoteDelete returns null when missing`() = runBlocking {
        val (sync, _, _) = setup()
        assertNull(sync.simulateRemoteDelete(99))
    }

    @Test
    fun `clearRemote empties cloud`() = runBlocking {
        val (sync, _, remote) = setup(remote = listOf(RemoteNote(id = 1)))
        sync.clearRemote()
        assertTrue(remote.snapshot().isEmpty())
    }

    @Test
    fun `remoteNotes and localNotes expose snapshots`() = runBlocking {
        val (sync, _, _) = setup(
            local = listOf(Note(id = 1)),
            remote = listOf(RemoteNote(id = 1))
        )
        assertEquals(1, sync.remoteNotes().size)
        assertEquals(1, sync.localNotes().size)
    }
}
