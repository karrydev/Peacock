package com.karrydev.fasttouch.base

import android.widget.Toast
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.flow.Flow

abstract class BaseMviPreferenceFragment<VM : BaseMviViewModel> : PreferenceFragmentCompat() {

    private val baseMviUi by lazy { BaseMviUi(requireContext(), this) }
    protected abstract val viewModel: VM


    protected fun stateFlowHandle(flow: Flow<IUiState>, block: (state: IUiState) -> Unit) {
        baseMviUi.stateFlowHandle(flow, block)
    }

    protected fun showToast(msg: String, during: Int = Toast.LENGTH_SHORT) {
        baseMviUi.showToast(msg, during)
    }
}