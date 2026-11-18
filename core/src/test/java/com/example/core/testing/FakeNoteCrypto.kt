package com.example.core.testing

import com.example.core.security.NoteCrypto

/** 假加解密：只是加前缀，足够验证"加密/解密走对了路径"。 */
class FakeNoteCrypto(
    var available: Boolean = true
) : NoteCrypto {

    companion object {
        const val PREFIX = "enc:"
    }

    override fun encrypt(plaintext: String): String? =
        if (available) PREFIX + plaintext else null

    override fun decrypt(ciphertext: String): String? {
        if (!available) return null
        return if (ciphertext.startsWith(PREFIX)) ciphertext.removePrefix(PREFIX) else null
    }
}
