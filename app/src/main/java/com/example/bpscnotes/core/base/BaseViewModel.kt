package com.example.bpscnotes.core.base

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.core.network.toUserMessage
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    /** Report a validation failure without making a network call. */
    protected fun setError(message: String?) { _error.value = message }

    protected fun launchWithLoading(block: suspend () -> Unit) {
        // Rapid taps: the submit buttons are disabled while isLoading, but a
        // burst of taps can land before recomposition applies that. Dropping
        // re-entrant calls here stops duplicate OTP sends and double
        // registrations. Checked before launching, so the guard is set on the
        // caller's (main) thread rather than a dispatch later.
        if (_isLoading.value == true) return
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                Log.e("BaseVM", "Error: $e")
                _error.value = e.toUserMessage()
            } finally {
                _isLoading.value = false
            }
        }
    }
}