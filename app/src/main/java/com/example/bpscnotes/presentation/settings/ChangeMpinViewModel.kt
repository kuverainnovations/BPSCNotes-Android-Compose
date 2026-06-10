package com.example.bpscnotes.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChangeMpinUiState(
    val isLoading: Boolean   = false,
    val error: String?       = null,
    val confirmError: String?= null,
    val success: Boolean     = false,
    // Digit arrays
    val currentDigits: List<String> = List(6) { "" },
    val newDigits:     List<String> = List(6) { "" },
    val confirmDigits: List<String> = List(6) { "" },
)

@HiltViewModel
class ChangeMpinViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChangeMpinUiState())
    val state: StateFlow<ChangeMpinUiState> = _state.asStateFlow()

    // Which field is active: 0=current, 1=new, 2=confirm
    val activeField: Int get() {
        val s = _state.value
        return when {
            s.currentDigits.joinToString("").length < 6 -> 0
            s.newDigits.joinToString("").length < 6     -> 1
            else                                         -> 2
        }
    }

    fun onDigit(digit: String) {
        val s = _state.value
        when (activeField) {
            0 -> appendTo(s.currentDigits, digit) { _state.update { st -> st.copy(currentDigits = it, error = null) } }
            1 -> appendTo(s.newDigits,     digit) { _state.update { st -> st.copy(newDigits = it, error = null) } }
            2 -> appendTo(s.confirmDigits, digit) { _state.update { st -> st.copy(confirmDigits = it, confirmError = null) } }
        }
    }

    fun onBackspace() {
        val s = _state.value
        // Remove from most-recently-filled field
        when {
            s.confirmDigits.any { it.isNotEmpty() } ->
                removeFrom(s.confirmDigits) { _state.update { st -> st.copy(confirmDigits = it) } }
            s.newDigits.any { it.isNotEmpty() } ->
                removeFrom(s.newDigits) { _state.update { st -> st.copy(newDigits = it) } }
            else ->
                removeFrom(s.currentDigits) { _state.update { st -> st.copy(currentDigits = it) } }
        }
    }

    private fun appendTo(list: List<String>, digit: String, update: (List<String>) -> Unit) {
        val m = list.toMutableList()
        val idx = m.indexOfFirst { it.isEmpty() }
        if (idx == -1) return
        m[idx] = digit
        update(m)
    }

    private fun removeFrom(list: List<String>, update: (List<String>) -> Unit) {
        val m = list.toMutableList()
        val idx = m.indexOfLast { it.isNotEmpty() }
        if (idx == -1) return
        m[idx] = ""
        update(m)
    }

    fun changeMpin() {
        val s       = _state.value
        val current = s.currentDigits.joinToString("")
        val new     = s.newDigits.joinToString("")
        val confirm = s.confirmDigits.joinToString("")

        if (current.length < 6 || new.length < 6) {
            _state.update { it.copy(error = "Fill all fields") }; return
        }
        if (new != confirm) {
            _state.update { it.copy(confirmError = "New MPINs do not match") }
            _state.update { it.copy(newDigits = List(6) { "" }, confirmDigits = List(6) { "" }) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val ok = authRepository.changeMpin(current, new)
                if (ok) {
                    _state.update { it.copy(isLoading = false, success = true) }
                } else {
                    _state.update { it.copy(isLoading = false, error = "Failed to change MPIN") }
                    resetFields()
                }
            } catch (e: retrofit2.HttpException) {
                val body = e.response()?.errorBody()?.string() ?: ""
                val msg = try { org.json.JSONObject(body).optString("message", "Failed") } catch (_: Exception) { "Failed" }
                _state.update { it.copy(isLoading = false, error = msg) }
                resetFields()
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Failed") }
                resetFields()
            }
        }
    }

    private fun resetFields() {
        _state.update { it.copy(
            currentDigits = List(6) { "" },
            newDigits     = List(6) { "" },
            confirmDigits = List(6) { "" }
        )}
    }

    fun consumeSuccess() { _state.update { it.copy(success = false) } }
    fun clearError()     { _state.update { it.copy(error = null, confirmError = null) } }
}
