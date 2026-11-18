package com.example.core.data.repository

import com.example.core.data.dao.NoteDao
import com.example.core.data.entity.Note
import com.example.core.data.entity.RemoteNote
import com.example.core.security.NoteCrypto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * [NoteRepository] 的默认实现：把所有调用委托给 [NoteDao]。
 *
 * 【Java 思维对照】
 * 相当于 Spring 里 `@Service` 标注的 `UserRepositoryImpl`：
 * 构造器注入 DAO，方法内部直接转调。用 Kotlin 的表达式体语法
 * （`= noteDao.xxx()`）可以省掉 `{ return ... }` 的样板代码。
 *
 * 【为什么现在只做纯委托、不加业务逻辑？】
 * 这一步的目标是"引入抽象但不改变行为"，好处是：
 * - 编译能独立通过，不会像上次那样引发连锁报错；
 * - 将来加缓存、加密、冲突解决等逻辑时，有地方可以加，而不用再动 UI 层。
 *
 * 【@Inject 注解在哪】
 * 这里只标了构造器注入，**接口到实现的绑定**由
 * [com.example.core.di.RepositoryModule] 里的 `@Binds` 完成。
 */
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val cryptoManager: NoteCrypto
) : NoteRepository {

    // ==================== 查询 ====================

    override fun observeActiveNotes(): Flow<List<Note>> = noteDao.getActiveNotes()

    override fun observeArchivedNotes(): Flow<List<Note>> = noteDao.getArchivedNotes()

    override suspend fun getAllNotesOnce(): List<Note> = noteDao.getAllNotesOnce()

    override suspend fun getNoteById(id: Long): Note? = noteDao.getNoteById(id)

    override suspend fun getNoteCount(): Int = noteDao.getNoteCount()

    override fun observeEncryptedNotes(): Flow<List<Note>> = noteDao.getEncryptedNotes()

    override suspend fun getEncryptedNoteCount(): Int = noteDao.getEncryptedNoteCount()

    // ==================== 写操作 ====================

    /**
     * 新建笔记。
     *
     * 这里刻意没有直接把 title/content 丢给调用方去构造 [Note]，
     * 而是由 Repository 负责填充时间戳与初始版本号 —— 因为
     * "新笔记的 lastModified 应该是创建时刻、version 应该是 1"
     * 属于**业务规则**，不该让 UI 层操心。
     *
     * 【关于 id = 0】
     * [Note.id] 是 `@PrimaryKey(autoGenerate = true)` 且有默认值 0，
     * 传入 id = 0 时 Room 会当作"新记录插入"并回填自增 id，不会插入一行 id 为 0 的数据。
     */
    override suspend fun createNote(title: String, content: String): Long =
        noteDao.insertNote(
            Note(
                title = title,
                content = content,
                lastModified = System.currentTimeMillis(),
                version = 1
            )
        )

    override suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    /**
     * 删除笔记（软删除）。
     *
     * 只打标记不真删 —— 详见 [Note.isDeleted]。时间戳由 Repository 统一生成，
     * 保证"删除"和"修改"用的是同一套时间基准，LWW 比较才有意义。
     */
    override suspend fun deleteNote(note: Note) =
        noteDao.softDelete(note.id, System.currentTimeMillis())

    override suspend fun deleteNoteById(id: Long) = noteDao.deleteNoteById(id)

    override suspend fun setArchived(id: Long, archived: Boolean) =
        noteDao.setArchived(id, archived)

    override suspend fun setPinned(id: Long, pinned: Boolean) = noteDao.setPinned(id, pinned)

    /**
     * 更新"正文 + 加密标记 + 加密时间"。
     *
     * 直接转交给 DAO 的 `@Transaction` 方法：三个字段要么一起改成功，
     * 要么一起不改，避免出现"标记为已加密但内容是明文"的不一致状态。
     */
    override suspend fun setEncryption(id: Long, encrypted: Boolean, content: String) =
        noteDao.setEncryption(id, encrypted, content)

    /**
     * 加密笔记正文。
     *
     * 设计要点：
     * 1. **已经是加密状态就直接返回 true**，避免把密文再加密一次（那会导致二次解密失败）
     * 2. 加密**失败时不改数据库**：宁可让用户看到"加密失败"，
     *    也不要出现"标记成已加密但内容还是明文"的不一致状态
     * 3. 落库走 DAO 的 `@Transaction` 方法，正文、标记、时间戳一起改
     */
    override suspend fun encryptNote(note: Note): Boolean {
        if (note.isEncrypted) return true

        val cipherText = cryptoManager.encrypt(note.content) ?: return false
        noteDao.setEncryption(note.id, encrypted = true, newContent = cipherText)
        return true
    }

    /**
     * 读取笔记明文（只读）。
     *
     * 未加密的笔记直接返回原文；已加密的解不开就返回 null，
     * 让 UI 显示"无法解密"而不是展示一堆 Base64 密文。
     */
    override suspend fun decryptNote(note: Note): String? {
        if (!note.isEncrypted) return note.content
        return cryptoManager.decrypt(note.content)
    }

    override suspend fun getAllNotesIncludingDeleted(): List<Note> =
        noteDao.getAllNotesIncludingDeleted()

    /**
     * 用远端版本覆盖本地。
     *
     * 【为什么用 updateNote 而不是 insert】
     * 同步下载分两种情况：
     * 1. 本地没有这条（远端新建）→ 需要 insert，id 由远端指定
     * 2. 本地有但较旧（远端更新）→ 需要 update
     *
     * 用 `@Insert(onConflict = REPLACE)` 语义最省事，但 `NoteDao.insertNote` 是
     * 普通 `@Insert`（冲突会抛异常）。所以这里先查本地是否存在，分别处理。
     *
     * 【保留 encryptedAt】
     * 那是"本机加密时间"，属于本机元数据，云端没有这个字段，不能被覆盖成 0，
     * 否则 UI 上"加密于 X"的信息会丢失。
     */
    override suspend fun applyRemoteNote(remote: RemoteNote) {
        val local = noteDao.getNoteById(remote.id)
        val merged = Note(
            id = remote.id,
            title = remote.title,
            content = remote.content,
            isArchived = remote.isArchived,
            isPinned = remote.isPinned,
            isEncrypted = remote.isEncrypted,
            encryptedAt = local?.encryptedAt ?: 0L,
            lastModified = remote.lastModified,
            version = remote.version,
            isDeleted = remote.isDeleted,
            deletedAt = remote.deletedAt
        )
        if (local == null) noteDao.insertNote(merged) else noteDao.updateNote(merged)
    }

    override suspend fun deleteAllNotes() = noteDao.deleteAllNotes()
}
