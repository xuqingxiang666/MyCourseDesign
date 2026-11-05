package com.example.core.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 笔记实体类，对应数据库中的 `notes` 表。
 *
 * 【Java 思维对照】
 * 这相当于 JPA / Hibernate 里的 POJO 实体类：
 * - `@Entity(tableName = "notes")` 相当于 JPA 的 `@Entity` + `@Table(name = "notes")`
 * - `@PrimaryKey(autoGenerate = true)` 相当于 `@Id` + `@GeneratedValue(strategy = IDENTITY)`
 * - `data class` 是 Kotlin 的语法糖，编译器自动生成 `equals()` / `hashCode()` /
 *   `toString()` / `copy()`，省掉 Java 里那几十行样板代码。
 *
 * 【为什么用 data class 而不是普通 class】
 * Room 的 `DiffUtil` 判断"条目内容是否变化"依赖 `equals()`，
 * 冲突解决里的"内容是否相同"比较也依赖它，所以值语义（value semantics）是必需的。
 */
@Entity(tableName = "notes")
data class Note(

    /** 主键，自增。`id == 0` 表示这是一条尚未入库的新笔记。 */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 标题。允许为空字符串，列表中会显示为"无标题"。 */
    val title: String = "",

    /** 正文内容。支持 Markdown 语法（阶段 5 实现实时预览）。 */
    val content: String = "",

    /** 是否已归档。归档的笔记不出现在主列表，但不会被删除。 */
    val isArchived: Boolean = false,

    /** 是否置顶。置顶的笔记排在列表最前面。 */
    val isPinned: Boolean = false,

    /**
     * 内容是否已加密。
     *
     * 为 true 时，[content] 里存放的不是明文，而是
     * `Base64(IV + AES-GCM 密文)` 形式的字符串。
     * 之所以复用 [content] 而不另开一个字段，是为了让"加密/解密"变成
     * 同一字段的两种编码，列表、搜索、排序等既有逻辑都不需要改动。
     */
    val isEncrypted: Boolean = false,

    /**
     * 加密时间戳（毫秒）。`0` 表示从未加密过。
     *
     * 用途有两个：
     * 1. UI 上显示"加密于 X 月 X 日"，让用户知道这条笔记处于保护状态；
     * 2. 解密时可以判断"密文是不是在这次修改之前产生的"，
     *    避免把刚编辑过的新明文误当成旧密文。
     *
     * 数据库版本 1 → 2 时新增此列（见 `NoteDatabase.MIGRATION_1_2`）。
     *
     * 【★ 为什么必须写 @ColumnInfo(defaultValue = "0")】
     * Kotlin 里的 `= 0L` 只是**代码层**的默认值，Room 建表时并不会把它写进 DDL。
     * 而迁移用的 `ALTER TABLE ... ADD COLUMN ... NOT NULL DEFAULT 0` 会在
     * SQLite 里真的留下 `DEFAULT 0`。
     *
     * Room 启动时会比对"实体声明的表结构"和"迁移后实际得到的表结构"，
     * `defaultValue` 对不上就抛：
     *     IllegalStateException: Migration didn't properly handle: notes(...)
     * 所以必须用 `@ColumnInfo(defaultValue = "0")` 让**实体侧也声明默认值**，
     * 两边才能一致。这是 Room 迁移里最容易被忽略的细节之一。
     */
    @ColumnInfo(defaultValue = "0")
    val encryptedAt: Long = 0L,

    /**
     * 最后修改时间戳（毫秒）。
     *
     * ★ 这是"冲突解决机制"的核心字段。
     * 两条数据发生冲突时，比较此字段，值大的获胜（Last-Write-Wins 策略）。
     */
    val lastModified: Long = System.currentTimeMillis(),

    /**
     * 数据版本号，每次修改自增 1。
     *
     * ★ 这是冲突解决的次级判据：当 [lastModified] 完全相同时（例如同一毫秒内的修改），
     * 用版本号决定胜负，保证结果始终确定、可复现。
     */
    val version: Int = 1
)
