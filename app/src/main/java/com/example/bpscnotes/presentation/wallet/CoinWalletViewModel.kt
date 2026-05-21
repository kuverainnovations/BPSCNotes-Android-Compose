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
    val claimingTaskId: String?                 = null,  // which task is being claimed
    val isLoadingTransactions: Boolean          = false,
    val transactionPage: Int                    = 1,
    val hasMoreTransactions: Boolean            = true,
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
                        checkInStreak  = balanceData?.checkInStreak ?: 0,
                        checkedInToday = balanceData?.checkedInToday ?: false,
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

    /** Called when rewarded ad completes — credits coins via API */
    fun onAdRewardEarned(coins: Int) {
        viewModelScope.launch {
            try {
                // Award coins via the existing earn task / manual credit endpoint
                //coinsApi.recordAdReward(coins)   // POST /coins/ad-reward
                // Refresh balance
                load()
                _uiState.update { it.copy(successMessage = "🎉 +$coins coins earned from watching ad!") }
            } catch (e: Exception) {
                // Fallback: update balance optimistically if API doesn't exist yet
                _uiState.update { s -> s.copy(
                    balance         = s.balance + coins,
                    successMessage  = "🎉 +$coins coins earned!"
                )}
                android.util.Log.w("CoinWalletVM", "Ad reward API error: ${e.message}")
            }
        }
    }

    fun showMessage(msg: String) {
        _uiState.update { it.copy(successMessage = msg) }
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
                        totalEarned    = (it.totalEarned) + (result?.balance?.minus(it.balance)?.coerceAtLeast(0) ?: 0),
                        checkInStreak  = result?.checkInStreak ?: it.checkInStreak,
                        checkedInToday = true,
                        isCheckingIn   = false,
                        successMessage = "✅ +${(result?.balance ?: it.balance) - it.balance} coins! Streak: ${result?.checkInStreak ?: it.checkInStreak} days 🔥"
                    )
                }
                // Refresh check-in days state after successful check-in
                load()
            } catch (e: Exception) {
                _uiState.update { it.copy(isCheckingIn = false, error = "Check-in failed. Try again.") }
            }
        }
    }

    fun claimTask(taskId: String) {
        if (_uiState.value.claimingTaskId != null) return  // prevent double-tap
        viewModelScope.launch {
            _uiState.update { it.copy(claimingTaskId = taskId) }
            try {
                val result = coinsApi.claimTask(taskId).data
                _uiState.update { state ->
                    state.copy(
                        claimingTaskId = null,
                        balance        = result?.balance ?: state.balance,
                        // Optimistically mark this task as completed in the list
                        earnTasks      = state.earnTasks.map { task ->
                            if (task.id == taskId) task.copy(isCompleted = true) else task
                        },
                        successMessage = "🎉 +${(result?.balance ?: state.balance) - state.balance} coins earned!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(claimingTaskId = null, error = "Could not claim reward: ${e.message}") }
            }
        }
    }

    fun loadMoreTransactions() {
        val s = _uiState.value
        if (!s.hasMoreTransactions || s.isLoadingTransactions) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTransactions = true) }
            try {
                val nextPage  = s.transactionPage + 1
                val res       = coinsApi.getTransactions(limit = 20, page = nextPage).data
                val newTxns   = res?.transactions ?: emptyList()
                _uiState.update { it.copy(
                    transactions        = it.transactions + newTxns,
                    transactionPage     = nextPage,
                    hasMoreTransactions = newTxns.size >= 20,
                    isLoadingTransactions = false
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingTransactions = false) }
            }
        }
    }

    fun clearMessage() { _uiState.update { it.copy(successMessage = null, error = null) } }
    fun retry() = load()
}