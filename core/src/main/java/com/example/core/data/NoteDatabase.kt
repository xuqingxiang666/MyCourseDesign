package com.example.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
 * - `version`：schema 版本号。**修改实体字段结构时必须 +1**，
 *   否则运行时会抛 `IllegalStateException: Room cannot verify the data integrity`。
 * - `exportSchema = true`：把 schema 导成 JSON 文件，用于版本迁移对比与代码审查。
 *
 * 【版本历史】
 * - v1：初始版本（id/title/content/isArchived/isPinned/isEncrypted/lastModified/version）
 * - v2：新增 `encryptedAt` 字段，用于记录加密时间（配合加密与生物识别功能）
 */
@Database(
    entities = [Note::class],
    version = 2,
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

        /**
         * 版本 1 → 2 的迁移：给 notes 表增加 `encryptedAt` 列。
         *
         * 【为什么必须写 Migration 而不能只把 version 改成 2】
         * Room 启动时会比对"代码声明的 schema"和"磁盘上数据库的 schema"。
         * 如果版本号变了但没有对应 Migration，Room 会抛
         * `IllegalStateException: A migration from 1 to 2 was required but not found`。
         *
         * `DatabaseModule` 里虽然还留着 `fallbackToDestructiveMigration()`，
         * 但那只在**找不到迁移**时才生效——它会**删库重建，用户数据全丢**。
         * 有了下面的 Migration，升级时数据会完整保留，这是正确做法。
         *
         * 【SQL 说明】
         * SQLite 的 `ALTER TABLE ... ADD COLUMN` 支持指定 NOT NULL + DEFAULT，
         * 因此已有行会自动填入默认值 0（表示"从未加密"），不需要手动回填。
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE notes ADD COLUMN encryptedAt INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}
