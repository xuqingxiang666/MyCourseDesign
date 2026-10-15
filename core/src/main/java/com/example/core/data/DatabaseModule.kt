package com.example.core.data

import android.content.Context
import androidx.room.Room
import com.example.core.data.dao.NoteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt 依赖注入模块：负责创建并提供数据库与 DAO 实例。
 *
 * 【Java 思维对照】
 * 相当于 Spring 的 `@Configuration` 类：
 * - `@Module` ≈ `@Configuration`
 * - `@Provides` ≈ `@Bean`
 * - `@InstallIn(SingletonComponent::class)` ≈ 声明这些 Bean 的作用域是"整个应用单例容器"
 * - `@Singleton` ≈ Spring 默认的单例作用域
 *
 * 有了这个模块，ViewModel 里写 `@Inject constructor(private val noteDao: NoteDao)`
 * 就能自动拿到 DAO，不需要自己 new，也不需要用 Application 全局变量传递 Context。
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * 构建 Room 数据库实例。
     *
     * 【@ApplicationContext 的作用】
     * 注入的是 Application 级别的 Context 而不是 Activity 的，避免数据库单例
     * 持有 Activity 引用导致内存泄漏 —— 这是 Android 里非常经典的坑。
     *
     * 【@Singleton 的作用】
     * 保证整个 App 生命周期内只创建一次数据库连接池。
     * 如果少了它，每次注入都会新建一个数据库实例，会导致内存暴涨和数据不一致。
     */
    @Provides
    @Singleton
    fun provideNoteDatabase(
        @ApplicationContext context: Context
    ): NoteDatabase =
        Room.databaseBuilder(
            context,
            NoteDatabase::class.java,
            NoteDatabase.DATABASE_NAME
        )
            // 版本号变化时直接删库重建，而不是崩溃。
            // 【注意】这是课设阶段的简化做法；生产环境应改用 Migration 保留用户数据。
            .fallbackToDestructiveMigration()
            .build()

    /**
     * 从数据库实例中取出 DAO。
     *
     * 这里没有加 `@Singleton`：因为传入的 [NoteDatabase] 本身已经是单例，
     * 而 `database.noteDao()` 每次返回的是同一个缓存实例，再加 @Singleton 是冗余的。
     */
    @Provides
    fun provideNoteDao(database: NoteDatabase): NoteDao = database.noteDao()
}
