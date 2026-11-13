package com.example.core.domain

import com.example.core.data.entity.Note
import com.example.core.data.entity.RemoteNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [ConflictResolver] 的单元测试。
 *
 * 覆盖三级判定（时间戳 / 版本号 / 内容哈希）以及软删除场景。
 * 因为它是纯 Kotlin，测试不需要数据库、不需要模拟器，跑一次不到 1 秒。
 */
class ConflictResolverTest {

    // ==================== 辅助构造 ====================

    private fun local(
        id: Long = 1,
        title: String = "标题",
        content: String = "内容",
        lastModified: Long = 1_000,
        version: Int = 1,
        isDeleted: Boolean = false,
        deletedAt: Long = 0
    ) = Note(
        id = id,
        title = title,
        content = content,
        lastModified = lastModified,
        version = version,
        isDeleted = isDeleted,
        deletedAt = deletedAt
    )

    private fun remote(
        id: Long = 1,
        title: String = "标题",
        content: String = "内容",
        lastModified: Long = 1_000,
        version: Int = 1,
        isDeleted: Boolean = false,
        deletedAt: Long = 0
    ) = RemoteNote(
        id = id,
        title = title,
        content = content,
        lastModified = lastModified,
        version = version,
        isDeleted = isDeleted,
        deletedAt = deletedAt
    )

    // ==================== 级别 0：完全一致 ====================

    @Test
    fun `identical content returns IDENTICAL`() {
        val decision = ConflictResolver.resolve(local(), remote())
        assertEquals(Winner.IDENTICAL, decision.winner)
    }

    @Test
    fun `identical content ignores different timestamps`() {
        // 内容一样就不该有动作，即使时间戳不同
        val decision = ConflictResolver.resolve(
            local(lastModified = 1_000),
            remote(lastModified = 9_999)
        )
        assertEquals(Winner.IDENTICAL, decision.winner)
    }

    // ==================== 级别 1：时间戳 ====================

    @Test
    fun `newer local wins`() {
        val decision = ConflictResolver.resolve(
            local(content = "本地新内容", lastModified = 2_000),
            remote(content = "云端旧内容", lastModified = 1_000)
        )
        assertEquals(Winner.LOCAL, decision.winner)
        assertEquals(2_000, decision.localTime)
        assertEquals(1_000, decision.remoteTime)
    }

    @Test
    fun `newer remote wins`() {
        val decision = ConflictResolver.resolve(
            local(content = "本地旧内容", lastModified = 1_000),
            remote(content = "云端新内容", lastModified = 2_000)
        )
        assertEquals(Winner.REMOTE, decision.winner)
    }

    @Test
    fun `reason text mentions timestamps`() {
        val decision = ConflictResolver.resolve(
            local(content = "a", lastModified = 2_000),
            remote(content = "b", lastModified = 1_000)
        )
        assertTrue("原因应包含时间说明: ${decision.reason}", decision.reason.contains("本地版本更新"))
    }

    // ==================== 级别 2：版本号 ====================

    @Test
    fun `same timestamp higher local version wins`() {
        val decision = ConflictResolver.resolve(
            local(content = "本地", lastModified = 1_000, version = 5),
            remote(content = "云端", lastModified = 1_000, version = 3)
        )
        assertEquals(Winner.LOCAL, decision.winner)
        assertTrue(decision.reason.contains("版本"))
    }

    @Test
    fun `same timestamp higher remote version wins`() {
        val decision = ConflictResolver.resolve(
            local(content = "本地", lastModified = 1_000, version = 2),
            remote(content = "云端", lastModified = 1_000, version = 4)
        )
        assertEquals(Winner.REMOTE, decision.winner)
    }

    // ==================== 级别 3：内容哈希（确定性） ====================

    @Test
    fun `same timestamp and version is decided deterministically`() {
        val a = local(content = "AAAA", lastModified = 1_000, version = 1)
        val b = remote(content = "BBBB", lastModified = 1_000, version = 1)

        val first = ConflictResolver.resolve(a, b).winner
        val second = ConflictResolver.resolve(a, b).winner

        // 同一组输入必须每次得到相同结果（不能随机、不能依赖遍历顺序）
        assertEquals(first, second)
        // 而且不是"无变化"——内容确实不同
        assertTrue(first == Winner.LOCAL || first == Winner.REMOTE)
    }

    @Test
    fun `symmetric case picks opposite side`() {
        val a = local(content = "AAAA", lastModified = 1_000, version = 1)
        val b = remote(content = "BBBB", lastModified = 1_000, version = 1)

        val decisionA = ConflictResolver.resolve(a, b).winner
        // 把内容对调后，结论必须反过来 —— 说明裁决只取决于内容，不取决于参数位置
        val decisionB = ConflictResolver.resolve(
            local(content = "BBBB", lastModified = 1_000, version = 1),
            remote(content = "AAAA", lastModified = 1_000, version = 1)
        ).winner

        assertTrue(
            "对调内容后结论应相反，实际 $decisionA / $decisionB",
            decisionA != decisionB
        )
    }

    // ==================== 软删除 ====================

    @Test
    fun `deletion later than remote edit wins`() {
        // 本地删了（时间 2000），云端在 1000 改过 -> 删除生效
        val decision = ConflictResolver.resolve(
            local(isDeleted = true, deletedAt = 2_000, lastModified = 2_000),
            remote(content = "云端改过", lastModified = 1_000)
        )
        assertEquals(Winner.LOCAL, decision.winner)
    }

    @Test
    fun `remote edit later than local deletion wins`() {
        // 本地删了（1000），云端在 2000 改过 -> 笔记复活
        val decision = ConflictResolver.resolve(
            local(isDeleted = true, deletedAt = 1_000, lastModified = 1_000),
            remote(content = "云端改过", lastModified = 2_000)
        )
        assertEquals(Winner.REMOTE, decision.winner)
    }

    @Test
    fun `remote deletion later than local edit wins`() {
        val decision = ConflictResolver.resolve(
            local(content = "本地改过", lastModified = 1_000),
            remote(isDeleted = true, deletedAt = 2_000, lastModified = 2_000)
        )
        assertEquals(Winner.REMOTE, decision.winner)
    }

    @Test
    fun `both deleted with same content is identical`() {
        val decision = ConflictResolver.resolve(
            local(isDeleted = true, deletedAt = 1_000, lastModified = 1_000),
            remote(isDeleted = true, deletedAt = 1_000, lastModified = 1_000)
        )
        assertEquals(Winner.IDENTICAL, decision.winner)
    }

    // ==================== effectiveTime 辅助方法 ====================

    @Test
    fun `effectiveTime uses lastModified when alive`() {
        assertEquals(500, ConflictResolver.effectiveTime(500, isDeleted = false, deletedAt = 999))
    }

    @Test
    fun `effectiveTime uses deletedAt when deleted`() {
        assertEquals(999, ConflictResolver.effectiveTime(500, isDeleted = true, deletedAt = 999))
    }

    @Test
    fun `effectiveTime falls back to lastModified when deletedAt missing`() {
        // 防御：只写了 isDeleted 没写 deletedAt 时，不能返回 0
        assertEquals(500, ConflictResolver.effectiveTime(500, isDeleted = true, deletedAt = 0))
    }
}
