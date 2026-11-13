package com.example.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * "云端"笔记副本。
 *
 * 【为什么要有这张表】
 * 课设要求"离线-同步冲突解决机制"。真正接入后端会带来三个问题：
 * 1. 需要新增网络依赖（本项目版本全部锁定、离线环境，不允许）；
 * 2. 演示时必须联网，答辩现场一旦网络不通就演不出来；
 * 3. 没法写单元测试（测试要连服务器，慢且不稳定）。
 *
 * 所以用一种等价但成本低得多的做法：**在同一个数据库里放一张"远端表"**，
 * 它扮演云端的角色：
 * - `notes`      = 本机数据
 * - `remote_notes` = 云端数据
 *
 * 同步就是把两张表对齐。冲突判定逻辑完全不关心"远端"是数据库还是 HTTP，
 * 所以将来换成真实后端时，只要换掉 [com.example.core.data.remote.RemoteDataSource]
 * 的实现即可，**冲突解决代码一行都不用改**。
 *
 * 【为什么不直接复用 Note 实体】
 * 两者字段高度相似但**语义不同**：
 * - [Note] 有 `encryptedAt`（本机的加密时间，云端不关心）
 * - 远端表没有 `@PrimaryKey(autoGenerate = true)`（id 由本地分配后上传，不能自增）
 * 硬复用一个类会让字段含义变得模糊，也让未来的迁移更难。分开更清晰。
 *
 * 【加密笔记怎么同步】
 * `content` 直接存密文（Base64 字符串）。也就是说**云端拿到的也是密文**，
 * 没有本机 Keystore 里的密钥无法解开 —— 这是加密与同步结合后的安全优势。
 */
@Entity(tableName = "remote_notes")
data class RemoteNote(

    /**
     * 与本地 [Note.id] 相同的标识，代表"这是同一条笔记"。
     *
     * ★ 不能用自增：id 是本地创建时分配的，同步时原样带到云端，
     * 两边靠这个 id 配对。若用自增，同一逻辑笔记在两边会得到不同 id，永远配不上。
     */
    @PrimaryKey
    val id: Long,

    val title: String = "",

    /** 正文。加密笔记这里存的是密文。 */
    val content: String = "",

    val isEncrypted: Boolean = false,

    val isArchived: Boolean = false,

    val isPinned: Boolean = false,

    /** 云端视角的"已删除"。 */
    val isDeleted: Boolean = false,

    val deletedAt: Long = 0L,

    /** 最后修改时间（毫秒），LWW 的主要判据。 */
    val lastModified: Long = 0L,

    /** 数据版本号，时间戳相同时的次级判据。 */
    val version: Int = 1
) {

    companion object {

        /**
         * 由本地笔记生成云端副本。
         *
         * 【Java 思维对照】相当于一个静态工厂方法 `RemoteNote.from(note)`。
         */
        fun from(note: Note): RemoteNote = RemoteNote(
            id = note.id,
            title = note.title,
            content = note.content,
            isEncrypted = note.isEncrypted,
            isArchived = note.isArchived,
            isPinned = note.isPinned,
            isDeleted = note.isDeleted,
            deletedAt = note.deletedAt,
            lastModified = note.lastModified,
            version = note.version
        )
    }
}
