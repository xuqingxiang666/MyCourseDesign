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

    /** 观察所有已加密的笔记，供"加密笔记"视图使用。 */
    fun observeEncryptedNotes(): Flow<List<Note>>

    /** 已加密笔记数量。 */
    suspend fun getEncryptedNoteCount(): Int

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

    /**
     * 一次性更新"正文 + 加密标记 + 加密时间"。
     *
     * 加密/解密是三步改动的原子操作，调用方不应自己拼 `updateNote(note.copy(...))`，
     * 否则容易漏改 `encryptedAt` 或忘记递增 `version`，导致数据不一致。
     *
     * @param id 笔记主键
     * @param encrypted 操作后是否加密
     * @param content 操作后的正文（加密时为密文，解密时为明文）
     */
    suspend fun setEncryption(id: Long, encrypted: Boolean, content: String)

    /**
     * 加密一条笔记：把正文换成密文，并标记 `isEncrypted = true`。
     *
     * @return true 表示成功；false 表示密钥不可用或加密失败（此时数据库**不做任何改动**）
     */
    suspend fun encryptNote(note: Note): Boolean

    /**
     * 读取一条笔记的明文。
     *
     * - 未加密的笔记：直接返回 `content`
     * - 已加密的笔记：解密后返回明文；解密失败返回 null
     *
     * **只读**，不会修改数据库。要在 UI 上"取消加密"时，
     * 调用方拿到明文后再自行调用 [setEncryption] 落库。
     */
    suspend fun decryptNote(note: Note): String?

    /** 清空全部笔记，仅测试使用。 */
    suspend fun deleteAllNotes()
}
