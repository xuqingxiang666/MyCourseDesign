package com.example.mycoursedesign

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.core.data.entity.Note
import com.example.mycoursedesign.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 应用主界面：展示笔记列表，提供新建入口，并支持点击编辑、长按操作。
 *
 * 【Java 思维对照】
 * - `@AndroidEntryPoint`：告诉 Hilt 这个 Activity 需要参与依赖注入，
 *   相当于给类加上 Spring 的 `@Component` 扫描标记。
 * - `by viewModels()`：Kotlin 属性委托，等价于 Java 里的
 *   `new ViewModelProvider(this).get(NoteViewModel.class)`，
 *   但它是**懒加载**的，第一次访问时才真正创建 ViewModel。
 * - `by viewModels()` 还能保证屏幕旋转后拿到的是**同一个** ViewModel 实例，
 *   数据不会丢失 —— 这正是 MVVM 相对传统开发的核心优势。
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    /** Hilt 自动注入的 ViewModel，负责持有并暴露 UI 状态。 */
    private val viewModel: NoteViewModel by viewModels()

    /**
     * ViewBinding 绑定对象。
     * `ActivityMainBinding` 是编译期由 AGP 根据 `activity_main.xml` 自动生成的类，
     * 类名 = 布局文件名转大驼峰 + "Binding"，其中每个带 id 的控件都会变成一个属性。
     *
     * 【Java 思维对照】相当于 `findViewById` 的类型安全 + 空安全版本，
     * 写错控件名会直接编译失败，而不是等到运行时抛 NullPointerException。
     */
    private lateinit var binding: ActivityMainBinding

    private lateinit var adapter: NoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupFab()
        observeNotes()
    }

    /**
     * 初始化列表。
     *
     * 点击 → 打开编辑器编辑这条笔记；
     * 长按 → 弹出操作菜单（目前只有删除，后续会加置顶/归档）。
     */
    private fun setupRecyclerView() {
        adapter = NoteAdapter(
            onItemClick = { note -> openEditor(note.id) },
            onItemLongClick = { note -> showNoteOptions(note) }
        )
        binding.recyclerView.adapter = adapter
    }

    /** 悬浮按钮：新建一条笔记。 */
    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            openEditor(NoteEditorActivity.NEW_NOTE_ID)
        }
    }

    /**
     * 打开发编辑器界面。
     *
     * @param noteId 要编辑的笔记 id；传 [NoteEditorActivity.NEW_NOTE_ID] 表示新建。
     */
    private fun openEditor(noteId: Long) {
        startActivity(NoteEditorActivity.newIntent(this, noteId))
    }

    /**
     * 长按笔记弹出的操作菜单。
     *
     * 【为什么用数组 + when(which) 而不是写死一个"删除"按钮】
     * 后续要给菜单加"置顶""归档"，只需要往数组里加一项、往 when 里加个分支，
     * 扩展成本几乎为零。这是为了避免下次改动时重写整个方法。
     */
    private fun showNoteOptions(note: Note) {
        val options = arrayOf(getString(R.string.action_delete))

        MaterialAlertDialogBuilder(this)
            .setTitle(note.title.ifBlank { getString(R.string.note_untitled) })
            .setItems(options) { _, which ->
                when (which) {
                    0 -> confirmDelete(note)
                }
            }
            .show()
    }

    /**
     * 删除确认对话框。
     *
     * 删除是"不可恢复"操作，必须二次确认，否则手滑长按就把笔记弄丢了。
     * 确认后调用 ViewModel 删除，列表通过 [observeNotes] 的 Flow 自动刷新，
     * 无需手动通知适配器。
     */
    private fun confirmDelete(note: Note) {
        val displayTitle = note.title.ifBlank { getString(R.string.note_untitled) }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_dialog_title)
            .setMessage(getString(R.string.delete_dialog_message, displayTitle))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteNote(note)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    /**
     * 订阅 ViewModel 暴露的笔记列表状态。
     *
     * ★ 这里是本文件最关键的一处 Kotlin/协程写法，也是常见报错点。
     *
     * 【Java 思维对照】
     * 如果用的是 LiveData，你写的是：
     *     viewModel.getActiveNotes().observe(this, notes -> adapter.setNotes(notes));
     * 一个 `observe()` 就够了，因为 LiveData 自带生命周期感知。
     *
     * 但这里用的是 **StateFlow**（Kotlin 协程的数据流），它是"冷流 + 需手动收集"的设计，
     * **没有 `observe()` 方法**。直接写 `viewModel.activeNotes.observe(this) { ... }`
     * 会编译报错：`Unresolved reference: observe`。
     *
     * 正确做法是 `repeatOnLifecycle`：
     * 1. 当界面处于 STARTED（可见）状态时才开始收集数据流；
     * 2. 当界面进入 STOPPED（如按 Home 键、跳转到别的界面）时自动取消收集，
     *    避免后台无谓地刷新 UI —— 这正是 LiveData 曾经帮你做的事；
     * 3. 界面重新可见时自动重新收集。
     */
    private fun observeNotes() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.activeNotes.collect { notes ->
                    // collect 的 lambda 参数类型可以从 Flow 的泛型推断出来，
                    // 写 `collect { notes -> ... }` 即可，无需显式标注类型。
                    adapter.submitList(notes)
                }
            }
        }
    }
}
