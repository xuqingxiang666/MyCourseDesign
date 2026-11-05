package com.example.core.security

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 纯逻辑的加解密引擎：AES-256-GCM。
 *
 * 【为什么把它和"密钥管理"拆成两个类】
 * 这个对象**不依赖任何 Android API**（只用 `javax.crypto` 和 `java.security`），
 * 因此可以直接用普通 JUnit 在 JVM 上跑单元测试，不需要模拟器。
 * 而取密钥、存密钥必须依赖 Android（`EncryptedSharedPreferences`），
 * 那部分放在 [CryptoManager] 里。
 *
 * 【为什么选 AES-GCM 而不是 AES-CBC】
 * | | AES-CBC | AES-GCM |
 * |---|---|---|
 * | 机密性 | ✅ | ✅ |
 * | **完整性/防篡改** | ❌ 需要额外加 HMAC，否则可被篡改而不被发现 | ✅ 自带认证标签 |
 * | 是否需要 padding | 需要（PKCS5） | 不需要 |
 * 课设里"笔记不能被偷偷改动"是基本要求，GCM 一次到位。
 *
 * 【IV 是什么、为什么每次都要随机】
 * IV（初始向量）保证"同样的明文 + 同样的密钥"每次加密结果不同。
 * 如果 IV 固定，攻击者能看出"两条笔记内容相同"，GCM 的安全性也会被破坏。
 * GCM 推荐 IV 长度为 12 字节，且**绝不能重复使用同一个 IV + 密钥组合**。
 *
 * 【输出格式】
 * `IV(12 字节) || 密文(含 16 字节认证标签)`
 * 把 IV 直接拼在密文前面，解密时再切出来 —— 这样数据库里只存一个字符串就够，
 * 不需要额外的 IV 列。
 *
 * 【Java 思维对照】
 * `object` 是 Kotlin 的单例，等价于 Java 里只含 `static` 方法的工具类。
 */
object CryptoEngine {

    /** 加密算法 / 工作模式 / 填充方式。GCM 不需要填充。 */
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    /** GCM 认证标签长度（位）。128 位是推荐值。 */
    private const val TAG_LENGTH_BITS = 128

    /** IV 长度（字节）。GCM 推荐 12 字节。 */
    const val IV_LENGTH_BYTES = 12

    /** 密钥长度（字节）。32 字节 = 256 位 = AES-256。 */
    const val KEY_LENGTH_BYTES = 32

    /**
     * 加密。
     *
     * @param key 32 字节的 AES-256 密钥
     * @param plaintext 明文
     * @return `IV || 密文`，可以直接存进数据库（外层还会做 Base64）
     * @throws IllegalArgumentException 密钥长度不对时抛出，属于**编程错误**，应尽早暴露
     */
    fun encrypt(key: ByteArray, plaintext: String): ByteArray {
        require(key.size == KEY_LENGTH_BYTES) {
            "AES-256 需要 $KEY_LENGTH_BYTES 字节密钥，实际 ${key.size} 字节"
        }

        val iv = ByteArray(IV_LENGTH_BYTES).also {
            // SecureRandom 是密码学安全的随机数生成器，不能用 Random
            SecureRandom().nextBytes(it)
        }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(TAG_LENGTH_BITS, iv)
        )
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // IV 放在最前面，解密时按固定长度切回来
        return iv + ciphertext
    }

    /**
     * 解密。
     *
     * @param key 32 字节的 AES-256 密钥
     * @param payload `IV || 密文`
     * @return 明文；**任何异常都返回 null**，而不是抛出
     *
     * 【为什么失败要返回 null 而不是抛异常】
     * 解密失败的常见原因都是"数据或环境出了问题"，而不是程序写错了：
     * - 密钥变了（用户清除了应用数据 / 换了设备）
     * - 密文被篡改或截断（GCM 认证失败）
     * - 这条笔记其实没加密（误调）
     * 这些都是**可预期的情况**，UI 应该显示"无法解密"而不是让 App 崩溃。
     * 所以这里把 `GeneralSecurityException` 等一律吞掉并返回 null。
     */
    fun decrypt(key: ByteArray, payload: ByteArray): String? {
        if (key.size != KEY_LENGTH_BYTES) return null
        // 至少要有 IV + 1 字节密文；少于这个长度必然是坏数据
        if (payload.size <= IV_LENGTH_BYTES) return null

        return try {
            val iv = payload.copyOfRange(0, IV_LENGTH_BYTES)
            val ciphertext = payload.copyOfRange(IV_LENGTH_BYTES, payload.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, "AES"),
                GCMParameterSpec(TAG_LENGTH_BITS, iv)
            )
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            // 认证失败 / 密钥不匹配 / 数据损坏都会走到这里
            null
        }
    }

    /**
     * 生成一个安全的随机密钥（32 字节）。
     *
     * 只应该在第一台设备首次使用时调用一次，之后从安全存储里读。
     */
    fun generateKey(): ByteArray =
        ByteArray(KEY_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
}
