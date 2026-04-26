package com.example.bpscnotes.presentation.wallet

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CoinWalletUiState(
    val balance: Int                            = 0,
    val totalEarned: Int                        = 0,
    val totalSpent: Int                         = 0,
    val checkInStreak: Int                      = 0,
    val checkedInToday: Boolean                 = false,
    val checkInDays: List<CheckInDayDto>        = emptyList(),
    val earnTasks: List<EarnTaskDto>            = emptyList(),
    val transactions: List<CoinTransactionDto>  = emptyList(),
    val isLoading: Boolean                      = true,
    val isCheckingIn: Boolean                   = false,
    val error: String?                          = null,
    val successMessage: String?                 = null
)

@HiltViewModel
class CoinWalletViewModel @Inject constructor(
    private val coinsApi: CoinsApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoinWalletUiState())
    val uiState: StateFlow<CoinWalletUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val balanceJob      = async { coinsApi.getBalance().data }
                val tasksJob        = async { try { coinsApi.getEarnTasks().data?.tasks } catch (e: Exception) { emptyList() } }
                val transactionsJob = async { try { coinsApi.getTransactions().data?.transactions } catch (e: Exception) { emptyList() } }

                val balanceData  = balanceJob.await()
                val tasks        = tasksJob.await() ?: emptyList()
                val transactions = transactionsJob.await() ?: emptyList()

                _uiState.update {
                    it.copy(
                        balance        = balanceData?.balance ?: 0,
                        totalEarned    = balanceData?.totalEarned ?: 0,
                        totalSpent     = balanceData?.totalSpent ?: 0,
                       // checkInStreak  = balanceData?.balance?.checkInStreak ?: 0,
                      //  checkedInToday = balanceData?.balance?.checkedInToday ?: false,
                        checkInDays    = balanceData?.checkInDays ?: emptyList(),
                        earnTasks      = tasks,
                        transactions   = transactions,
                        isLoading      = false
                    )
                }
            } catch (e: Exception) {
                Log.e("CoinWalletVM", e.message ?: "", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load wallet") }
            }
        }
    }

    fun checkIn() {
        if (_uiState.value.checkedInToday || _uiState.value.isCheckingIn) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingIn = true) }
            try {
                val result = coinsApi.checkIn().data
                _uiState.update {
                    it.copy(
                        balance        = result?.balance ?: it.balance,
                      //  checkInStreak  = result?.checkInStreak ?: it.checkInStreak,
                        checkedInToday = true,
                        isCheckingIn   = false,
                        successMessage = "✅ Daily check-in done! 🪙"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isCheckingIn = false, error = "Check-in failed. Try again.") }
            }
        }
    }

    fun claimTask(taskId: String) {
        viewModelScope.launch {
            try {
                coinsApi.claimTask(taskId)
                // Refresh wallet to get updated balance + mark task complete
                load()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Could not claim reward.") }
            }
        }
    }

    fun clearMessage() { _uiState.update { it.copy(successMessage = null, error = null) } }
    fun retry() = load()
}
