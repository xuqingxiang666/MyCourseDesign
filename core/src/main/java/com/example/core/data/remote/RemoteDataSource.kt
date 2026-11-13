package com.example.core.data.remote

import com.example.core.data.entity.RemoteNote

/**
 * "云端"数据源抽象。
 *
 * 【这是整个离线-同步设计里最重要的一个接口】
 * 冲突解决逻辑（[com.example.core.domain.SyncEngine]）只依赖这个接口，
 * **完全不知道远端到底是 SQLite 表还是 HTTP 服务器**。
 *
 * 于是：
 * - 课设/演示阶段：用 [LocalRemoteDataSource]（同一数据库里的 `remote_notes` 表），
 *   不联网也能完整演示冲突解决；
 * - 将来接真实后端：新写一个 `HttpRemoteDataSource` 实现本接口，
 *   **同步与冲突判定的代码一行都不用改**。
 *
 * 这正是 Clean Architecture 里"依赖倒置"的实际价值：
 * 高层策略（冲突怎么解决）不依赖低层细节（数据存在哪里）。
 *
 * 【Java 思维对照】
 * 相当于 Spring 里的 `RemoteNoteClient` 接口 + `LocalRemoteNoteClient` /
 * `HttpRemoteNoteClient` 两个实现，靠 `@Primary` 或配置切换。
 */
interface RemoteDataSource {

    /** 拉取云端全部笔记（包含已标记删除的，否则无法同步删除操作）。 */
    suspend fun fetchAll(): List<RemoteNote>

    /** 写入或覆盖一条云端笔记。 */
    suspend fun upsert(note: RemoteNote)

    /** 云端笔记数量，演示界面显示"云端 N 条"用。 */
    suspend fun count(): Int

    /** 清空云端数据，仅用于演示重置。 */
    suspend fun clear()
}
