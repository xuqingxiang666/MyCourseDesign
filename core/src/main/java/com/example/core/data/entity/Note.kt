package com.example.core.data.entity

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

    /** 内容是否已加密（阶段 5 使用）。加密时 [content] 存放的是密文。 */
    val isEncrypted: Boolean = false,

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
