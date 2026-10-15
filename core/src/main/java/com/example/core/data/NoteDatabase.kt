package com.example.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.core.data.dao.NoteDao
import com.example.core.data.entity.Note

/**
 * 应用的 Room 数据库。
 *
 * 【Java 思维对照】
 * 相当于 Spring 里配置 DataSource + SessionFactory 的地方，
 * 只不过 Room 把这个过程简化成了一个抽象类：你只声明"有哪些表、有哪些 DAO"，
 * Room 在编译期生成 `NoteDatabase_Impl` 实现类。
 *
 * 【@Database 参数说明】
 * - `entities`：这个数据库包含哪些表（每个 @Entity 类对应一张表）。
 * - `version`：schema 版本号。修改实体字段结构时必须 +1，
 *   否则运行时会抛 `IllegalStateException: Room cannot verify the data integrity`。
 * - `exportSchema = true`：把 schema 导成 JSON 文件，用于版本迁移对比与代码审查。
 */
@Database(
    entities = [Note::class],
    version = 1,
    exportSchema = true
)
abstract class NoteDatabase : RoomDatabase() {

    /**
     * 返回笔记表的 DAO。
     *
     * Room 生成的实现类会自动 new 出 `NoteDao_Impl` 并缓存，多次调用返回同一实例。
     */
    abstract fun noteDao(): NoteDao

    companion object {
        /** 数据库文件名。存放在 `/data/data/<包名>/databases/note_database.db`。 */
        const val DATABASE_NAME = "note_database.db"
    }
}
