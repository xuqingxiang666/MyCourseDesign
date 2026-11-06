package com.example.mycoursedesign

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.mycoursedesign.databinding.ActivityNoteEditorBinding
import com.example.mycoursedesign.markdown.MarkdownRenderer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 输入停止多久后触发草稿自动保存（毫秒）。 */
private const val AUTO_SAVE_DELAY_MS = 600L

/**
 * 笔记编辑界面：整页编辑 + Markdown 预览 + 草稿自动保存。
 *
 * 【界面结构】
 * 顶部工具栏（关闭 / 预览切换 / 保存），下方标题输入框，再下方正文区。
 * 正文区与预览区占据同一位置，同一时刻只显示一个。
 *
 * 【两种模式】
 * - 新建：`NoteEditorActivity.newIntent(context, NEW_NOTE_ID)`
 * - 编辑：`NoteEditorActivity.newIntent(context, noteId)`
 *
 * 【草稿自动保存的工作方式】
 * 1. 标题或正文每次变化 → 取消上一个待执行的保存任务、重新计时 600ms
 * 2. 600ms 内没有新输入 → 调用 `viewModel.autoSave(...)` 真正落库
 * 3. 保存成功后工具栏副标题显示「草稿已自动保存 10:38」
 *
 * 这就是**防抖（debounce）**：用户连续打字时不会每敲一个字就写一次数据库。
 *
 * @AndroidEntryPoint 让 Hilt 能为本 Activity 注入 ViewModel。
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
         * 用工厂方法而不是让调用方自己拼 Intent：extra 的键名和类型只在一处定义，
         * 调用方不可能写错；将来加参数时也只改这里。
         */
        fun newIntent(context: Context, noteId: Long): Intent =
            Intent(context, NoteEditorActivity::class.java)
                .putExtra(EXTRA_NOTE_ID, noteId)
    }

    private val viewModel: NoteEditorViewModel by viewModels()

    private lateinit var binding: ActivityNoteEditorBinding

    /** 是否处于预览模式。 */
    private var isPreviewMode = false

    /** 待执行的自动保存任务；每次输入都取消重排，实现防抖。 */
    private var autoSaveJob: Job? = null

    /**
     * 是否已经把数据填进输入框。
     *
     * `note` 这个 StateFlow 在配置变更（旋转屏幕）后会重新发射，
     * 若无条件 setText 会覆盖用户正在编辑的内容，所以只在第一次填充。
     */
    private var hasPrefilledInputs = false

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupAutoSave()
        observeNote()
        observeDraftState()
    }

    // ==================== 工具栏 ====================

    private fun setupToolbar() {
        binding.toolbar.setTitle(
            if (viewModel.isNewNote) R.string.editor_title_new else R.string.editor_title_edit
        )

        // 左上角 × 关闭。内容已由自动保存落库，所以直接退出不会丢东西。
        binding.toolbar.setNavigationOnClickListener {
            autoSaveJob?.cancel()
            finish()
        }

        binding.toolbar.inflateMenu(R.menu.menu_note_editor)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_toggle_preview -> {
                    togglePreview()
                    true
                }
                R.id.action_save -> {
                    saveNote()
                    true
                }
                else -> false
            }
        }
    }

    /**
     * 切换「编辑 / 预览」。
     *
     * 进入预览前先把当前内容立即落库一次，保证预览显示的是最新内容；
     * 同时取消待执行的自动保存，避免切换瞬间又触发一次写库。
     */
    private fun togglePreview() {
        isPreviewMode = !isPreviewMode

        if (isPreviewMode) {
            autoSaveJob?.cancel()
            viewModel.autoSave(
                binding.etTitle.text.toString(),
                binding.etContent.text.toString()
            )
            renderPreview()
            binding.etContent.visibility = View.GONE
            binding.scrollPreview.visibility = View.VISIBLE
            binding.toolbar.menu.findItem(R.id.action_toggle_preview)?.title =
                getString(R.string.action_edit)
        } else {
            binding.scrollPreview.visibility = View.GONE
            binding.etContent.visibility = View.VISIBLE
            binding.toolbar.menu.findItem(R.id.action_toggle_preview)?.title =
                getString(R.string.action_preview)
        }
    }

    /** 用 [MarkdownRenderer] 把正文渲染成富文本。 */
    private fun renderPreview() {
        val source = binding.etContent.text.toString()
        if (source.isBlank()) {
            binding.tvPreview.text = getString(R.string.preview_empty)
        } else {
            binding.tvPreview.text = MarkdownRenderer.render(this, source)
            // 让 [ ](链接) 生成的 URLSpan 可以点击
            binding.tvPreview.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    // ==================== 草稿自动保存 ====================

    private fun setupAutoSave() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = scheduleAutoSave()
        }
        binding.etTitle.addTextChangedListener(watcher)
        binding.etContent.addTextChangedListener(watcher)
    }

    /**
     * 安排一次延迟保存。
     *
     * 每次调用先取消上一个 Job —— 这就是防抖：只有"最后一次输入后 600ms 内没有新输入"
     * 才会真正执行保存。
     */
    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = lifecycleScope.launch {
            delay(AUTO_SAVE_DELAY_MS)
            viewModel.autoSave(
                binding.etTitle.text.toString(),
                binding.etContent.text.toString()
            )
        }
    }

    /** 订阅自动保存时间，显示在工具栏副标题上。 */
    private fun observeDraftState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.lastAutoSavedAt.collect { savedAt ->
                    if (savedAt == null) return@collect
                    binding.toolbar.subtitle =
                        getString(R.string.draft_saved_at, timeFormat.format(Date(savedAt)))
                }
            }
        }
    }

    // ==================== 加载与保存 ====================

    /**
     * 观察 ViewModel 中的笔记数据，加载完成后填入输入框。
     *
     * 与 MainActivity 写法一致：`repeatOnLifecycle(STARTED) + collect`，
     * 因为 StateFlow 没有 LiveData 的 `observe()` 方法。
     */
    private fun observeNote() {
        if (viewModel.isNewNote) return

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.note.collect { note ->
                    if (note == null) return@collect
                    if (!hasPrefilledInputs) {
                        hasPrefilledInputs = true
                        binding.etTitle.setText(note.title)
                        binding.etContent.setText(note.content)
                        binding.etContent.setSelection(note.content.length)
                    }
                }
            }
        }
    }

    /** 点「保存」：立即落库并返回列表。 */
    private fun saveNote() {
        autoSaveJob?.cancel()
        viewModel.save(
            binding.etTitle.text.toString(),
            binding.etContent.text.toString()
        ) { saved ->
            if (saved) {
                Toast.makeText(this, R.string.toast_saved, Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, R.string.toast_empty_note, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
