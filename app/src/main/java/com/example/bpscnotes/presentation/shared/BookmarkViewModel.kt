package com.example.bpscnotes.presentation.shared

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BookmarkViewModel : ViewModel() {
    private val _bookmarkedIds = MutableStateFlow<Set<String>>(emptySet())
    val bookmarkedIds: StateFlow<Set<String>> = _bookmarkedIds

    fun toggle(id: String) {
        _bookmarkedIds.value = if (_bookmarkedIds.value.contains(id))
            _bookmarkedIds.value - id
        else
            _bookmarkedIds.value + id
    }

    val count get() = _bookmarkedIds.value.size
}