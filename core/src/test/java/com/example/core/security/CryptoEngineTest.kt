package com.example.core.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [CryptoEngine] 的单元测试。
 *
 * 【为什么这些测试能跑在 JVM 上而不需要模拟器】
 * [CryptoEngine] 只用了 `javax.crypto` 和 `java.security`，没有任何 Android API，
 * 所以普通 `gradlew :core:testDebugUnitTest` 就能跑，秒级完成。
 * 这正是"把纯逻辑和 Android 依赖拆开"带来的好处。
 *
 * 【测试覆盖的场景】
 * 1. 正常往返（各种字符：英文、中文、emoji、换行、超长文本）
 * 2. IV 随机性：同一明文两次加密结果必须不同
 * 3. 错误密钥：解不开必须返回 null，不能返回错误内容
 * 4. 数据被篡改：GCM 认证标签会失败，必须返回 null
 * 5. 异常输入：空数据、超短数据、错误长度密钥
 */
class CryptoEngineTest {

    // ==================== 1. 正常往返 ====================

    @Test
    fun `encrypt then decrypt returns original - ascii`() {
        val key = CryptoEngine.generateKey()
        val plain = "hello world"

        val cipher = CryptoEngine.encrypt(key, plain)
        assertEquals(plain, CryptoEngine.decrypt(key, cipher))
    }

    @Test
    fun `encrypt then decrypt returns original - chinese`() {
        val key = CryptoEngine.generateKey()
        val plain = "这是一条中文笔记，包含标点符号：，。！？"

        val cipher = CryptoEngine.encrypt(key, plain)
        assertEquals(plain, CryptoEngine.decrypt(key, cipher))
    }

    @Test
    fun `encrypt then decrypt returns original - emoji and newlines`() {
        val key = CryptoEngine.generateKey()
        val plain = "第一行 😀🎉\n第二行 <>&\"'\n第三行\t制表符"

        val cipher = CryptoEngine.encrypt(key, plain)
        assertEquals(plain, CryptoEngine.decrypt(key, cipher))
    }

    @Test
    fun `encrypt then decrypt returns original - empty string`() {
        val key = CryptoEngine.generateKey()

        val cipher = CryptoEngine.encrypt(key, "")
        assertEquals("", CryptoEngine.decrypt(key, cipher))
    }

    @Test
    fun `encrypt then decrypt returns original - long text`() {
        val key = CryptoEngine.generateKey()
        val plain = "长文本测试".repeat(10_000) // 约 5 万字符

        val cipher = CryptoEngine.encrypt(key, plain)
        assertEquals(plain, CryptoEngine.decrypt(key, cipher))
    }

    // ==================== 2. IV 随机性 ====================

    @Test
    fun `same plaintext produces different ciphertext each time`() {
        val key = CryptoEngine.generateKey()
        val plain = "同一个内容"

        val c1 = CryptoEngine.encrypt(key, plain)
        val c2 = CryptoEngine.encrypt(key, plain)

        // 如果 IV 真的随机，两次密文不应相同
        assertTrue(
            "两次加密结果相同，说明 IV 没有随机化",
            !c1.contentEquals(c2)
        )
        // 但解密后都必须是原文
        assertEquals(plain, CryptoEngine.decrypt(key, c1))
        assertEquals(plain, CryptoEngine.decrypt(key, c2))
    }

    @Test
    fun `ciphertext contains iv prefix`() {
        val key = CryptoEngine.generateKey()
        val cipher = CryptoEngine.encrypt(key, "x")

        // IV(12) + 密文(1) + 认证标签(16) = 至少 29 字节
        assertTrue(
            "密文长度 ${cipher.size} 不符合 IV+密文+标签 的结构",
            cipher.size >= CryptoEngine.IV_LENGTH_BYTES + 1 + 16
        )
    }

    // ==================== 3. 错误密钥 ====================

    @Test
    fun `decrypt with wrong key returns null`() {
        val key = CryptoEngine.generateKey()
        val wrongKey = CryptoEngine.generateKey()

        val cipher = CryptoEngine.encrypt(key, "机密内容")
        assertNull(
            "用错误密钥竟然解出了内容，说明加密无效",
            CryptoEngine.decrypt(wrongKey, cipher)
        )
    }

    // ==================== 4. 数据被篡改 ====================

    @Test
    fun `tampered ciphertext returns null`() {
        val key = CryptoEngine.generateKey()
        val cipher = CryptoEngine.encrypt(key, "原始内容").copyOf()

        // 篡改最后一个字节（落在 GCM 认证标签里）
        cipher[cipher.size - 1] = (cipher[cipher.size - 1].toInt() xor 0xFF).toByte()

        assertNull(
            "篡改后的密文竟然能解开，说明没有完整性校验",
            CryptoEngine.decrypt(key, cipher)
        )
    }

    @Test
    fun `truncated ciphertext returns null`() {
        val key = CryptoEngine.generateKey()
        val cipher = CryptoEngine.encrypt(key, "原始内容")

        val truncated = cipher.copyOfRange(0, cipher.size - 4)
        assertNull("截断后的密文竟然能解开", CryptoEngine.decrypt(key, truncated))
    }

    @Test
    fun `tampered iv returns null`() {
        val key = CryptoEngine.generateKey()
        val cipher = CryptoEngine.encrypt(key, "原始内容").copyOf()

        // 篡改 IV 的第一个字节
        cipher[0] = (cipher[0].toInt() xor 0xFF).toByte()

        assertNull("IV 被改后仍然解开，说明 IV 没有参与认证", CryptoEngine.decrypt(key, cipher))
    }

    // ==================== 5. 异常输入 ====================

    @Test
    fun `decrypt empty payload returns null`() {
        val key = CryptoEngine.generateKey()
        assertNull(CryptoEngine.decrypt(key, ByteArray(0)))
    }

    @Test
    fun `decrypt payload shorter than iv returns null`() {
        val key = CryptoEngine.generateKey()
        assertNull(CryptoEngine.decrypt(key, ByteArray(CryptoEngine.IV_LENGTH_BYTES)))
    }

    @Test
    fun `encrypt with wrong key size throws`() {
        try {
            CryptoEngine.encrypt(ByteArray(16), "x")
            throw AssertionError("密钥长度不对却没有抛异常")
        } catch (expected: IllegalArgumentException) {
            // 符合预期
        }
    }

    @Test
    fun `decrypt with wrong key size returns null`() {
        val key = CryptoEngine.generateKey()
        val cipher = CryptoEngine.encrypt(key, "x")
        assertNull(CryptoEngine.decrypt(ByteArray(16), cipher))
    }

    // ==================== 6. 密钥生成 ====================

    @Test
    fun `generate key returns 32 bytes`() {
        assertEquals(CryptoEngine.KEY_LENGTH_BYTES, CryptoEngine.generateKey().size)
    }

    @Test
    fun `generate key returns different keys`() {
        val k1 = CryptoEngine.generateKey()
        val k2 = CryptoEngine.generateKey()
        assertTrue("两次生成的密钥相同，随机数有问题", !k1.contentEquals(k2))
    }
}
