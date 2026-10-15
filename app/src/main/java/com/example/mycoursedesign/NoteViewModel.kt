package com.example.mycoursedesign

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.data.dao.NoteDao
import com.example.core.data.entity.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * NoteViewModel：负责管理 UI 数据状态，隔离 UI 和数据库。
 *
 * 【Java 思维对照】：
 * 1. `@HiltViewModel` + `@Inject constructor` 相当于 Spring 的 `@Service` 和 `@Autowired`，Hilt 会自动把 NoteDao 传进来，不需要你自己 new。
 * 2. `viewModelScope.launch` 相当于 Java 里的 `Executors.newSingleThreadExecutor().execute(() -> { ... })`，用来在后台线程执行数据库操作。
 * 3. `StateFlow` 相当于 Java 的 `LiveData`，它是可观察的数据流，数据库一变，UI 就能自动刷新。
 */
@HiltViewModel
class NoteViewModel @Inject constructor(
    private val noteDao: NoteDao
) : ViewModel() {

    // 暴露给 UI 的活跃笔记列表状态
    val activeNotes: StateFlow<List<Note>> = noteDao.getActiveNotes()
        .stateIn(
            scope = viewModelScope, // 绑定生命周期，ViewModel 销毁时自动取消，防止内存泄漏
            started = SharingStarted.WhileSubscribed(5000), // 5秒内没有订阅者就停止更新，节省资源
            initialValue = emptyList() // 初始值为空列表
        )

    // 新增笔记
    fun addNote(title: String, content: String) {
        viewModelScope.launch { // 开启协程
            val newNote = Note(title = title, content = content)
            noteDao.insertNote(newNote)
        }
    }

    // 更新笔记
    fun updateNote(note: Note) {
        viewModelScope.launch {
            noteDao.updateNote(note.copy(lastModified = System.currentTimeMillis(), version = note.version + 1))
        }
    }

    // 删除笔记
    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteDao.deleteNote(note)
        }
    }
}