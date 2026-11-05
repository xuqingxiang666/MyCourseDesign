package com.example.mycoursedesign

import android.os.Bundle
import android.view.View
import android.widget.Toast
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
 * 界面由三部分组成：顶部工具栏（切换"我的笔记 / 已归档"）、笔记列表、右下角新增按钮。
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
        setupToolbar()
        observeViewMode()
        observeNotes()
    }

    /** 初始化列表：点击编辑、长按弹出操作菜单。 */
    private fun setupRecyclerView() {
        adapter = NoteAdapter(
            onItemClick = { note -> onNoteClicked(note) },
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
     * 初始化顶部工具栏。
     *
     * 菜单里只有一个"切换归档视图"的项。这里只负责加载菜单和绑定点击事件，
     * 菜单文字的更新交给 [observeViewMode]，因为文字要随视图模式变化。
     */
    private fun setupToolbar() {
        binding.toolbar.inflateMenu(R.menu.menu_main)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_toggle_archive -> {
                    viewModel.toggleShowArchived()
                    true
                }
                R.id.action_theme -> {
                    showThemeDialog()
                    true
                }
                else -> false
            }
        }
    }

    /**
     * 弹出主题选择对话框（跟随系统 / 浅色 / 深色）。
     *
     * 【实现要点】
     * - 用 `setSingleChoiceItems` 做单选列表，并把当前模式对应的序号设为选中项
     * - 选中后立刻调用 [ThemePreferences.saveMode]，它会触发
     *   `AppCompatDelegate.setDefaultNightMode(...)`，AppCompat 随即**自动重建
     *   所有 Activity**，于是界面立即切换主题，无需手动刷新
     * - 因为会重建，所以先 `dismiss()` 关掉对话框，避免它跟着重建出现错位
     */
    private fun showThemeDialog() {
        val modes = ThemePreferences.supportedModes
        val labels = arrayOf(
            getString(R.string.theme_follow_system),
            getString(R.string.theme_light),
            getString(R.string.theme_dark)
        )
        // indexOf 返回当前模式在列表中的位置；理论上一定找得到，兜底取 0
        val checkedIndex = modes.indexOf(ThemePreferences.currentMode(this)).coerceAtLeast(0)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.theme_dialog_title)
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                dialog.dismiss()
                ThemePreferences.saveMode(this, modes[which])
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    /**
     * 观察"当前视图模式"，同步更新工具栏标题、菜单文字和空状态文案。
     *
     * 三处文字同时更新，是为了让用户随时明确自己正在看
     * "我的笔记"还是"已归档" —— 否则两个视图在空数据时看起来完全一样。
     */
    private fun observeViewMode() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isShowingArchived.collect { archived ->
                    binding.toolbar.setTitle(
                        if (archived) R.string.list_title_archived else R.string.list_title_active
                    )
                    binding.toolbar.menu.findItem(R.id.action_toggle_archive)?.title = getString(
                        if (archived) R.string.action_show_active else R.string.action_show_archived
                    )
                    binding.tvEmpty.setText(
                        if (archived) R.string.empty_archived_hint else R.string.empty_active_hint
                    )
                }
            }
        }
    }

    /**
     * 订阅 ViewModel 暴露的笔记列表状态。
     *
     * ★ 这里是 Kotlin/协程最容易踩坑的地方。
     *
     * 【Java 思维对照】
     * 用 LiveData 时写的是：
     *     viewModel.getNotes().observe(this, notes -> adapter.setNotes(notes));
     * 一个 `observe()` 就够了，因为 LiveData 自带生命周期感知。
     *
     * 但 StateFlow **没有 `observe()` 方法**，直接写会报
     * `Unresolved reference: observe`。正确做法是 `repeatOnLifecycle`：
     * 界面可见（STARTED）时开始收集，不可见时自动取消，重新可见时自动恢复。
     */
    private fun observeNotes() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.notes.collect { notes ->
                    adapter.submitList(notes)
                    // 列表为空时显示提示文字，避免用户以为界面出错
                    binding.tvEmpty.visibility =
                        if (notes.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    /** 打开编辑器。传 [NoteEditorActivity.NEW_NOTE_ID] 表示新建。 */
    private fun openEditor(noteId: Long) {
        startActivity(NoteEditorActivity.newIntent(this, noteId))
    }

    /**
     * 长按笔记弹出的操作菜单。
     *
     * 【为什么用"动态构建选项数组"而不是写死几个按钮】
     * 菜单内容依赖笔记自身的状态：
     * - 已归档的笔记：只能「取消归档」或「删除」，不应再提供"置顶/归档"
     * - 活跃的笔记：提供「置顶/取消置顶」「归档」「删除」
     *
     * 用两个平行列表（`labels` 放显示文字、`actions` 放对应操作）一一对应，
     * 点击时按索引取 action 执行。将来增加选项只需各追加一项，不必改结构。
     */
    private fun showNoteOptions(note: Note) {
        val labels = mutableListOf<String>()
        val actions = mutableListOf<() -> Unit>()

        if (note.isArchived) {
            // 归档视图下：允许取消归档
            labels += getString(R.string.action_unarchive)
            actions += { changeArchived(note, false) }
        } else {
            // 活跃视图下：允许置顶/取消置顶、归档
            labels += getString(
                if (note.isPinned) R.string.action_unpin else R.string.action_pin
            )
            actions += { changePinned(note, !note.isPinned) }

            labels += getString(R.string.action_archive)
            actions += { changeArchived(note, true) }
        }

        // 加密 / 取消加密。加密与归档是两个独立维度，所以放在 if/else 之外，
        // 两种视图下都提供（归档笔记也可能需要加密）。
        if (note.isEncrypted) {
            labels += getString(R.string.action_decrypt)
            actions += { changeEncryption(note, encrypt = false) }
        } else {
            labels += getString(R.string.action_encrypt)
            actions += { changeEncryption(note, encrypt = true) }
        }

        labels += getString(R.string.action_delete)
        actions += { confirmDelete(note) }

        MaterialAlertDialogBuilder(this)
            .setTitle(note.title.ifBlank { getString(R.string.note_untitled) })
            .setItems(labels.toTypedArray()) { _, which -> actions[which]() }
            .show()
    }

    /** 切换置顶状态并给出提示。 */
    private fun changePinned(note: Note, pinned: Boolean) {
        viewModel.setPinned(note, pinned)
        Toast.makeText(
            this,
            if (pinned) R.string.toast_pinned else R.string.toast_unpinned,
            Toast.LENGTH_SHORT
        ).show()
    }

    /** 切换归档状态并给出提示。 */
    private fun changeArchived(note: Note, archived: Boolean) {
        viewModel.setArchived(note, archived)
        Toast.makeText(
            this,
            if (archived) R.string.toast_archived else R.string.toast_unarchived,
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * 点击笔记时的分流：
     * - 普通笔记：直接进编辑器
     * - **加密笔记**：先验证身份，通过后以只读方式展示解密内容
     *
     * 【为什么加密笔记不直接进编辑器】
     * 编辑器保存时写的是明文。如果允许编辑加密笔记，保存后就会出现
     * "isEncrypted = true 但 content 是明文"的不一致状态，解密必然失败。
     * 所以加密状态下先做只读预览；需要编辑时，让用户显式「取消加密」。
     * 这是刻意的取舍：用一点操作步骤换取数据一致性。
     */
    private fun onNoteClicked(note: Note) {
        if (note.isEncrypted) {
            authenticateAndPreview(note)
        } else {
            openEditor(note.id)
        }
    }

    /**
     * 弹出身份验证，通过后展示解密内容。
     *
     * 流程：检查设备是否支持 → BiometricPrompt → 解密 → 只读预览对话框
     */
    private fun authenticateAndPreview(note: Note) {
        // 先检查：设备没设置指纹/锁屏时，直接给出明确指引，而不是弹一个必然失败的框
        if (!BiometricGate.canAuthenticate(this)) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.biometric_title)
                .setMessage(R.string.biometric_unavailable)
                .setPositiveButton(R.string.preview_close, null)
                .show()
            return
        }

        BiometricGate.authenticate(
            activity = this,
            title = getString(R.string.biometric_title),
            subtitle = getString(R.string.biometric_subtitle),
            onSuccess = {
                // 验证通过，才去解密。没通过就不碰数据，明文永远不会提前出现在内存里被 UI 用到
                viewModel.readDecryptedContent(note) { plaintext ->
                    if (plaintext == null) {
                        toast(R.string.toast_decrypt_failed)
                    } else {
                        showPreviewDialog(note, plaintext)
                    }
                }
            },
            onFailure = { reason ->
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.biometric_title)
                    .setMessage(reason + "\n\n" + getString(R.string.biometric_failed))
                    .setPositiveButton(R.string.preview_close, null)
                    .show()
            }
        )
    }

    /** 只读预览对话框：展示解密后的正文。 */
    private fun showPreviewDialog(note: Note, plaintext: String) {
        val body = if (plaintext.isBlank()) getString(R.string.note_no_content) else plaintext
        MaterialAlertDialogBuilder(this)
            .setTitle(note.title.ifBlank { getString(R.string.note_untitled) })
            .setMessage(body + "\n\n" + getString(R.string.preview_hint_edit))
            .setPositiveButton(R.string.preview_close, null)
            .show()
    }

    /**
     * 加密 / 取消加密。
     *
     * @param encrypt true = 加密，false = 取消加密（解密后写回明文）
     */
    private fun changeEncryption(note: Note, encrypt: Boolean) {
        if (encrypt) {
            viewModel.encryptNote(note) { success ->
                toast(if (success) R.string.toast_encrypted else R.string.toast_encrypt_failed)
            }
        } else {
            viewModel.removeEncryption(note) { success ->
                toast(if (success) R.string.toast_decrypted else R.string.toast_decrypt_failed)
            }
        }
    }

    /** Toast 的小包装，避免每次写三行。 */
    private fun toast(resId: Int) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    }

    /**
     * 删除确认对话框。
     *
     * 删除不可恢复，必须二次确认。确认后由 ViewModel 执行删除，
     * 列表通过 Flow 自动刷新，不需要手动通知适配器。
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
}
