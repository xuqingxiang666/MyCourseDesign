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
 * 【它和 [NoteViewModel] 的分工】
 * - [NoteViewModel] 负责**列表**：观察所有笔记、删除笔记。
 * - [NoteEditorViewModel] 负责**单条**：加载一条笔记用于编辑、保存标题与正文。
 *
 * 拆成两个 ViewModel 而不是塞进一个，是因为职责完全不同：
 * 列表页关心"有哪些笔记变了"，编辑页关心"这一条笔记的内容"。
 * 而且编辑页关掉后它的 ViewModel 就该销毁，不该继续观察整个列表。
 *
 * 【为什么用 NoteRepository 而不是直接注入 NoteDao】
 * 这是上一步引入的抽象层的第一个实际受益者。ViewModel 只依赖接口，
 * 将来加加密、冲突解决等逻辑时，只改 Repository 实现即可，这个文件不用动。
 *
 * 【@HiltViewModel + SavedStateHandle 的工作方式】
 * `@HiltViewModel` 让 Hilt 能构造这个 ViewModel；
 * [SavedStateHandle] 是 AndroidX 提供的"可跨进程重建保存的键值容器"，
 * 它**自动包含启动本 Activity 时 Intent 里的 extras**。
 * 所以 `NoteEditorActivity.newIntent(context, noteId)` 放进 Intent 的 noteId，
 * 在这里可以直接读出来，不需要 Activity 手动往 ViewModel 里塞参数。
 *
 * 【Java 思维对照】
 * 相当于 Spring 的 `@Service`，通过构造器注入依赖；
 * SavedStateHandle 类似 Web 框架里从 request 中取 path 参数的 `@PathVariable`。
 */
@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NoteRepository
) : ViewModel() {

    /**
     * 从 Intent extras 中取出要编辑的笔记 id。
     * 若为 [NoteEditorActivity.NEW_NOTE_ID]（-1），表示这是"新建笔记"模式。
     */
    private val noteId: Long =
        savedStateHandle.get<Long>(NoteEditorActivity.EXTRA_NOTE_ID)
            ?: NoteEditorActivity.NEW_NOTE_ID

    /** 是否是新建模式。Activity 用它决定工具栏标题显示"新建笔记"还是"编辑笔记"。 */
    val isNewNote: Boolean get() = noteId == NoteEditorActivity.NEW_NOTE_ID

    /**
     * 对外暴露的笔记内容状态。
     *
     * 新建模式下永远是 null；编辑模式下初始为 null，从数据库读出来后变成具体笔记。
     * Activity 观察到非 null 时，把标题和正文填进输入框。
     *
     * 用 [MutableStateFlow] 而不是普通变量，是为了配合 `repeatOnLifecycle` 的
     * 生命周期感知收集，和列表页的写法保持一致。
     */
    private val _note = MutableStateFlow<Note?>(null)
    val note: StateFlow<Note?> = _note.asStateFlow()

    init {
        // 只有编辑模式才需要从数据库读数据。
        if (!isNewNote) {
            viewModelScope.launch {
                _note.value = noteRepository.getNoteById(noteId)
            }
        }
    }

    /**
     * 保存笔记。
     *
     * @param title 用户输入的标题（未 trim）
     * @param content 用户输入的正文（未 trim）
     * @param onResult 保存结果回调，在**主线程**执行。
     *        true = 已保存，false = 标题和正文都为空，未保存。
     *
     * 【为什么用回调而不是返回 Boolean】
     * Kotlin 里虽然可以 `suspend fun save(): Boolean`，但 ViewModel 的
     * `viewModelScope` 已经绑定了生命周期，直接在 ViewModel 内部启动协程更安全
     * （Activity 销毁时自动取消）。用回调把结果通知回 UI，是 MVVM 中常见做法。
     *
     * 【Java 思维对照】
     * 相当于 Java 里传一个 `Callback<Boolean>` 接口或 `Consumer<Boolean>`。
     */
    fun save(title: String, content: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val trimmedTitle = title.trim()
            val trimmedContent = content.trim()

            // 标题和正文都为空时不保存，避免数据库里堆积空记录。
            if (trimmedTitle.isEmpty() && trimmedContent.isEmpty()) {
                onResult(false)
                return@launch
            }

            if (isNewNote) {
                // 新建：由 Repository 负责填充 lastModified / version 等字段
                noteRepository.createNote(trimmedTitle, trimmedContent)
            } else {
                // 编辑：先取到原记录，再整体覆盖
                val existing = _note.value ?: noteRepository.getNoteById(noteId)
                if (existing == null) {
                    // 笔记在编辑过程中被别处删除了，无法保存
                    onResult(false)
                    return@launch
                }
                noteRepository.updateNote(
                    existing.copy(
                        title = trimmedTitle,
                        content = trimmedContent,
                        // ★ 每次修改都刷新时间戳并递增版本号，
                        //   这两个字段是后续"LWW 冲突解决机制"的判据
                        lastModified = System.currentTimeMillis(),
                        version = existing.version + 1
                    )
                )
            }
            onResult(true)
        }
    }
}
