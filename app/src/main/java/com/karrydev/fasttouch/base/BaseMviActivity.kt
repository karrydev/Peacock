package com.karrydev.fasttouch.base

import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
import kotlinx.coroutines.flow.Flow

abstract class BaseMviActivity<VM: BaseMviViewModel, Binding : ViewDataBinding>: AppCompatActivity() {

    private val baseMviUi by lazy { BaseMviUi(this, this) }
    protected abstract val viewModel: VM
    protected lateinit var binding: Binding

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        binding = initBinding()
    }

    protected abstract fun initBinding() : Binding

    protected fun stateFlowHandle(flow: Flow<IUiState>, block: (state:IUiState) -> Unit) {
        baseMviUi.stateFlowHandle(flow, block)
    }

    protected fun showToast(msg: String, during: Int = Toast.LENGTH_SHORT) {
        baseMviUi.showToast(msg, during)
    }
}