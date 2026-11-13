package com.example.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.core.data.entity.RemoteNote

/**
 * "云端"笔记表的 DAO。
 *
 * 【和 NoteDao 的关键区别：没有 Flow】
 * 本地笔记要"观察"（数据一变 UI 自动刷新），所以返回 `Flow`。
 * 但云端表只是同步时的数据源，UI 不订阅它，用普通的 `suspend` 一次性读取即可，
 * 既省内存也避免无谓的订阅。
 *
 * 【全部写操作用 REPLACE 策略】
 * 同步的语义是"以某一版为准，整行覆盖"。用 `@Insert(onConflict = REPLACE)`
 * 就能同时处理"插入新行"和"更新已有行"两种情况，不需要先查再决定 insert 还是 update。
 * 对应 SQL 层面的 `INSERT OR REPLACE`。
 */
@Dao
interface RemoteNoteDao {

    /** 取出全部云端笔记。 */
    @Query("SELECT * FROM remote_notes ORDER BY lastModified DESC")
    suspend fun getAll(): List<RemoteNote>

    /** 按 id 查询单条云端笔记。 */
    @Query("SELECT * FROM remote_notes WHERE id = :id")
    suspend fun getById(id: Long): RemoteNote?

    /** 写入或覆盖一条云端笔记。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: RemoteNote)

    /** 批量写入或覆盖。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(notes: List<RemoteNote>)

    /** 按 id 删除云端笔记（真正的删除，仅用于测试或重置演示数据）。 */
    @Query("DELETE FROM remote_notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** 清空云端表。 */
    @Query("DELETE FROM remote_notes")
    suspend fun clear()

    /** 云端笔记数量，演示页显示"云端 N 条"用。 */
    @Query("SELECT COUNT(*) FROM remote_notes")
    suspend fun count(): Int
}
