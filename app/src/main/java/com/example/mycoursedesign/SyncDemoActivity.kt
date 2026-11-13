package com.example.mycoursedesign

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.mycoursedesign.databinding.ActivitySyncDemoBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 模拟同步演示页。
 *
 * 【这是给答辩准备的页面】
 * 冲突解决发生在数据层，看不见。这个页面把"制造冲突 → 执行同步 → 查看判定依据"
 * 变成三个按钮，现场演示时按顺序点即可。
 *
 * 【为什么"另一端"用本地表模拟】
 * 详见 [com.example.core.data.remote.RemoteDataSource]：
 * 冲突判定不依赖"远端是数据库还是 HTTP"，用表模拟可以离线演示、可重复、可单测。
 * 将来接真实后端时，只要换掉 RemoteDataSource 的实现，本页代码不用改。
 */
@AndroidEntryPoint
class SyncDemoActivity : AppCompatActivity() {

    companion object {
        fun newIntent(context: Context): Intent =
            Intent(context, SyncDemoActivity::class.java)
    }

    private val viewModel: SyncDemoViewModel by viewModels()

    private lateinit var binding: ActivitySyncDemoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySyncDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnSeed.setOnClickListener { viewModel.seed() }
        binding.btnSimulateEdit.setOnClickListener { viewModel.simulateRemoteEdit() }
        binding.btnSimulateDelete.setOnClickListener { viewModel.simulateRemoteDelete() }
        binding.btnSync.setOnClickListener { viewModel.sync() }
        binding.btnReset.setOnClickListener { viewModel.resetRemote() }

        observeState()
    }

    override fun onResume() {
        super.onResume()
        // 从列表页改了笔记再回来时，条数要刷新
        viewModel.refresh()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.tvCounts.text = getString(
                        R.string.sync_demo_counts,
                        state.localCount,
                        state.localDeletedCount,
                        state.remoteCount
                    )
                    binding.tvResult.text = state.message
                }
            }
        }
    }
}
