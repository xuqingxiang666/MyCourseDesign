package com.example.core.data.remote

import com.example.core.data.dao.RemoteNoteDao
import com.example.core.data.entity.RemoteNote
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用本地数据库表模拟"云端"的 [RemoteDataSource] 实现。
 *
 * 【为什么这就够了】
 * 同步机制要考的是"两条数据不一致时怎么判"，而不是"HTTP 请求怎么写"。
 * 用一张表代替服务器，行为上完全等价：
 * - 云端有数据 → 表里有行
 * - 云端收到更新 → 表里行被覆盖
 * - 云端删除 → 行上 `isDeleted = 1`
 *
 * 而且它有真实服务器没有的三个好处：
 * 1. **离线可演示**：答辩现场不需要网络；
 * 2. **可单元测试**：可以直接构造数据断言同步结果；
 * 3. **可重复**：一键重置演示数据，随时能重演同一个冲突场景。
 *
 * 【Java 思维对照】
 * 相当于一个 DAO 之上再包一层的 `LocalRemoteRepository`，
 * 用 `@Singleton` 保证全局只有一个实例（避免多个实例看到不同数据）。
 */
@Singleton
class LocalRemoteDataSource @Inject constructor(
    private val remoteNoteDao: RemoteNoteDao
) : RemoteDataSource {

    override suspend fun fetchAll(): List<RemoteNote> = remoteNoteDao.getAll()

    override suspend fun upsert(note: RemoteNote) = remoteNoteDao.upsert(note)

    override suspend fun count(): Int = remoteNoteDao.count()

    override suspend fun clear() = remoteNoteDao.clear()
}
