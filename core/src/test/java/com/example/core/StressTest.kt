package com.example.core

import com.example.core.data.entity.Note
import com.example.core.data.entity.RemoteNote
import com.example.core.domain.ConflictResolver
import com.example.core.domain.SyncEngine
import com.example.core.security.CryptoEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 轻量压力测试（性能回归）。
 *
 * 【为什么要有这一类测试】
 * 功能测试只保证"结果对"，不保证"够快"。笔记数量上去之后（几百到几千条），
 * 同步、冲突判定、加解密都可能变慢，而这类退化很难靠肉眼发现。
 * 这里用"大批量数据 + 时间上限断言"把它们固定住：
 * 一旦有人写出 O(n²) 的实现，测试会立刻失败。
 *
 * 【阈值怎么定】
 * 取实测值的 5~10 倍作为上限，机器快慢都有余量，但能挡住数量级退化。
 *
 * 【注意】
 * 这些是 JVM 上的纯逻辑测试，不涉及 Android，也不测 UI 帧率。
 */
class StressTest {

    private fun note(id: Long, offset: Long = 0) = Note(
        id = id,
        title = "笔记$id",
        content = "本地内容".repeat(20),
        lastModified = 1_000 + offset,
        version = 1
    )

    private fun remote(id: Long, offset: Long = 0) = RemoteNote(
        id = id,
        title = "笔记$id",
        content = "云端内容".repeat(20),
        lastModified = 1_000 + offset,
        version = 1
    )

    // ==================== 同步计划：1000 条 ====================

    @Test
    fun `sync plan with 1000 notes completes quickly`() {
        val local = (1L..1000L).map { note(it, offset = if (it % 2 == 0L) 500 else 0) }
        val remote = (1L..1000L).map { remote(it, offset = if (it % 2 == 0L) 0 else 500) }

        val start = System.nanoTime()
        val plan = SyncEngine.plan(local, remote)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000

        println("[STRESS] SyncEngine.plan 1000 条耗时 = ${elapsedMs}ms（上传 ${plan.uploads.size}、下载 ${plan.downloads.size}、冲突 ${plan.conflicts.size}）")

        // 1000 条两边都存在且内容不同 -> 全部是冲突
        assertEquals("两边内容不同，全部应判为冲突", 1000, plan.conflicts.size)
        assertEquals("偶数 id 本地更新 -> 上传", 500, plan.uploads.size)
        assertEquals("奇数 id 云端更新 -> 下载", 500, plan.downloads.size)
        assertTrue("1000 条同步计划应在 1000ms 内完成，实际 ${elapsedMs}ms", elapsedMs < 1000)
    }

    // ==================== 冲突判定：10000 次 ====================

    @Test
    fun `conflict resolution 10000 times completes quickly`() {
        val a = note(1, offset = 500)
        val b = remote(1, offset = 0)

        val start = System.nanoTime()
        repeat(10_000) { ConflictResolver.resolve(a, b) }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000

        println("[STRESS] ConflictResolver.resolve 10000 次耗时 = ${elapsedMs}ms")
        assertTrue("10000 次判定应在 2000ms 内完成，实际 ${elapsedMs}ms", elapsedMs < 2000)
    }

    // ==================== 加解密：1MB 数据 ====================

    @Test
    fun `encrypt and decrypt 1MB total completes quickly`() {
        val key = CryptoEngine.generateKey()
        val plaintext = "笔记内容".repeat(500) // 约 2KB/条

        val start = System.nanoTime()
        repeat(500) {
            val cipher = CryptoEngine.encrypt(key, plaintext)
            val back = CryptoEngine.decrypt(key, cipher)
            assertEquals(plaintext, back)
        }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000

        println("[STRESS] 500 次加解密（约 1MB）耗时 = ${elapsedMs}ms")
        assertTrue("1MB 加解密应在 5000ms 内完成，实际 ${elapsedMs}ms", elapsedMs < 5000)
    }
}
