package com.example.mycoursedesign

import android.os.Bundle
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.mycoursedesign.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 应用主界面：展示笔记列表，并提供新增入口。
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

    /** 初始化列表。 */
    private fun setupRecyclerView() {
        adapter = NoteAdapter { note ->
            // TODO(阶段3-提交#4)：点击进入编辑界面
        }
        binding.recyclerView.adapter = adapter
    }

    /** 悬浮按钮：弹出新建笔记对话框。 */
    private fun setupFab() {
        binding.fabAdd.setOnClickListener { showAddNoteDialog() }
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

    /** 弹出对话框，输入标题与正文后保存新笔记。 */
    private fun showAddNoteDialog() {
        // 用代码动态构建输入布局，避免为一个小对话框单独建 XML 文件。
        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(56, 40, 56, 20)
        }

        val etTitle = EditText(this).apply { hint = "请输入标题" }
        val etContent = EditText(this).apply { hint = "请输入内容" }

        container.addView(etTitle)
        container.addView(etContent)

        AlertDialog.Builder(this)
            .setTitle("新建笔记")
            .setView(container)
            .setPositiveButton("保存") { _, _ ->
                val title = etTitle.text.toString().trim()
                val content = etContent.text.toString().trim()
                // 标题和内容全为空时不保存，避免产生空记录。
                if (title.isNotEmpty() || content.isNotEmpty()) {
                    viewModel.addNote(title, content)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
