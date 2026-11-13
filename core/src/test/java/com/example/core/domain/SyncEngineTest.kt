package com.example.core.domain

import com.example.core.data.entity.Note
import com.example.core.data.entity.RemoteNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [SyncEngine] 的单元测试。
 *
 * 验证"输入本地列表 + 云端列表 → 输出同步计划"这一整套规则，
 * 包括只在一端存在、两端冲突、软删除传播等场景。
 */
class SyncEngineTest {

    private fun local(
        id: Long,
        content: String = "内容",
        lastModified: Long = 1_000,
        version: Int = 1,
        isDeleted: Boolean = false,
        deletedAt: Long = 0
    ) = Note(
        id = id,
        title = "笔记$id",
        content = content,
        lastModified = lastModified,
        version = version,
        isDeleted = isDeleted,
        deletedAt = deletedAt
    )

    private fun remote(
        id: Long,
        content: String = "内容",
        lastModified: Long = 1_000,
        version: Int = 1,
        isDeleted: Boolean = false,
        deletedAt: Long = 0
    ) = RemoteNote(
        id = id,
        title = "笔记$id",
        content = content,
        lastModified = lastModified,
        version = version,
        isDeleted = isDeleted,
        deletedAt = deletedAt
    )

    // ==================== 基本情况 ====================

    @Test
    fun `empty lists produce empty plan`() {
        val plan = SyncEngine.plan(emptyList(), emptyList())
        assertTrue(plan.isEmpty)
        assertEquals(0, plan.unchangedCount)
        assertTrue(plan.conflicts.isEmpty())
    }

    @Test
    fun `local only note is uploaded`() {
        val plan = SyncEngine.plan(listOf(local(1)), emptyList())
        assertEquals(1, plan.uploads.size)
        assertEquals(1L, plan.uploads[0].id)
        assertTrue(plan.downloads.isEmpty())
    }

    @Test
    fun `remote only note is downloaded`() {
        val plan = SyncEngine.plan(emptyList(), listOf(remote(2)))
        assertEquals(1, plan.downloads.size)
        assertEquals(2L, plan.downloads[0].id)
        assertTrue(plan.uploads.isEmpty())
    }

    @Test
    fun `identical note is counted as unchanged`() {
        val plan = SyncEngine.plan(listOf(local(1)), listOf(remote(1)))
        assertEquals(1, plan.unchangedCount)
        assertTrue(plan.isEmpty)
        assertTrue(plan.conflicts.isEmpty())
    }

    // ==================== 冲突：上传 / 下载 ====================

    @Test
    fun `local newer causes upload and conflict record`() {
        val plan = SyncEngine.plan(
            listOf(local(1, content = "本地新", lastModified = 2_000)),
            listOf(remote(1, content = "云端旧", lastModified = 1_000))
        )
        assertEquals(1, plan.uploads.size)
        assertEquals(0, plan.downloads.size)
        assertEquals(1, plan.conflicts.size)
        assertEquals(Winner.LOCAL, plan.conflicts[0].winner)
    }

    @Test
    fun `remote newer causes download and conflict record`() {
        val plan = SyncEngine.plan(
            listOf(local(1, content = "本地旧", lastModified = 1_000)),
            listOf(remote(1, content = "云端新", lastModified = 2_000))
        )
        assertEquals(0, plan.uploads.size)
        assertEquals(1, plan.downloads.size)
        assertEquals(Winner.REMOTE, plan.conflicts[0].winner)
    }

    @Test
    fun `conflict record carries both sides details`() {
        val plan = SyncEngine.plan(
            listOf(local(7, content = "本地", lastModified = 2_000, version = 3)),
            listOf(remote(7, content = "云端", lastModified = 1_000, version = 2))
        )
        val record = plan.conflicts.single()
        assertEquals(7L, record.id)
        assertEquals(2_000, record.localModified)
        assertEquals(3, record.localVersion)
        assertEquals(1_000, record.remoteModified)
        assertEquals(2, record.remoteVersion)
    }

    // ==================== 软删除 ====================

    @Test
    fun `local deletion is uploaded so remote also deletes`() {
        val plan = SyncEngine.plan(
            listOf(local(1, isDeleted = true, deletedAt = 2_000, lastModified = 2_000)),
            listOf(remote(1, lastModified = 1_000))
        )
        assertEquals(1, plan.uploads.size)
        assertTrue("上传的应是已删除状态", plan.uploads[0].isDeleted)
    }

    @Test
    fun `remote deletion later than local edit causes download`() {
        val plan = SyncEngine.plan(
            listOf(local(1, content = "本地改过", lastModified = 1_000)),
            listOf(remote(1, isDeleted = true, deletedAt = 2_000, lastModified = 2_000))
        )
        assertEquals(1, plan.downloads.size)
        assertTrue(plan.downloads[0].isDeleted)
    }

    @Test
    fun `both deleted is unchanged`() {
        val plan = SyncEngine.plan(
            listOf(local(1, isDeleted = true, deletedAt = 1_000, lastModified = 1_000)),
            listOf(remote(1, isDeleted = true, deletedAt = 1_000, lastModified = 1_000))
        )
        assertEquals(1, plan.unchangedCount)
        assertTrue(plan.isEmpty)
    }

    // ==================== 混合场景 ====================

    @Test
    fun `mixed case handles every category`() {
        val plan = SyncEngine.plan(
            localNotes = listOf(
                local(1),                                   // 与云端一致 -> 无变化
                local(2, content = "本地新", lastModified = 2_000), // 本地胜 -> 上传
                local(3),                                   // 本地独有 -> 上传
                local(4, content = "本地旧", lastModified = 1_000)  // 云端胜 -> 下载
            ),
            remoteNotes = listOf(
                remote(1),
                remote(2, content = "云端旧", lastModified = 1_000),
                remote(4, content = "云端新", lastModified = 2_000),
                remote(5)                                   // 云端独有 -> 下载
            )
        )

        assertEquals("应上传 id=2,3", listOf(2L, 3L), plan.uploads.map { it.id })
        assertEquals("应下载 id=4,5", listOf(4L, 5L), plan.downloads.map { it.id })
        assertEquals(2, plan.conflicts.size)
        assertEquals(1, plan.unchangedCount)
    }

    @Test
    fun `output order is stable for same input`() {
        val local = listOf(local(3), local(1), local(2))
        val planA = SyncEngine.plan(local, emptyList())
        val planB = SyncEngine.plan(local, emptyList())
        assertEquals(
            planA.uploads.map { it.id },
            planB.uploads.map { it.id }
        )
        assertEquals("应按 id 升序输出", listOf(1L, 2L, 3L), planA.uploads.map { it.id })
    }

    // ==================== 摘要文案 ====================

    @Test
    fun `summary mentions every category`() {
        val plan = SyncEngine.plan(
            listOf(local(1), local(2, content = "x", lastModified = 2_000)),
            listOf(remote(1), remote(2, content = "y", lastModified = 1_000), remote(3))
        )
        val summary = plan.summary()
        assertTrue(summary, summary.contains("上传"))
        assertTrue(summary, summary.contains("下载"))
        assertTrue(summary, summary.contains("冲突"))
        assertTrue(summary, summary.contains("无变化"))
    }
}
