package com.example.mycoursedesign

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.data.entity.Note
import com.example.core.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
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
 * 【依赖 NoteRepository 而非 NoteDao】
 * 分层为：ViewModel → NoteRepository(接口) ← NoteRepositoryImpl → NoteDao。
 * 好处是可测试（测试可传假 Repository，不需要真实 Room 数据库）、可替换（换数据源不改这里）。
 *
 * 【本次新增：归档视图切换】
 *
 * 归档功能有个 UX 陷阱：如果笔记归档后直接从主列表消失，用户就再也找不到它了
 * —— 归档变成了"单向黑洞"。所以必须提供一个"查看已归档"的入口。
 *
 * 实现上用 [flatMapLatest] 把"当前视图模式"和"数据库查询"串起来：
 *     showArchived(StateFlow<Boolean>) --flatMapLatest--> 对应的 Flow<List<Note>>
 * 当用户切换视图时，flatMapLatest 会自动取消上一个查询的订阅、订阅新的查询，
 * 不需要手动管理两套数据的生命周期。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    /**
     * 当前是否处于"查看已归档"模式。
     *
     * 放在 ViewModel 而不是 Activity 里，是为了让它在**屏幕旋转后仍然保留**
     * （Activity 会重建，但 ViewModel 不会）。这正是 MVVM 相对传统开发的优势之一。
     */
    private val _showArchived = MutableStateFlow(false)

    /** 对外只读的视图模式，供 Activity 更新标题与菜单文字。 */
    val isShowingArchived: StateFlow<Boolean> = _showArchived.asStateFlow()

    /**
     * 当前列表数据。
     *
     * `flatMapLatest` 的作用：每当 [_showArchived] 发射新值，就切换到对应的数据源，
     * 并**自动取消**上一轮还在进行的收集。这比手动 cancel + 重新 observe 简洁得多，
     * 也避免了"两个 Flow 同时更新导致列表闪烁"的问题。
     *
     * 【Java 思维对照】类似于 `LiveData.switchMap()`：
     * `Transformations.switchMap(showArchived, archived -> archived ? dao.getArchived() : dao.getActive())`
     */
    val notes: StateFlow<List<Note>> = _showArchived
        .flatMapLatest { archived ->
            if (archived) {
                noteRepository.observeArchivedNotes()
            } else {
                noteRepository.observeActiveNotes()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** 切换"笔记列表 / 已归档"视图。 */
    fun toggleShowArchived() {
        _showArchived.value = !_showArchived.value
    }

    /**
     * 新建笔记。
     *
     * 不再自己构造 [Note] 对象、也不手动填 `lastModified` / `version`，
     * 这些属于业务规则，已经下沉到 [NoteRepository.createNote] 里。
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
     * 设置置顶状态。
     *
     * 置顶效果不需要额外处理：`NoteDao.getActiveNotes()` 的 SQL 已经按
     * `isPinned DESC, lastModified DESC` 排序，只要 `isPinned` 一变，
     * Room 就会重新发射列表，置顶的笔记自然排到最前面。
     *
     * 版本号与时间戳的更新在 DAO 的 `@Transaction` 方法里完成（`setPinned`），
     * 保证"读-改-写"三步原子执行。
     */
    fun setPinned(note: Note, pinned: Boolean) {
        viewModelScope.launch {
            noteRepository.setPinned(note.id, pinned)
        }
    }

    /**
     * 设置归档状态。
     *
     * 归档后该笔记不再出现在主列表（`getActiveNotes()` 带 `isArchived = 0` 条件），
     * 但仍保留在数据库中，可通过 [toggleShowArchived] 切到"已归档"视图找回。
     */
    fun setArchived(note: Note, archived: Boolean) {
        viewModelScope.launch {
            noteRepository.setArchived(note.id, archived)
        }
    }

    /**
     * 删除笔记。
     *
     * 删除后**不需要手动通知列表刷新**：`notes` 这个 StateFlow 会收到 Room 的数据变更
     * 通知并自动发射新列表，`ListAdapter` + `DiffUtil` 随后自动计算差异并播放删除动画。
     */
    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteRepository.deleteNote(note)
        }
    }
}
