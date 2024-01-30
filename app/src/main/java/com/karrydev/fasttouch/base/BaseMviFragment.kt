package com.karrydev.fasttouch.base

import android.os.Bundle
import android.view.View
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import kotlinx.coroutines.flow.Flow

abstract class BaseMviFragment<VM : BaseMviViewModel, Binding : ViewDataBinding> : Fragment() {

    private val baseMviUi by lazy { BaseMviUi(requireContext(), this) }
    protected abstract val viewModel: VM
    protected lateinit var binding: Binding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = initBinding(view)
    }

    protected abstract fun initBinding(view: View): Binding

    protected fun stateFlowHandle(flow: Flow<IUiState>, block: (state: IUiState) -> Unit) {
        baseMviUi.stateFlowHandle(flow, block)
    }
}