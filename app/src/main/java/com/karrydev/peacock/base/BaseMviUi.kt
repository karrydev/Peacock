package com.karrydev.peacock.base

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.karrydev.peacock.util.showToast
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class BaseMviUi(private val context: Context, private val lifecycleOwner: LifecycleOwner) {

    fun stateFlowHandle(flow: Flow<IUiState>, block: (state: IUiState) -> Unit) {
        // 开启新的协程
        lifecycleOwner.lifecycleScope.launch {
            // repeatOnLifecycle 是一个挂起函数；低于目标生命周期状态会取消协程，内部由suspendCancellableCoroutine实现
            // 最早在 View 处于 RESUMED 状态时从数据流收集数据，并在
            // 生命周期进入 PAUSED 状态时 SUSPENDS（挂起）收集操作。
            // 在 View 转为 DESTROYED 状态时取消数据流的收集操作。
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                flow.filter { iUiState -> iUiState !is EmptyState }.collect {
                    block(it)
                }
            }
        }
    }

    /**
     * TODO
     */
    fun showLoadingDialog() {
        showToast("加载中")
    }

    fun dismissLoadingDialog() {
        showToast("加载完成")
    }
}