package com.karrydev.fasttouch.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel基类，基于MVI封装
 */
abstract class BaseMviViewModel : ViewModel() {
    /**
     * UI 状态
     */
    private val _uiStateFlow by lazy { MutableStateFlow(initUiState()) }
    val uiStateFlow = _uiStateFlow.asStateFlow()

    /**
     * 事件意图
     */
    private val _userIntent = MutableSharedFlow<IUiIntent>()
    val userIntent = _userIntent.asSharedFlow()

    init {
        viewModelScope.launch {
            userIntent.distinctUntilChanged().collect {
                handleUserIntent(it)
            }
        }
    }

    /**
     * 子类可重写此状态初始化方法
     */
    protected open fun initUiState(): IUiState {
        return EmptyState()
    }

    /**
     * 子类必须重写该方法，实现Intent的处理
     */
    abstract fun handleUserIntent(intent: IUiIntent)

    /**
     * 子类更新state的方法
     */
    protected fun sendUiState(block: IUiState.() -> IUiState) {
        _uiStateFlow.update { block(it) } // 更新状态
    }

    /**
     * 提供给View层调用的Intent分发方法
     * 这是唯一暴露给View层的方法
     */
    fun dispatchIntent(intent: IUiIntent) {
        viewModelScope.launch {
            _userIntent.emit(intent)
        }
    }
}