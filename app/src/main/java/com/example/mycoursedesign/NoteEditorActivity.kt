package com.example.mycoursedesign

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.mycoursedesign.databinding.ActivityNoteEditorBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 笔记编辑界面：整页编辑新建或修改一条笔记。
 *
 * 【界面结构】
 * 顶部工具栏（关闭 + 保存），下方标题输入框，再下方铺满整屏的正文输入区。
 * 详见 `res/layout/activity_note_editor.xml` 里的逐条注释。
 *
 * 【两种模式】
 * - 新建：`NoteEditorActivity.newIntent(context, NEW_NOTE_ID)`
 * - 编辑：`NoteEditorActivity.newIntent(context, noteId)`
 * 通过 Intent extra 传入 noteId，由 [NoteEditorViewModel] 从 SavedStateHandle 读取。
 *
 * 【@AndroidEntryPoint 的作用】
 * 让 Hilt 能为本 Activity 注入 ViewModel。**忘记加这个注解**时，
 * `by viewModels()` 会在运行时抛 `IllegalStateException: Given component holder
 * ... does not implement interface GeneratedComponentManager` —— 这是常见错误。
 */
@AndroidEntryPoint
class NoteEditorActivity : AppCompatActivity() {

    companion object {
        /** Intent extra 的键：要编辑的笔记 id。 */
        const val EXTRA_NOTE_ID = "extra_note_id"

        /** 表示"新建笔记"的哨兵值。用 -1 是因为 Room 自增主键从 1 开始，不会撞车。 */
        const val NEW_NOTE_ID = -1L

        /**
         * 构造启动本界面的 Intent。
         *
         * 用工厂方法而不是让调用方自己拼 Intent，好处是：
         * 1. extra 的键名和类型只在这一处定义，调用方不可能写错；
         * 2. 将来参数变多时，只需改这个方法，所有调用点自动适配。
         *
         * 【Java 思维对照】相当于 Java 里的静态工厂方法 `NoteEditorActivity.newIntent(...)`。
         */
        fun newIntent(context: Context, noteId: Long): Intent =
            Intent(context, NoteEditorActivity::class.java)
                .putExtra(EXTRA_NOTE_ID, noteId)
    }

    private val viewModel: NoteEditorViewModel by viewModels()

    private lateinit var binding: ActivityNoteEditorBinding

    /**
     * 标记"是否已经把数据填进输入框"。
     *
     * 为什么需要：`note` 这个 StateFlow 在配置变更（如旋转屏幕）后会重新发射，
     * 如果每次都无条件 setText，就会把用户正在编辑的内容覆盖掉。
     * 所以只在第一次收到数据时填充。
     */
    private var hasPrefilledInputs = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        observeNote()
    }

    /** 初始化顶部工具栏：标题、关闭按钮、保存菜单。 */
    private fun setupToolbar() {
        // 新建模式显示"新建笔记"，编辑模式显示"编辑笔记"
        binding.toolbar.setTitle(
            if (viewModel.isNewNote) R.string.editor_title_new else R.string.editor_title_edit
        )

        // 左上角 × 关闭（不保存直接退出）
        binding.toolbar.setNavigationOnClickListener { finish() }

        // 从 XML 加载菜单（里面只有一个「保存」项）
        binding.toolbar.inflateMenu(R.menu.menu_note_editor)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_save -> {
                    saveNote()
                    true
                }
                else -> false
            }
        }
    }

    /**
     * 观察 ViewModel 中的笔记数据，加载完成后填入输入框。
     *
     * 写法与 MainActivity 中观察列表完全一致：`repeatOnLifecycle(STARTED) + collect`，
     * 因为两者都是 StateFlow，而 StateFlow 没有 LiveData 的 `observe()` 方法。
     */
    private fun observeNote() {
        if (viewModel.isNewNote) return // 新建模式没有旧数据要加载

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.note.collect { note ->
                    if (note == null) {
                        // 编辑模式下读不到笔记，说明它已被删除，直接退出避免用户白写
                        return@collect
                    }
                    if (!hasPrefilledInputs) {
                        hasPrefilledInputs = true
                        binding.etTitle.setText(note.title)
                        binding.etContent.setText(note.content)
                        // 把光标移到正文末尾，用户点进来就能继续写
                        binding.etContent.setSelection(note.content.length)
                    }
                }
            }
        }
    }

    /** 收集输入框内容并交给 ViewModel 保存。 */
    private fun saveNote() {
        val title = binding.etTitle.text.toString()
        val content = binding.etContent.text.toString()

        viewModel.save(title, content) { saved ->
            // 回调运行在主线程（viewModelScope 默认使用 Dispatchers.Main），
            // 因此可以直接操作 UI 和 Toast。
            if (saved) {
                Toast.makeText(this, R.string.toast_saved, Toast.LENGTH_SHORT).show()
                // 关闭编辑页，回到列表。列表页的 Flow 会自动感知数据变化并刷新，
                // 不需要手动通知 MainActivity。
                finish()
            } else {
                Toast.makeText(this, R.string.toast_empty_note, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
