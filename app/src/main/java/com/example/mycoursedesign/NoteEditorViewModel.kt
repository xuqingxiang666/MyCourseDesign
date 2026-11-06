package com.example.mycoursedesign

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.data.entity.Note
import com.example.core.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 笔记编辑器界面的 ViewModel。
 *
 * 【职责】
 * - 编辑模式：加载这条笔记
 * - 两种保存路径：
 *   1. **草稿自动保存** [autoSave]：输入停顿后由界面调用，静默落库
 *   2. **显式保存** [save]：点工具栏「保存」时调用，落库后返回列表
 *
 * 【为什么"自动保存"要放在 ViewModel 而不是 Activity】
 * 真正的落库逻辑（新建 vs 更新、版本号递增、内容未变则跳过）是**业务规则**，
 * 应该和 UI 解耦；Activity 只负责"什么时候触发"（防抖计时）。
 * 这样以后换 Fragment 或 Compose 界面时，保存逻辑不用重写。
 *
 * 【Java 思维对照】
 * `@HiltViewModel` + `@Inject constructor` 相当于 Spring 的 `@Service` + `@Autowired`；
 * `SavedStateHandle` 类似从请求里取 path 参数。
 */
@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NoteRepository
) : ViewModel() {

    /**
     * 当前编辑的笔记 id。
     *
     * ★ 必须是 `var`：新建模式下第一次自动保存会真正插入数据库并拿到新 id，
     * 之后就要按"编辑已有笔记"的方式更新，否则每次自动保存都会新建一条。
     */
    private var noteId: Long =
        savedStateHandle.get<Long>(NoteEditorActivity.EXTRA_NOTE_ID)
            ?: NoteEditorActivity.NEW_NOTE_ID

    /** 是否还没落过库（新建且尚未保存）。 */
    val isNewNote: Boolean get() = noteId == NoteEditorActivity.NEW_NOTE_ID

    private val _note = MutableStateFlow<Note?>(null)

    /** 已加载的笔记；新建模式下为 null。 */
    val note: StateFlow<Note?> = _note.asStateFlow()

    private val _lastAutoSavedAt = MutableStateFlow<Long?>(null)

    /**
     * 最近一次自动保存的时间（毫秒）；null 表示还没保存过。
     * 界面订阅它来显示"草稿已自动保存 10:38"这类提示。
     */
    val lastAutoSavedAt: StateFlow<Long?> = _lastAutoSavedAt.asStateFlow()

    init {
        if (!isNewNote) {
            viewModelScope.launch {
                _note.value = noteRepository.getNoteById(noteId)
            }
        }
    }

    /**
     * 草稿自动保存。
     *
     * 不返回结果、不弹提示（除非成功后会更新 [lastAutoSavedAt] 让界面显示时间），
     * 因为它在用户输入过程中被反复调用，不能打扰用户。
     */
    fun autoSave(title: String, content: String) {
        viewModelScope.launch {
            if (persist(title, content)) {
                _lastAutoSavedAt.value = System.currentTimeMillis()
            }
        }
    }

    /**
     * 显式保存（点工具栏「保存」）。
     *
     * @param onResult 主线程回调；true 表示已落库。false 只在"标题和内容都为空"时出现。
     */
    fun save(title: String, content: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val saved = persist(title, content)
            if (saved) _lastAutoSavedAt.value = System.currentTimeMillis()
            onResult(saved)
        }
    }

    /**
     * 真正的落库逻辑，两条保存路径共用。
     *
     * 规则：
     * 1. 标题和正文都为空 → 不保存（避免库里堆空记录）
     * 2. 还没落过库 → 新建，并把新 id 记下来
     * 3. 已存在 → 更新；**内容与数据库一致时直接返回成功、不写库**
     *    —— 否则用户每停一次输入就会把 version 无意义地刷高一次，
     *    会让后面的"冲突解决机制"误判数据新旧
     */
    private suspend fun persist(title: String, content: String): Boolean {
        val trimmedTitle = title.trim()
        val trimmedContent = content.trim()

        if (trimmedTitle.isEmpty() && trimmedContent.isEmpty()) return false

        // ---------- 新建 ----------
        if (noteId == NoteEditorActivity.NEW_NOTE_ID) {
            val newId = noteRepository.createNote(trimmedTitle, trimmedContent)
            noteId = newId
            _note.value = noteRepository.getNoteById(newId)
            return true
        }

        // ---------- 更新 ----------
        val existing = _note.value ?: noteRepository.getNoteById(noteId) ?: return false

        // 内容没变就不写库
        if (existing.title == trimmedTitle && existing.content == trimmedContent) return true

        val updated = existing.copy(
            title = trimmedTitle,
            content = trimmedContent,
            // ★ 这两个字段是"LWW 冲突解决机制"的判据，任何写路径都必须更新
            lastModified = System.currentTimeMillis(),
            version = existing.version + 1
        )
        noteRepository.updateNote(updated)
        _note.value = updated
        return true
    }
}
