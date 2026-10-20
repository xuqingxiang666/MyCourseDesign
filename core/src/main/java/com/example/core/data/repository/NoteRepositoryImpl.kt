package com.example.core.data.repository

import com.example.core.data.dao.NoteDao
import com.example.core.data.entity.Note
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
    private val noteDao: NoteDao
) : NoteRepository {

    // ==================== 查询 ====================

    override fun observeActiveNotes(): Flow<List<Note>> = noteDao.getActiveNotes()

    override fun observeArchivedNotes(): Flow<List<Note>> = noteDao.getArchivedNotes()

    override suspend fun getAllNotesOnce(): List<Note> = noteDao.getAllNotesOnce()

    override suspend fun getNoteById(id: Long): Note? = noteDao.getNoteById(id)

    override suspend fun getNoteCount(): Int = noteDao.getNoteCount()

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

    override suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    override suspend fun deleteNoteById(id: Long) = noteDao.deleteNoteById(id)

    override suspend fun setArchived(id: Long, archived: Boolean) =
        noteDao.setArchived(id, archived)

    override suspend fun setPinned(id: Long, pinned: Boolean) = noteDao.setPinned(id, pinned)

    override suspend fun deleteAllNotes() = noteDao.deleteAllNotes()
}
