package com.karrydev.fasttouch.base

import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.flow.Flow

abstract class BaseMviPreferenceFragment<VM : BaseMviViewModel> : PreferenceFragmentCompat() {

    private val baseMviUi by lazy { BaseMviUi(requireContext(), this) }
    protected abstract val viewModel: VM


    protected fun stateFlowHandle(flow: Flow<IUiState>, block: (state: IUiState) -> Unit) {
        baseMviUi.stateFlowHandle(flow, block)
    }
}