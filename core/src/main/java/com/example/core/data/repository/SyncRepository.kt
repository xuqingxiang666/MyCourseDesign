package com.example.core.data.repository

import com.example.core.data.entity.Note
import com.example.core.data.entity.RemoteNote
import com.example.core.data.remote.RemoteDataSource
import com.example.core.domain.SyncEngine
import com.example.core.domain.SyncPlan
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 一次同步的执行结果。
 *
 * 除了 [plan]（"算出来的计划"），还带上实际执行成功的条数，
 * 界面可以据此显示"上传 2 条、下载 1 条"。
 */
data class SyncResult(
    val plan: SyncPlan,
    val uploaded: Int,
    val downloaded: Int
) {
    val summary: String
        get() = "上传 $uploaded 条、下载 $downloaded 条" +
            if (plan.conflicts.isNotEmpty()) "、冲突 ${plan.conflicts.size} 条" else ""
}

/**
 * 同步仓储：把"算计划"和"改数据"串起来。
 *
 * 【职责划分】
 * - [SyncEngine]：纯函数，只算"该做什么"（不碰数据库）
 * - 本类：真正执行 —— 该上传的写到云端，该下载的写回本地
 *
 * 这样拆分的好处是：冲突判定能脱离数据库做单元测试，
 * 而"怎么落库"这种容易出错的事集中在少数几个方法里。
 *
 * 【演示辅助方法】
 * 为了让答辩时能一键复现冲突，提供了 [simulateRemoteEdit] /
 * [simulateRemoteDelete] / [pushAllLocalToRemote]，它们模拟"另一台设备"的行为。
 * 生产环境当然不会保留这些方法，但课程演示里非常有用。
 */
@Singleton
class SyncRepository @Inject constructor(
    private val noteRepository: NoteRepository,
    private val remoteDataSource: RemoteDataSource
) {

    /** 只计算同步计划，不改任何数据（用于界面预览）。 */
    suspend fun plan(): SyncPlan {
        val local = noteRepository.getAllNotesIncludingDeleted()
        val remote = remoteDataSource.fetchAll()
        return SyncEngine.plan(local, remote)
    }

    /**
     * 执行同步。
     *
     * 顺序说明：**先上传、后下载**。
     * 如果顺序反过来，先下载的远端数据可能被随后上传的本地旧数据覆盖掉
     * （虽然本次计划已经算好了各自的胜负，但先上传能让"本地胜出"的记录
     * 更早进入云端，避免中途失败造成更大的不一致）。
     */
    suspend fun sync(): SyncResult {
        val local = noteRepository.getAllNotesIncludingDeleted()
        val remote = remoteDataSource.fetchAll()
        val plan = SyncEngine.plan(local, remote)

        // 上传：本地版本胜出 / 云端没有的
        plan.uploads.forEach { note ->
            remoteDataSource.upsert(RemoteNote.from(note))
        }

        // 下载：云端版本胜出 / 本地没有的
        plan.downloads.forEach { remoteNote ->
            noteRepository.applyRemoteNote(remoteNote)
        }

        return SyncResult(plan, plan.uploads.size, plan.downloads.size)
    }

    /** 本地（含已删除）与云端的条数，演示界面显示用。 */
    suspend fun counts(): Triple<Int, Int, Int> {
        val localAll = noteRepository.getAllNotesIncludingDeleted()
        return Triple(
            localAll.count { !it.isDeleted },
            localAll.count { it.isDeleted },
            remoteDataSource.count()
        )
    }

    // ==================== 演示辅助 ====================

    /**
     * 把本地全部笔记推送到云端（不含已删除的）。
     *
     * 演示第一步：让两端数据一致，制造"同步前无冲突"的起点。
     */
    suspend fun pushAllLocalToRemote(): Int {
        val local = noteRepository.getAllNotesIncludingDeleted().filter { !it.isDeleted }
        local.forEach { remoteDataSource.upsert(RemoteNote.from(it)) }
        return local.size
    }

    /**
     * 模拟"另一台设备修改了某条笔记"。
     *
     * 关键点：把 `lastModified` 设成**未来 5 分钟**，
     * 这样云端版本一定比本地新，同步时云端胜出 —— 演示确定性冲突。
     *
     * @return 被修改的笔记 id；云端没有这条时返回 null
     */
    suspend fun simulateRemoteEdit(noteId: Long, suffix: String = "（另一台设备改的）"): Long? {
        val remote = remoteDataSource.fetchAll().firstOrNull { it.id == noteId } ?: return null
        val future = System.currentTimeMillis() + 5 * 60 * 1000L
        remoteDataSource.upsert(
            remote.copy(
                title = if (remote.title.isBlank()) remote.title else "${remote.title}${suffix}",
                content = remote.content + suffix,
                lastModified = future,
                version = remote.version + 1,
                isDeleted = false,
                deletedAt = 0L
            )
        )
        return remote.id
    }

    /**
     * 模拟"另一台设备删除了某条笔记"。
     *
     * 同样把删除时间设成未来 5 分钟，保证"删除"在 LWW 里胜出。
     */
    suspend fun simulateRemoteDelete(noteId: Long): Long? {
        val remote = remoteDataSource.fetchAll().firstOrNull { it.id == noteId } ?: return null
        val future = System.currentTimeMillis() + 5 * 60 * 1000L
        remoteDataSource.upsert(
            remote.copy(
                isDeleted = true,
                deletedAt = future,
                lastModified = future,
                version = remote.version + 1
            )
        )
        return remote.id
    }

    /** 清空云端（演示重置）。 */
    suspend fun clearRemote() = remoteDataSource.clear()

    /** 云端全部笔记，界面展示"云端数据现状"用。 */
    suspend fun remoteNotes(): List<RemoteNote> = remoteDataSource.fetchAll()

    /** 本地全部笔记（含已删除），界面展示用。 */
    suspend fun localNotes(): List<Note> = noteRepository.getAllNotesIncludingDeleted()
}
