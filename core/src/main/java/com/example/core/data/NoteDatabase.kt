package com.example.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.core.data.dao.NoteDao
import com.example.core.data.dao.RemoteNoteDao
import com.example.core.data.entity.Note
import com.example.core.data.entity.RemoteNote

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
 * - v3：新增 `isDeleted` / `deletedAt`（软删除，供同步冲突解决使用），
 *       并创建 `remote_notes` 表扮演云端
 */
@Database(
    entities = [Note::class, RemoteNote::class],
    version = 3,
    exportSchema = true
)
abstract class NoteDatabase : RoomDatabase() {

    /**
     * 返回笔记表的 DAO。
     *
     * Room 生成的实现类会自动 new 出 `NoteDao_Impl` 并缓存，多次调用返回同一实例。
     */
    abstract fun noteDao(): NoteDao

    /**
     * 返回"云端"笔记表的 DAO。
     *
     * 注意它不是 Rem 的远端服务，而是同一数据库里的另一张表，
     * 用来在课设范围内模拟同步——详见 [RemoteNote] 的类注释。
     */
    abstract fun remoteNoteDao(): RemoteNoteDao

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

        /**
         * 版本 2 → 3 的迁移：给 notes 增加软删除字段，并创建 remote_notes 表。
         *
         * 【两件事都要做】
         * 1. `ALTER TABLE notes ADD COLUMN ...` 给已有表加两列
         * 2. `CREATE TABLE remote_notes ...` 建新表
         *
         * 【CREATE TABLE 的 SQL 必须和 Room 期望的完全一致】
         * Room 启动时会把"磁盘上实际的表结构"和"实体声明的表结构"逐列比对：
         * 列名、类型、是否 NOT NULL、默认值、主键位置，任何一项不同都会抛
         * `IllegalStateException: Migration didn't properly handle: remote_notes(...)`。
         * 所以这里的 SQL 是照着 Room 的生成规则手写的：主键写成表级 `PRIMARY KEY(id)`，
         * 所有列 NOT NULL 且**不写 DEFAULT**（实体侧也没有 default，两边才一致）。
         *
         * 【坑：不要在这里给列加 DEFAULT】
         * 上一版迁移 `encryptedAt` 就踩过：实体没有声明默认值时，
         * SQL 里写了 `DEFAULT 0` 会导致两边对不上。要么两边都写（用
         * `@ColumnInfo(defaultValue = "0")`），要么两边都不写。
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN deletedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS remote_notes (" +
                        "id INTEGER NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "content TEXT NOT NULL, " +
                        "isEncrypted INTEGER NOT NULL, " +
                        "isArchived INTEGER NOT NULL, " +
                        "isPinned INTEGER NOT NULL, " +
                        "isDeleted INTEGER NOT NULL, " +
                        "deletedAt INTEGER NOT NULL, " +
                        "lastModified INTEGER NOT NULL, " +
                        "version INTEGER NOT NULL, " +
                        "PRIMARY KEY(id))"
                )
            }
        }
    }
}
