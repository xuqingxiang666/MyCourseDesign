package com.example.core.domain

import com.example.core.data.entity.Note
import com.example.core.data.entity.RemoteNote

/**
 * 一次同步中"两条记录都有、但内容不同"的冲突明细。
 *
 * 字段特意把本地/云端的时间、版本、删除状态都带出来，
 * 界面可以直接显示"为什么判给了本地"，答辩演示时一目了然。
 */
data class ConflictRecord(
    val id: Long,
    val localTitle: String,
    val localModified: Long,
    val localVersion: Int,
    val localDeleted: Boolean,
    val remoteTitle: String,
    val remoteModified: Long,
    val remoteVersion: Int,
    val remoteDeleted: Boolean,
    val winner: Winner,
    val reason: String
)

/**
 * 同步计划：只描述"该做什么"，**不直接改数据**。
 *
 * 【为什么要把"算"和"做"分开】
 * 1. `SyncEngine` 变成纯函数，可以脱离数据库和 Android 单元测试；
 * 2. 界面可以先展示"将要发生什么"，用户确认后再执行（可预览的同步）；
 * 3. 真正写库的代码集中在一处，容易保证事务性。
 */
data class SyncPlan(
    /** 需要把本地版本写到云端的笔记（本地独有，或本地版本胜出）。 */
    val uploads: List<Note> = emptyList(),
    /** 需要把云端版本写到本地的笔记（云端独有，或云端版本胜出）。 */
    val downloads: List<RemoteNote> = emptyList(),
    /** 两边都存在但内容不同的记录。 */
    val conflicts: List<ConflictRecord> = emptyList(),
    /** 两边完全一致、无需处理的条数。 */
    val unchangedCount: Int = 0
) {
    /** 没有任何需要执行的动作。 */
    val isEmpty: Boolean
        get() = uploads.isEmpty() && downloads.isEmpty()

    /** 给界面用的一句话摘要。 */
    fun summary(): String = buildString {
        append("上传 ${uploads.size} 条")
        append("、下载 ${downloads.size} 条")
        if (conflicts.isNotEmpty()) append("、冲突 ${conflicts.size} 条")
        if (unchangedCount > 0) append("、无变化 $unchangedCount 条")
    }
}

/**
 * 同步引擎：对比"本地列表"和"云端列表"，产出 [SyncPlan]。
 *
 * 这是整个离线-同步机制的核心，但它**没有任何 I/O**：
 * 输入两个列表，输出一个计划。所有数据库读写都由调用方（Repository）执行。
 *
 * 【配对规则：按 id 配对】
 * 两边的笔记靠 `id` 认定"是同一条"。这也是 [RemoteNote.id] 不能用自增的原因：
 * id 必须由创建方分配、同步时原样带到另一端，否则永远配不上。
 *
 * 【三种情况】
 * | 情况 | 处理 |
 * |---|---|
 * | 两边都有 | 交给 [ConflictResolver] 判胜负 |
 * | 只有本地有 | 上传（云端新增） |
 * | 只有云端有 | 下载（本地新增） |
 *
 * 注意"只有本地有"包括**本地已软删除**的情况 —— 那也要上传，
 * 否则云端会一直留着这条笔记，下次同步又会被下载回来（删了又复活）。
 */
object SyncEngine {

    /**
     * 生成同步计划。
     *
     * @param localNotes 本地全部笔记（**必须包含已删除的**，否则无法同步删除操作）
     * @param remoteNotes 云端全部笔记
     */
    fun plan(localNotes: List<Note>, remoteNotes: List<RemoteNote>): SyncPlan {
        val localById = localNotes.associateBy { it.id }
        val remoteById = remoteNotes.associateBy { it.id }

        val uploads = mutableListOf<Note>()
        val downloads = mutableListOf<RemoteNote>()
        val conflicts = mutableListOf<ConflictRecord>()
        var unchanged = 0

        // 用排序保证输出顺序稳定，方便测试断言与界面展示
        val allIds = (localById.keys + remoteById.keys).sorted()

        allIds.forEach { id ->
            val local = localById[id]
            val remote = remoteById[id]

            when {
                // ---------- 两边都有：判冲突 ----------
                local != null && remote != null -> {
                    val decision = ConflictResolver.resolve(local, remote)
                    when (decision.winner) {
                        Winner.IDENTICAL -> {
                            unchanged++
                        }

                        Winner.LOCAL -> {
                            uploads += local
                            conflicts += toRecord(local, remote, decision)
                        }

                        Winner.REMOTE -> {
                            downloads += remote
                            conflicts += toRecord(local, remote, decision)
                        }
                    }
                }

                // ---------- 只有本地有：上传（含"本地已删除"） ----------
                local != null -> {
                    uploads += local
                }

                // ---------- 只有云端有：下载 ----------
                remote != null -> {
                    downloads += remote
                }
            }
        }

        return SyncPlan(
            uploads = uploads,
            downloads = downloads,
            conflicts = conflicts,
            unchangedCount = unchanged
        )
    }

    private fun toRecord(
        local: Note,
        remote: RemoteNote,
        decision: ConflictDecision
    ): ConflictRecord = ConflictRecord(
        id = local.id,
        localTitle = local.title,
        localModified = decision.localTime,
        localVersion = local.version,
        localDeleted = local.isDeleted,
        remoteTitle = remote.title,
        remoteModified = decision.remoteTime,
        remoteVersion = remote.version,
        remoteDeleted = remote.isDeleted,
        winner = decision.winner,
        reason = decision.reason
    )
}
