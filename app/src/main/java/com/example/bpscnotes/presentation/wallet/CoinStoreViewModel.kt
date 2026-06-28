package com.example.bpscnotes.presentation.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.CoinStoreApiService
import com.example.bpscnotes.data.remote.api.CoinStoreItemDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CoinStoreState(
    val items: List<CoinStoreItemDto> = emptyList(),
    val balance: Int = 0,
    val isLoading: Boolean = false,
    val redeeming: String? = null,   // item id being redeemed
    val successMessage: String? = null,
    val error: String? = null,
)

@HiltViewModel
class CoinStoreViewModel @Inject constructor(
    private val api: CoinStoreApiService,
) : ViewModel() {

    private val _state = MutableStateFlow(CoinStoreState())
    val state: StateFlow<CoinStoreState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val res = api.getStoreItems()
                _state.update { it.copy(isLoading = false, items = res.data?.items ?: emptyList(), balance = res.data?.balance ?: 0) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun redeem(itemId: String) {
        if (_state.value.redeeming != null) return
        viewModelScope.launch {
            _state.update { it.copy(redeeming = itemId, error = null, successMessage = null) }
            try {
                val res = api.redeemItem(itemId)
                _state.update {
                    it.copy(
                        redeeming      = null,
                        balance        = res.data?.balance ?: it.balance,
                        successMessage = "Redeemed successfully! 🎉",
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(redeeming = null, error = e.message ?: "Redemption failed") }
            }
        }
    }

    fun clearMessages() { _state.update { it.copy(successMessage = null, error = null) } }
}
