package com.example.core.di

import com.example.core.data.repository.NoteRepository
import com.example.core.data.repository.NoteRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 依赖绑定模块：把仓储接口绑定到它的实现。
 *
 * 【@Binds 是什么？—— 最容易和 @Provides 搞混的地方】
 *
 * | | [@Binds]（本文件用的） | [@Provides]（DatabaseModule 用的） |
 * |---|---|---|
 * | 适用场景 | 接口 → 实现，**只是换个类型名** | 需要自己写创建逻辑（new 对象、调 Builder） |
 * | 方法体 | **不能有**，方法声明为 `abstract` | 必须有方法体，`return` 一个对象 |
 * | 参数 | 只能有 1 个，且必须是实现类 | 任意个 |
 * | 性能 | 编译期生成，几乎零开销 | 每次调用都执行方法体 |
 *
 * 所以：
 * - `NoteRepository → NoteRepositoryImpl` 只是"告诉 Hilt 用哪个实现"，用 `@Binds`；
 * - `Context → NoteDatabase` 需要调用 `Room.databaseBuilder(...)`，用 `@Provides`。
 *
 * 【Java 思维对照】
 * `@Binds` 相当于 Spring 里 `@Autowired` 配合"接口只有一个实现"时的自动装配，
 * 或者手工配置的 `@Bean` 返回 `new UserRepositoryImpl(userDao)`。
 * 区别是 `@Binds` 更省事：Hilt 会自己调用 `NoteRepositoryImpl` 的构造器，
 * 你连 `new` 都不用写。
 *
 * 【为什么用 abstract class 而不是 object】
 * `@Binds` 方法必须是抽象的（它没有方法体），抽象方法不能放在 `object` 里，
 * 所以这个 Module 必须声明为 `abstract class`。这一点和 DatabaseModule
 * （用 `object` + `@Provides`）不同，是新手最容易写错的地方。
 *
 * 【为什么用 @Singleton】
 * 保证整个 App 生命周期内只创建**一个** NoteRepositoryImpl 实例。
 * 虽然本例中它无状态、多创建几个也不影响正确性，但统一为单例更符合规范，
 * 也能避免将来加入缓存后出现"每个 ViewModel 各有一份缓存"的问题。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * 声明：当有人请求 [NoteRepository] 时，提供一个 [NoteRepositoryImpl]。
     *
     * Hilt 看到这个绑定后，会自动调用 `NoteRepositoryImpl` 的 `@Inject` 构造器，
     * 而该构造器需要 [com.example.core.data.dao.NoteDao]——DAO 又由
     * [com.example.core.data.DatabaseModule] 提供，于是整条依赖链自动打通。
     */
    @Binds
    @Singleton
    abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository
}
