package com.example.bpscnotes.presentation.marketplace

import com.example.bpscnotes.core.network.toUserMessage

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.MarketplaceApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.bpscnotes.core.events.RefreshEvent
import com.example.bpscnotes.core.events.RefreshEventBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MarketplaceViewModel @Inject constructor(
    private val api: MarketplaceApiService,
    private val bus: RefreshEventBus
) : ViewModel() {

    private val _state = MutableStateFlow(MarketplaceUiState())
    val state: StateFlow<MarketplaceUiState> = _state.asStateFlow()

    init { load()
        // ── Refresh on bus events ─────────────────────────────
        viewModelScope.launch {
            bus.events.collect { event ->
                when (event) {
                    is RefreshEvent.CoinsChanged -> load()
                    else -> {}
                }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val s = _state.value
                val resp = api.list(
                    subject = s.selectedSubject.takeIf { it.isNotEmpty() },
                    search  = s.searchQuery.takeIf { it.isNotEmpty() },
                    sort    = s.selectedSort
                )
                // Backend returns comingSoon=true while feature is not live
                val comingSoon = resp.data?.items?.isEmpty() == true && resp.message?.contains("soon", ignoreCase = true) == true
                _state.update { it.copy(
                    items       = resp.data?.items ?: emptyList(),
                    isLoading   = false,
                    isComingSoon = comingSoon
                )}
            } catch (e: Exception) {
                Log.e("MarketplaceVM", e.toUserMessage(""), e)
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun setSearch(q: String)   { _state.update { it.copy(searchQuery   = q) }; load() }
    fun setSubject(s: String)  { _state.update { it.copy(selectedSubject = s) }; load() }
    fun setSort(sort: String)  { _state.update { it.copy(selectedSort  = sort) }; load() }

    fun purchase(itemId: String) {
        viewModelScope.launch {
            _state.update { it.copy(purchasing = itemId, purchaseError = null) }
            try {
                val result = api.purchase(itemId).data
                val msg = if (result?.alreadyPurchased == true) "You already own this!" else "Purchase successful! 🎉"
                _state.update { it.copy(
                    purchasing     = null,
                    purchaseSuccess = msg,
                    items = it.items.map { item ->
                        if (item.id == itemId) item.copy(isPurchased = true) else item
                    }
                )}
            } catch (e: Exception) {
                _state.update { it.copy(purchasing = null, purchaseError = e.toUserMessage("Purchase failed")) }
            }
        }
    }

    fun clearMessages() { _state.update { it.copy(purchaseSuccess = null, purchaseError = null) } }
}