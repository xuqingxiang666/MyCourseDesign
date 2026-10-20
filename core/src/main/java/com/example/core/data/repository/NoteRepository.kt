package com.example.core.data.repository

import com.example.core.data.entity.Note
import kotlinx.coroutines.flow.Flow

/**
 * 笔记仓储接口（Repository）。
 *
 * 【为什么要多这一层？—— Clean Architecture 的核心约束】
 *
 * 在加入本层之前，`app` 模块的 `NoteViewModel` 是直接注入 `NoteDao` 的：
 *     class NoteViewModel @Inject constructor(private val noteDao: NoteDao)
 * 这样虽然能跑，但有两个隐患：
 *
 * 1. **UI 层与数据库实现强耦合**。如果将来要把数据源从 Room 换成网络接口
 *    （比如加个云端同步），就得改 ViewModel；而 ViewModel 一改，单元测试全废。
 * 2. **无法做单元测试**。直接依赖 `NoteDao` 意味着测试时必须构造真实的 Room 数据库，
 *    测试跑得慢，还要用 Robolectric 或仪器测试。
 *
 * 加入 Repository 后，依赖方向变成：
 *     ViewModel → NoteRepository（接口） ← NoteRepositoryImpl → NoteDao
 * ViewModel 只认接口，测试时传一个假的实现即可，**这是单元测试覆盖率能上 60% 的前提**。
 *
 * 【Java 思维对照】
 * 相当于 Spring 里的 `UserRepository` 接口 + `UserRepositoryImpl` 实现，
 * 配合 `@Service` 注入。区别只是这里用 Hilt 的 `@Binds` 来完成接口到实现的绑定。
 *
 * 【为什么查询返回 Flow，写操作是 suspend】
 * 见 [com.example.core.data.dao.NoteDao] 中的说明，此处保持一致：
 * 读操作需要持续观察变化，写操作只需要一次性完成。
 */
interface NoteRepository {

    // ==================== 查询 ====================

    /**
     * 观察所有未归档的笔记，按"置顶优先 + 修改时间倒序"排列。
     *
     * 这是主界面列表的数据源。返回 [Flow] 意味着数据一变 UI 自动刷新。
     */
    fun observeActiveNotes(): Flow<List<Note>>

    /** 观察已归档的笔记。 */
    fun observeArchivedNotes(): Flow<List<Note>>

    /** 一次性取出全部笔记（快照）。供 P4 分享卡片模块导出 CSV / 批量生成卡片使用。 */
    suspend fun getAllNotesOnce(): List<Note>

    /** 按主键查询单条笔记，不存在返回 null。 */
    suspend fun getNoteById(id: Long): Note?

    /** 笔记总数，用于空状态判断与测试断言。 */
    suspend fun getNoteCount(): Int

    // ==================== 写操作 ====================

    /**
     * 新建笔记，返回新记录的 id。
     *
     * 注意：调用方只需传标题与内容，[Note] 的其余字段（id / 时间戳 / 版本号等）
     * 由实现类填充默认值，避免 UI 层关心数据库细节。
     */
    suspend fun createNote(title: String, content: String): Long

    /** 更新一条笔记（整体覆盖）。 */
    suspend fun updateNote(note: Note)

    /** 删除一条笔记。 */
    suspend fun deleteNote(note: Note)

    /** 按主键删除，不需要先查出对象。 */
    suspend fun deleteNoteById(id: Long)

    /** 设置归档状态。 */
    suspend fun setArchived(id: Long, archived: Boolean)

    /** 设置置顶状态。 */
    suspend fun setPinned(id: Long, pinned: Boolean)

    /** 清空全部笔记，仅测试使用。 */
    suspend fun deleteAllNotes()
}
