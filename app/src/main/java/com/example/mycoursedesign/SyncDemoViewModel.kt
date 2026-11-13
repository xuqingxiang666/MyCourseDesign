package com.example.mycoursedesign

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.data.repository.SyncRepository
import com.example.core.domain.Winner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * 模拟同步演示页的 ViewModel。
 *
 * 【为什么要专门做一个演示页】
 * 冲突解决是课设必做项，但它发生在"看不见的数据层"。
 * 答辩时必须能当场演示：怎么产生冲突、系统怎么判、结果怎么落到两端。
 * 这个页面就是那条演示路径 —— 按钮一步步按下去即可。
 *
 * 【演示数据从哪来】
 * 用 [SyncRepository] 的演示辅助方法：
 * - 初始化：把本地笔记推到"云端"表，制造"两端一致"的起点
 * - 模拟另一台设备修改/删除：直接改云端表的行，并给一个"未来 5 分钟"的时间戳，
 *   保证云端版本必然更新，从而稳定复现冲突
 */
@HiltViewModel
class SyncDemoViewModel @Inject constructor(
    private val syncRepository: SyncRepository
) : ViewModel() {

    /**
     * 界面状态。
     *
     * @param localCount 本地未删除条数
     * @param localDeletedCount 本地已软删除条数
     * @param remoteCount 云端条数
     * @param message 操作结果/提示文字（多行）
     */
    data class UiState(
        val localCount: Int = 0,
        val localDeletedCount: Int = 0,
        val remoteCount: Int = 0,
        val message: String = "① 先点「初始化云端」，把本地笔记同步到云端\n" +
            "② 再点「模拟另一台设备修改」制造冲突\n" +
            "③ 最后点「立即同步」看系统怎么判"
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    init {
        refresh()
    }

    /** 刷新两端条数。 */
    fun refresh() {
        viewModelScope.launch {
            val (alive, deleted, remote) = syncRepository.counts()
            _state.value = _state.value.copy(
                localCount = alive,
                localDeletedCount = deleted,
                remoteCount = remote
            )
        }
    }

    /** ① 初始化：把本地笔记推到云端。 */
    fun seed() {
        viewModelScope.launch {
            val count = syncRepository.pushAllLocalToRemote()
            _state.value = _state.value.copy(
                message = "✅ 已把本地 $count 条笔记推送到云端\n现在两端一致，可以制造冲突了"
            )
            refresh()
        }
    }

    /**
     * ② 模拟"另一台设备修改"。
     *
     * 挑第一条未删除的本地笔记，把云端那份改掉并把时间戳设成未来 5 分钟，
     * 这样同步时云端必然胜出 —— 效应确定、可复现。
     */
    fun simulateRemoteEdit() {
        viewModelScope.launch {
            val target = syncRepository.localNotes().firstOrNull { !it.isDeleted }
            if (target == null) {
                _state.value = _state.value.copy(message = "⚠️ 本地没有笔记，先去列表里新建一条")
                return@launch
            }
            val id = syncRepository.simulateRemoteEdit(target.id)
            if (id == null) {
                _state.value = _state.value.copy(
                    message = "⚠️ 云端还没有这条笔记\n请先点「① 初始化云端」"
                )
            } else {
                _state.value = _state.value.copy(
                    message = "✅ 已模拟另一台设备修改笔记 #$id\n" +
                        "├ 本地：v${target.version}  ${timeFormat.format(Date(target.lastModified))}\n" +
                        "└ 云端：v${target.version + 1}  未来 5 分钟（更新）\n\n" +
                        "现在点「④ 立即同步」，看系统判给谁"
                )
            }
            refresh()
        }
    }

    /** ③ 模拟"另一台设备删除"。 */
    fun simulateRemoteDelete() {
        viewModelScope.launch {
            val target = syncRepository.localNotes().firstOrNull { !it.isDeleted }
            if (target == null) {
                _state.value = _state.value.copy(message = "⚠️ 本地没有可删除的笔记")
                return@launch
            }
            val id = syncRepository.simulateRemoteDelete(target.id)
            if (id == null) {
                _state.value = _state.value.copy(
                    message = "⚠️ 云端还没有这条笔记，请先初始化云端"
                )
            } else {
                _state.value = _state.value.copy(
                    message = "✅ 已模拟另一台设备删除笔记 #$id\n" +
                        "云端把这条标记为「已删除」，删除时间是未来 5 分钟\n\n" +
                        "现在点「④ 立即同步」，删除会传播到本地"
                )
            }
            refresh()
        }
    }

    /** ④ 执行同步。 */
    fun sync() {
        viewModelScope.launch {
            val result = syncRepository.sync()
            val builder = StringBuilder()

            builder.appendLine("同步完成：${result.summary}")
            builder.appendLine()

            if (result.plan.conflicts.isEmpty()) {
                builder.appendLine("本次没有冲突（两端内容一致，或只有一端有数据）")
            } else {
                builder.appendLine("冲突明细：")
                result.plan.conflicts.forEach { record ->
                    val side = if (record.winner == Winner.LOCAL) "本地" else "云端"
                    builder.appendLine("─ 笔记 #${record.id}")
                    builder.appendLine(
                        "   本地：v${record.localVersion}  " +
                            "${timeFormat.format(Date(record.localModified))}" +
                            if (record.localDeleted) "（已删除）" else ""
                    )
                    builder.appendLine(
                        "   云端：v${record.remoteVersion}  " +
                            "${timeFormat.format(Date(record.remoteModified))}" +
                            if (record.remoteDeleted) "（已删除）" else ""
                    )
                    builder.appendLine("   → 采用【$side】版本")
                    builder.appendLine("   依据：${record.reason}")
                }
            }

            _state.value = _state.value.copy(message = builder.toString().trimEnd())
            refresh()
        }
    }

    /** 重置云端（便于重复演示）。 */
    fun resetRemote() {
        viewModelScope.launch {
            syncRepository.clearRemote()
            _state.value = _state.value.copy(
                message = "🧹 已清空云端\n可以重新从「① 初始化云端」开始演示"
            )
            refresh()
        }
    }
}
