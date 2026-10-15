package com.example.mycoursedesign

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.core.data.entity.Note
import com.example.mycoursedesign.databinding.ItemNoteBinding

/**
 * 笔记列表的适配器。
 *
 * 【Java 思维对照】
 * 相当于 Java 中继承 `RecyclerView.Adapter<VH>` 的经典 ViewHolder 模式。
 * 但这里继承了 [ListAdapter] 而不是 `RecyclerView.Adapter`，区别在于：
 *
 * | | RecyclerView.Adapter（旧写法） | ListAdapter（本类） |
 * |---|---|---|
 * | 数据存放 | 自己维护 `private List<Note> notes` | 父类内部持有，用 `submitList()` 提交 |
 * | 刷新方式 | 手动 `notifyDataSetChanged()` | 自动用 DiffUtil 算出最小变更集 |
 * | 刷新效果 | 整个列表重绘，有闪烁、无动画 | 只刷新变化的条目，自带增删动画 |
 * | 性能 | O(n) 全量重绑 | O(n) 差分对比，但只重绑变化项 |
 *
 * 【为什么"判断哪些条目变了"这件事很重要】
 * [DIFF] 里的 `areItemsTheSame` / `areContentsTheSame` 是在回答
 * "这两条数据是不是同一条"和"同一条的内容变没变"。
 * 这和我们在冲突解决机制里用 `lastModified` + `version` 判断
 * "哪份数据更新"是同一类问题：**都要有一套确定性的比较规则**。
 */
class NoteAdapter(
    private val onItemClick: (Note) -> Unit
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    /**
     * 单个条目的 ViewHolder。
     *
     * 构造参数从 `View` 换成了 [ItemNoteBinding]：因为开启了 ViewBinding，
     * 可以直接用 `binding.tvTitle` 访问控件，不需要再写
     * `itemView.findViewById<TextView>(R.id.tvTitle)`。
     */
    class NoteViewHolder(
        private val binding: ItemNoteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note, onItemClick: (Note) -> Unit) {
            binding.tvTitle.text = note.title.ifBlank { "无标题" }
            binding.tvContent.text = note.content.ifBlank { "无内容" }

            // 状态徽章：置顶优先显示，其次显示归档。
            val badge = when {
                note.isPinned -> "★ 已置顶"
                note.isArchived -> "已归档"
                else -> null
            }
            binding.tvBadge.text = badge.orEmpty()
            binding.tvBadge.visibility = if (badge == null) View.GONE else View.VISIBLE

            binding.root.setOnClickListener { onItemClick(note) }
        }
    }

    companion object {
        /**
         * DiffUtil 回调：告诉 ListAdapter 如何比较两条笔记。
         *
         * - `areItemsTheSame`：判断是否为"同一个对象"。这里用主键 id，
         *   因为 id 是数据库给的唯一标识，只要 id 相同就是同一条笔记。
         * - `areContentsTheSame`：判断"同一条笔记的内容是否变化"。这里直接用
         *   `==`，因为 [Note] 是 data class，编译器自动生成的 `equals()`
         *   会逐字段比较 —— 这也是当初坚持用 data class 的原因之一。
         */
        private val DIFF = object : DiffUtil.ItemCallback<Note>() {
            override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean =
                oldItem == newItem
        }
    }
}
