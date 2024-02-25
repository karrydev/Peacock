package com.karrydev.peacock.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel基类，基于MVI封装
 */
abstract class BaseViewModel : ViewModel() {
}