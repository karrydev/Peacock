package com.karrydev.peacock.base

import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import com.karrydev.peacock.vm.GlobalViewModel

abstract class BaseActivity<VM : BaseViewModel, Binding : ViewDataBinding> : AppCompatActivity() {

    protected val globalViewModel: GlobalViewModel by viewModels()
    protected lateinit var viewModel: VM
    protected lateinit var binding: Binding

    @get: LayoutRes
    abstract val layoutId: Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, layoutId)

        initVM()
        initData()
        initView()
        initListener()
    }

    private fun initVM() {
        viewModel = ViewModelProvider(this)[getViewModelClass()]
    }

    abstract fun initData()

    abstract fun initView()

    abstract fun initListener()

    abstract fun getViewModelClass(): Class<VM>
}