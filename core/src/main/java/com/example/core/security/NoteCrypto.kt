package com.example.core.security

/**
 * 笔记加解密能力的最小接口。
 *
 * 【为什么要抽出接口，而不是让 Repository 直接依赖 CryptoManager】
 * `CryptoManager` 依赖 Android 的 `Context` 与 `EncryptedSharedPreferences`，
 * 在 JVM 单元测试里根本构造不出来。于是依赖它的 `NoteRepositoryImpl`
 * 就跟着没法测 —— 覆盖率上不去就是这么来的（改造前 core 只有 46%）。
 *
 * 抽出接口后：
 * - 生产代码注入 `CryptoManager`（Hilt 绑定见 RepositoryModule）
 * - 单元测试注入一个假的实现，**不需要 Android**
 *
 * 这就是"依赖倒置"最实际的收益：可测性。
 */
interface NoteCrypto {

    /** 加密明文。@return 密文；加密不可用时返回 null（调用方必须处理，不能当成空串） */
    fun encrypt(plaintext: String): String?

    /** 解密密文。@return 明文；解不开（密钥变更/数据损坏）返回 null */
    fun decrypt(ciphertext: String): String?
}
