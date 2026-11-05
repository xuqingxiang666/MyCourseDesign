package com.example.mycoursedesign

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * 生物识别（指纹 / 锁屏密码）验证的统一入口。
 *
 * 【为什么单独封装一个类】
 * `BiometricPrompt` 的用法比较啰嗦：要构造 Executor、实现回调、拼 PromptInfo，
 * 而且**只能在 FragmentActivity 上使用**。把它封装起来后，Activity 里只需一行调用：
 *
 * ```kotlin
 * BiometricGate.authenticate(this, title, subtitle, onSuccess = { ... }, onFailure = { ... })
 * ```
 *
 * 【为什么同时允许"锁屏密码"】
 * `setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)` 让用户在
 * 没录指纹、或指纹不好用（手湿/冬天干裂）时，可以退回输入锁屏密码。
 * 只允许指纹的话，用户一旦指纹失灵就永远打不开自己的笔记了。
 *
 * 【注意：允许 DEVICE_CREDENTIAL 时不能调用 setNegativeButtonText】
 * 这是 BiometricPrompt 的硬性限制（因为"取消"由系统的凭证界面自己提供）。
 * 若调用会抛 `IllegalArgumentException`。
 *
 * 【Java 思维对照】
 * `object` = 只含静态方法的工具类；`BiometricPrompt.AuthenticationCallback`
 * 是抽象类，用 Kotlin 的 `object : ...() {}` 匿名子类实现，等价于 Java 的匿名内部类。
 */
object BiometricGate {

    /**
     * 允许的认证方式：弱生物特征（指纹/人脸）+ 设备锁屏凭证（PIN/图案/密码）。
     *
     * 用 `BIOMETRIC_WEAK` 而不是 `BIOMETRIC_STRONG`，是因为模拟器和部分低端机
     * 只有弱生物特征；课设场景下"能验证身份"比"强度等级"更重要。
     */
    private const val AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    /**
     * 当前设备能否进行身份验证（是否设置了指纹或锁屏密码）。
     *
     * 必须**先检查再弹出**：若设备完全没设置锁屏，`authenticate()` 会直接失败，
     * 用户看到的是一个"闪一下就消失"的提示，体验很差。
     * 先检查就能给出明确指引（"请先在系统设置里设置锁屏密码"）。
     */
    fun canAuthenticate(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) ==
            BiometricManager.BIOMETRIC_SUCCESS

    /**
     * 弹出身份验证。
     *
     * @param activity 必须是 FragmentActivity（AppCompatActivity 本身就是）
     * @param title 对话框标题
     * @param subtitle 对话框副标题
     * @param onSuccess 验证通过回调（主线程）
     * @param onFailure 验证失败/取消回调，参数是系统给的提示文字（主线程）
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        // 回调切换到主线程执行，方便直接操作 UI
        val executor = ContextCompat.getMainExecutor(activity)

        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // 用户点取消、连续失败被锁定、硬件不可用等都会走到这里
                    onFailure(errString.toString())
                }

                // 单次识别失败（例如指纹没对准）由系统内部处理并允许重试，
                // 这里不需要额外动作，所以不重写 onAuthenticationFailed。
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()

        prompt.authenticate(promptInfo)
    }
}
