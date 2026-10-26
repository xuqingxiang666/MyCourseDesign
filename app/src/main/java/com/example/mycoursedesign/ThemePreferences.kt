package com.example.mycoursedesign

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * 主题偏好设置：保存并应用"跟随系统 / 浅色 / 深色"的选择。
 *
 * 【为什么需要这个类】
 * 项目用的是 `Theme.Material3.DayNight.NoActionBar`，它默认会**跟随系统**的深色开关。
 * 但"跟随系统"只是被动行为，用户没法在 App 内主动选择深浅色。
 * 本类就是补上这个能力，并把选择持久化下来。
 *
 * 【AppCompatDelegate 的三种夜间模式】
 * - `MODE_NIGHT_FOLLOW_SYSTEM`(-1)：跟随系统设置（默认）
 * - `MODE_NIGHT_NO`(1)：强制浅色
 * - `MODE_NIGHT_YES`(2)：强制深色
 *
 * 调用 `setDefaultNightMode(...)` 后，AppCompat 会让当前所有 Activity
 * **自动重建**（效果类似旋转屏幕），所以界面会立刻切换，不需要手动 refresh。
 *
 * 【Java 思维对照】
 * 相当于一个静态的工具类 + `SharedPreferences` 读写，没有特别的 Kotlin 语法。
 * `object` 关键字表示"单例对象"，等价于 Java 里只含静态成员的类。
 */
object ThemePreferences {

    private const val PREFS_NAME = "app_settings"
    private const val KEY_NIGHT_MODE = "night_mode"

    /**
     * 支持的三种模式，顺序与设置对话框里的选项一一对应。
     *
     * 直接使用 [AppCompatDelegate] 的常量作为存储值，避免"自有枚举值 ↔ 框架常量"
     * 再做一层映射，少一层出错的机会。
     */
    val supportedModes: List<Int> = listOf(
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        AppCompatDelegate.MODE_NIGHT_NO,
        AppCompatDelegate.MODE_NIGHT_YES
    )

    /** 读取已保存的模式；没有保存过时默认"跟随系统"。 */
    fun currentMode(context: Context): Int =
        prefs(context).getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    /**
     * 应用已保存的模式。
     *
     * 在 [MyApplication.onCreate] 里调用，保证 App 一启动就用上用户选的主题，
     * 而不是先闪一下系统默认主题再切换。
     */
    fun applyStoredMode(context: Context) {
        AppCompatDelegate.setDefaultNightMode(currentMode(context))
    }

    /** 保存并立即应用新模式。 */
    fun saveMode(context: Context, mode: Int) {
        prefs(context).edit().putInt(KEY_NIGHT_MODE, mode).apply()
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
