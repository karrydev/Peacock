package com.karrydev.peacock.base

import android.os.Bundle
import android.view.View
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.karrydev.peacock.vm.GlobalViewModel

abstract class BaseFragment<VM : BaseViewModel, Binding : ViewDataBinding> : Fragment() {

    protected val globalViewModel: GlobalViewModel by activityViewModels()
    protected abstract val viewModel: VM
    protected lateinit var binding: Binding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = initBinding(view)
    }

    protected abstract fun initBinding(view: View): Binding
}