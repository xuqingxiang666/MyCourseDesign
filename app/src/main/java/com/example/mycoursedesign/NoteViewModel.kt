package com.example.mycoursedesign

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.data.entity.Note
import com.example.core.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 列表页的 ViewModel：负责管理笔记列表的 UI 状态。
 *
 * 【Java 思维对照】
 * 1. `@HiltViewModel` + `@Inject constructor` 相当于 Spring 的 `@Service` + `@Autowired`，
 *    Hilt 会自动把 [NoteRepository] 传进来，不需要自己 new。
 * 2. `viewModelScope.launch` 相当于 Java 里的
 *    `Executors.newSingleThreadExecutor().execute(() -> { ... })`，用来在后台执行数据库操作。
 * 3. `StateFlow` 相当于 Java 的 `LiveData`，是可观察的数据流，数据库一变 UI 就自动刷新。
 *
 * 【本次重构：从依赖 NoteDao 改为依赖 NoteRepository】
 *
 * 重构前：
 *     class NoteViewModel @Inject constructor(private val noteDao: NoteDao)
 * 重构后：
 *     class NoteViewModel @Inject constructor(private val noteRepository: NoteRepository)
 *
 * **这是 Clean Architecture 分层真正落地的关键一步。** 为什么重要：
 *
 * 1. **可测试性**：现在单元测试可以传入一个假的 `NoteRepository` 实现，
 *    完全不需要构造真实的 Room 数据库。这是把测试覆盖率做到 60% 的前提。
 *    （如果直接依赖 `NoteDao`，就只能用 Robolectric 或仪器测试，既慢又依赖环境。）
 * 2. **可替换性**：将来若要把数据源从"本地 Room"换成"Room + 云端同步"，
 *    只需要替换 Repository 的实现，这个文件一行都不用改。
 * 3. **职责清晰**：`NoteDao` 是"SQL 语句的接口"，`NoteRepository` 是"业务操作的接口"。
 *    ViewModel 关心的是"新建一条笔记"，而不是"往 notes 表 insert 一行"。
 *
 * 【注意：对外的 API 完全没有变化】
 * `activeNotes` 的类型仍是 `StateFlow<List<Note>>`，`addNote` / `updateNote` / `deleteNote`
 * 的签名也没变，所以 `MainActivity` 一行都不需要改 —— 这正是一次成功重构的标志。
 */
@HiltViewModel
class NoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    /**
     * 暴露给 UI 的活跃笔记列表状态。
     *
     * `stateIn` 的三个参数值得记住：
     * - `viewModelScope`：绑定 ViewModel 生命周期，ViewModel 销毁时自动取消收集，防止内存泄漏。
     * - `SharingStarted.WhileSubscribed(5000)`：5 秒内没有订阅者就停止上游的数据流，
     *   节省资源（例如屏幕旋转的瞬间会短暂没有订阅者，5 秒的宽限期避免频繁重启）。
     * - `initialValue = emptyList()`：StateFlow 必须有初始值，否则 UI 首次渲染拿不到数据。
     */
    val activeNotes: StateFlow<List<Note>> = noteRepository.observeActiveNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * 新建笔记。
     *
     * 注意这里不再自己构造 [Note] 对象、也不再手动填 `lastModified` / `version`，
     * 这些属于业务规则，已经下沉到 [NoteRepository.createNote] 里。
     * ViewModel 只需要表达"我要新建一条标题为 X、内容为 Y 的笔记"。
     */
    fun addNote(title: String, content: String) {
        viewModelScope.launch {
            noteRepository.createNote(title, content)
        }
    }

    /**
     * 更新笔记。
     *
     * 每次修改都刷新 `lastModified` 并递增 `version`——这两个字段是后续
     * "LWW 冲突解决机制"的判据，所以任何写路径都不能漏掉它们。
     */
    fun updateNote(note: Note) {
        viewModelScope.launch {
            noteRepository.updateNote(
                note.copy(
                    lastModified = System.currentTimeMillis(),
                    version = note.version + 1
                )
            )
        }
    }

    /**
     * 删除笔记。
     *
     * 删除后**不需要手动通知列表刷新**：`observeActiveNotes()` 返回的 Flow
     * 会收到 Room 的数据变更通知并自动发射新列表，`ListAdapter` + `DiffUtil`
     * 随后自动计算差异并播放删除动画。
     */
    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteRepository.deleteNote(note)
        }
    }
}
