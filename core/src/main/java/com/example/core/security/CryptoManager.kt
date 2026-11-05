package com.example.core.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 密钥管理 + 字符串级加解密。
 *
 * 【它和 [CryptoEngine] 的分工】
 * - [CryptoEngine]：纯算法，输入 `ByteArray` 密钥，输出 `ByteArray`，无 Android 依赖
 * - 本类：负责**密钥从哪来、存到哪去**，以及和数据库字符串之间的 Base64 转换
 *
 * 【密钥存在哪里、为什么这么存】
 * AES 密钥不能明文存在 SharedPreferences 或代码里，否则 root 设备或备份提取就能拿到。
 * `EncryptedSharedPreferences` 的做法是：
 * 1. 在 **Android Keystore**（硬件支持时由 TEE/StrongBox 保护）里生成一个"主密钥"；
 * 2. 用这个主密钥加密 SharedPreferences 里的**键和值**；
 * 3. 主密钥本身永远不出 Keystore，只能"用它加解密"，不能读出来。
 *
 * 因此即使把 prefs 文件拷走，没有 Keystore 里的主密钥也解不开。
 *
 * 【Java 思维对照】
 * `@Singleton` + `@Inject constructor` 相当于 Spring 的单例 Bean；
 * `@ApplicationContext` 保证注入的是 Application 级 Context，避免持有 Activity 造成内存泄漏。
 *
 * 【注意：本类不能写 JVM 单元测试】
 * 它依赖 `android.util.Base64` 和 EncryptedSharedPreferences，只能在设备/仪器测试里跑。
 * 所以真正的算法测试放在 `CryptoEngineTest` 里，测的是 [CryptoEngine]。
 */
@Singleton
class CryptoManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * 加密后的 SharedPreferences。
     *
     * `by lazy` 表示第一次用的时候才创建（创建过程涉及 Keystore，略有耗时），
     * 且只会创建一次。
     */
    private val securePrefs: SharedPreferences? by lazy { createSecurePrefs() }

    /**
     * 加密一段明文，返回可以直接存进数据库的字符串。
     *
     * @return Base64 字符串；密钥不可用或加密失败时返回 null
     */
    fun encrypt(plaintext: String): String? {
        val key = encryptionKey() ?: return null
        return try {
            val payload = CryptoEngine.encrypt(key, plaintext)
            // NO_WRAP：不要插入换行符，否则存进数据库再读出来会解析失败
            Base64.encodeToString(payload, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 解密数据库里存的字符串。
     *
     * @return 明文；**任何失败（Base64 非法、密钥不匹配、数据被篡改）都返回 null**，
     *         由调用方决定是显示"无法解密"还是保持密文原样
     */
    fun decrypt(payload: String): String? {
        val key = encryptionKey() ?: return null
        return try {
            val bytes = Base64.decode(payload, Base64.NO_WRAP)
            CryptoEngine.decrypt(key, bytes)
        } catch (e: IllegalArgumentException) {
            // 不是合法的 Base64 —— 说明这条数据根本不是密文（可能是历史明文数据）
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 取 AES-256 密钥；不存在时生成一个并持久化。
     *
     * 【为什么用"读不到就生成"而不是在 Application.onCreate 里初始化】
     * 密钥只在真正需要加密/解密时才用到。按需初始化可以避免：
     * - App 启动时多一次 Keystore 访问（略慢）
     * - 用户从不使用加密功能时也生成密钥
     */
    private fun encryptionKey(): ByteArray? {
        val prefs = securePrefs ?: return null
        val stored = prefs.getString(KEY_AES, null)
        if (stored != null) {
            return try {
                Base64.decode(stored, Base64.NO_WRAP)
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        // 首次使用：生成并保存
        return try {
            val generated = CryptoEngine.generateKey()
            prefs.edit()
                .putString(KEY_AES, Base64.encodeToString(generated, Base64.NO_WRAP))
                .apply()
            generated
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 创建加密的 SharedPreferences。
     *
     * 失败时返回 null 而不是崩溃：某些设备（极旧的系统、Keystore 损坏）
     * 可能创建失败，此时加密功能降级为"不可用"，但笔记 App 的其他功能照常。
     */
    private fun createSecurePrefs(): SharedPreferences? = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        null
    }

    private companion object {
        /** 存放 AES 密钥的加密 prefs 文件名。 */
        const val PREFS_NAME = "secure_note_keys"

        /** AES-256 密钥在 prefs 里的键名。 */
        const val KEY_AES = "aes_key_v1"
    }
}
