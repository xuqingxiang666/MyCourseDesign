package com.example.core.domain

import com.example.core.data.entity.Note
import com.example.core.data.entity.RemoteNote
import java.security.MessageDigest

/**
 * 冲突判定的结果。
 */
enum class Winner {
    /** 本地版本胜出（需要上传覆盖云端）。 */
    LOCAL,

    /** 云端版本胜出（需要下载覆盖本地）。 */
    REMOTE,

    /** 两边完全一致，无需任何操作。 */
    IDENTICAL
}

/**
 * 单条笔记的判定详情，既用于执行同步，也用于界面展示"为什么这样判"。
 */
data class ConflictDecision(
    val winner: Winner,
    /** 判定依据的说明文字，直接显示给用户看。 */
    val reason: String,
    /** 本地参与比较的时间（已删除的笔记用删除时间）。 */
    val localTime: Long,
    /** 云端参与比较的时间。 */
    val remoteTime: Long
)

/**
 * LWW（Last-Write-Wins，后写者获胜）冲突解决策略。
 *
 * 【为什么选 LWW 而不是让用户手动合并】
 * 1. 实现简单、结果确定，**不会出现"每次同步结果都不一样"**；
 * 2. 笔记应用的真实场景里，同一秒内两端改同一条笔记的概率极低，
 *    LWW 的准确性足够；
 * 3. 用户不需要理解"合并"这种复杂交互，符合手机端使用习惯。
 *
 * 【为什么判定是三级而不是只比时间】
 * 只比 `lastModified` 有一个漏洞：**同一毫秒内的修改无法区分**。
 * 而同步可能在一瞬间处理几百条数据，时间戳撞车并不罕见。
 * 所以加了两级兜底：
 *
 * | 级别 | 比较 | 作用 |
 * |---|---|---|
 * | 1 | 时间戳 | 主要判据，符合"后写的赢" |
 * | 2 | 版本号 | 时间戳相同时，修改次数多的更新 |
 * | 3 | 内容哈希 | 版本也相同时，保证结果**确定且可复现**（不依赖随机或遍历顺序） |
 *
 * 【"删除"怎么参与比较】
 * 软删除也带时间（`deletedAt`）。判定时把"已删除的笔记"的时间取
 * `max(deletedAt, lastModified)`，与普通修改放在同一维度比较：
 * - 删除晚于他端修改 → 删除生效（同步出去）
 * - 他端修改晚于删除 → 笔记复活（以修改版本为准）
 *
 * 这样"删除"就变成了普通的"一次修改"，不需要为它单独写一套规则 —— 更不容易出 bug。
 *
 * 【纯 Kotlin，无 Android 依赖】
 * 所以可以用普通单元测试覆盖所有分支，跑一次不到 1 秒。
 */
object ConflictResolver {

    /**
     * 判定一条冲突。
     *
     * @param local 本地版本
     * @param remote 云端版本
     */
    fun resolve(local: Note, remote: RemoteNote): ConflictDecision {
        val localTime = effectiveTime(local.lastModified, local.isDeleted, local.deletedAt)
        val remoteTime = effectiveTime(remote.lastModified, remote.isDeleted, remote.deletedAt)

        // ---------- 0. 完全一致：不需要任何操作 ----------
        if (isSameContent(local, remote) && local.isDeleted == remote.isDeleted) {
            return ConflictDecision(
                winner = Winner.IDENTICAL,
                reason = "两边内容一致，无需处理",
                localTime = localTime,
                remoteTime = remoteTime
            )
        }

        // ---------- 1. 时间戳 ----------
        if (localTime != remoteTime) {
            return if (localTime > remoteTime) {
                ConflictDecision(
                    Winner.LOCAL,
                    "本地版本更新（本地 $localTime > 云端 $remoteTime）",
                    localTime, remoteTime
                )
            } else {
                ConflictDecision(
                    Winner.REMOTE,
                    "云端版本更新（云端 $remoteTime > 本地 $localTime）",
                    localTime, remoteTime
                )
            }
        }

        // ---------- 2. 版本号 ----------
        if (local.version != remote.version) {
            return if (local.version > remote.version) {
                ConflictDecision(
                    Winner.LOCAL,
                    "时间相同且本地版本更高（本地 v${local.version} > 云端 v${remote.version}）",
                    localTime, remoteTime
                )
            } else {
                ConflictDecision(
                    Winner.REMOTE,
                    "时间相同且云端版本更高（云端 v${remote.version} > 本地 v${local.version}）",
                    localTime, remoteTime
                )
            }
        }

        // ---------- 3. 内容哈希（保证确定性） ----------
        val localHash = contentHash(local)
        val remoteHash = contentHash(remote)
        return if (localHash > remoteHash) {
            ConflictDecision(
                Winner.LOCAL,
                "时间和版本都相同，按内容哈希裁决（本地胜出）",
                localTime, remoteTime
            )
        } else {
            ConflictDecision(
                Winner.REMOTE,
                "时间和版本都相同，按内容哈希裁决（云端胜出）",
                localTime, remoteTime
            )
        }
    }

    /**
     * 计算参与比较的"有效时间"。
     *
     * 已删除的笔记用删除时间（保留与 lastModified 的较大值，防止只写了一个字段）；
     * 未删除的笔记用最后修改时间。
     */
    fun effectiveTime(lastModified: Long, isDeleted: Boolean, deletedAt: Long): Long =
        if (isDeleted) maxOf(deletedAt, lastModified) else lastModified

    /** 内容（含状态字段）是否完全一致。 */
    private fun isSameContent(local: Note, remote: RemoteNote): Boolean =
        local.title == remote.title &&
            local.content == remote.content &&
            local.isArchived == remote.isArchived &&
            local.isPinned == remote.isPinned &&
            local.isEncrypted == remote.isEncrypted

    /**
     * 内容哈希（SHA-256 十六进制）。
     *
     * 用作最后一级裁决：即使时间戳和版本号都相同，也能得到一个**稳定的**结果，
     * 不会因为列表遍历顺序不同而出现"这次本地赢、下次云端赢"。
     */
    private fun contentHash(local: Note): String = sha256(
        "${local.title}\u0001${local.content}\u0001${local.isArchived}\u0001" +
            "${local.isPinned}\u0001${local.isEncrypted}\u0001${local.isDeleted}"
    )

    private fun contentHash(remote: RemoteNote): String = sha256(
        "${remote.title}\u0001${remote.content}\u0001${remote.isArchived}\u0001" +
            "${remote.isPinned}\u0001${remote.isEncrypted}\u0001${remote.isDeleted}"
    )

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
