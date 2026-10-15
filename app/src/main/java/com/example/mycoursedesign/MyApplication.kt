package com.example.mycoursedesign

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 【Java 思维对照】：相当于你在 AndroidManifest 里注册的自定义 Application，并在 onCreate 里初始化全局变量。
 * `@HiltAndroidApp` 相当于触发了 Hilt 的全局初始化，必须加！
 */
@HiltAndroidApp
class MyApplication : Application()