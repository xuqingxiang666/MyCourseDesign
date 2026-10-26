package com.example.mycoursedesign

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 应用入口。
 *
 * 【Java 思维对照】
 * 相当于你在 AndroidManifest 里注册的自定义 Application，并在 onCreate 里初始化全局变量。
 * `@HiltAndroidApp` 相当于触发 Hilt 的全局初始化，必须加。
 */
@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 应用用户上次选择的主题（跟随系统 / 浅色 / 深色）。
        // 放在 Application 里而不是 Activity 里，是为了保证进程启动时就生效一次，
        // 避免每个 Activity 各自应用一遍、也避免界面先渲染成错误主题再闪一下。
        ThemePreferences.applyStoredMode(this)
    }
}
