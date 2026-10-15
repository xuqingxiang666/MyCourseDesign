package com.example.core.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.core.data.entity.Note
import kotlinx.coroutines.flow.Flow

/**
 * 笔记数据访问接口（DAO = Data Access Object）。
 *
 * 【Java 思维对照】
 * 相当于 MyBatis 的 Mapper 接口或 Spring Data JPA 的 Repository：
 * 你只声明方法签名，Room 在编译期用注解处理器（KSP）自动生成实现类。
 * 你不需要写任何 SQL 执行代码。
 *
 * 【为什么查询返回 Flow 而增删改是 suspend】
 * - `Flow<List<Note>>`：相当于 Java 的 LiveData，是"可观察查询"。数据库内容一变，
 *   订阅者会自动收到新数据，UI 无需手动调用 requery。
 * - `suspend` 函数：相当于 Java 中放到后台线程执行的方法（如 AsyncTask / Executor）。
 *   Room 禁止在主线程操作数据库，`suspend` 让协程自动切到 IO 线程。
 */
@Dao
interface NoteDao {

    // ==================== 查询 ====================

    /**
     * 获取所有"活跃"笔记（未归档），供主界面列表使用。
     *
     * 排序规则：置顶的排在前面；同为置顶或同为非置顶时，按最后修改时间倒序（最新的在最上面）。
     */
    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY isPinned DESC, lastModified DESC")
    fun getActiveNotes(): Flow<List<Note>>

    /** 获取已归档的笔记，按修改时间倒序。 */
    @Query("SELECT * FROM notes WHERE isArchived = 1 ORDER BY lastModified DESC")
    fun getArchivedNotes(): Flow<List<Note>>

    /**
     * 一次性取出全部笔记（不分页、不观察）。
     *
     * 供 P4 分享卡片模块导出 CSV、批量生成卡片时使用 —— 那里需要的是"当前快照"，
     * 不需要持续观察数据变化。
     */
    @Query("SELECT * FROM notes ORDER BY lastModified DESC")
    suspend fun getAllNotesOnce(): List<Note>

    /** 按主键查询单条笔记，不存在时返回 null。 */
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?

    /** 统计笔记总数，单元测试与 UI 空状态判断使用。 */
    @Query("SELECT COUNT(*) FROM notes")
    suspend fun getNoteCount(): Int

    // ==================== 增删改 ====================

    /**
     * 插入笔记，返回新行的主键 id。
     *
     * 注意：因为 [Note.id] 有默认值 0 且是自增主键，传入 id = 0 的对象时
     * Room 会当作"新记录插入"而不是"更新 id 为 0 的行"。
     */
    @Insert
    suspend fun insertNote(note: Note): Long

    /** 按主键整体更新一条笔记。 */
    @Update
    suspend fun updateNote(note: Note)

    /** 按主键删除一条笔记。 */
    @Delete
    suspend fun deleteNote(note: Note)

    /** 按主键删除，不需要先查出对象。 */
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    /** 清空整张表，仅测试使用。 */
    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()

    // ==================== 状态切换 ====================
    //
    // 下面两个方法带方法体，是 Kotlin 接口的"默认实现"。
    // Room 支持在 DAO 接口里写带实现的方法，只要用 @Transaction 标注，
    // Room 就会把整个方法体包在一个数据库事务里执行 —— 保证"读-改-写"三步原子完成，
    // 不会出现"读完被别的线程改了再写回"的竞态。

    /**
     * 设置归档状态。
     *
     * 【Java 思维对照】相当于 `@Transactional` 注解的方法：
     * `getNoteById` → `copy` → `updateNote` 必须整体成功或整体失败。
     */
    @Transaction
    suspend fun setArchived(id: Long, archived: Boolean) {
        getNoteById(id)?.let { note ->
            updateNote(
                note.copy(
                    isArchived = archived,
                    lastModified = System.currentTimeMillis(),
                    version = note.version + 1
                )
            )
        }
    }

    /** 设置置顶状态，同样保证原子性。 */
    @Transaction
    suspend fun setPinned(id: Long, pinned: Boolean) {
        getNoteById(id)?.let { note ->
            updateNote(
                note.copy(
                    isPinned = pinned,
                    lastModified = System.currentTimeMillis(),
                    version = note.version + 1
                )
            )
        }
    }
}
