package com.example.bpscnotes.presentation.wallet

import com.example.bpscnotes.core.network.toUserMessage

import com.example.bpscnotes.data.remote.api.CoinsApiService

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.*
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.bpscnotes.core.events.RefreshEvent
import com.example.bpscnotes.core.events.RefreshEventBus
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CoinWalletUiState(
    val adCoinsPerAd: Int                       = 10,  // fetched from admin config
    val minAdsPerSession: Int                   = 2,   // fetched from admin config
    val adsWatchedToday: Int                    = 0,   // persists across navigation
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
    val claimingTaskId: String?                 = null,
    val isLoadingTransactions: Boolean          = false,
    val transactionPage: Int                    = 1,
    val hasMoreTransactions: Boolean            = true,
    val error: String?                          = null,
    val successMessage: String?                 = null,
    val referralCode: String                    = "",
    val referralStats: com.example.bpscnotes.data.remote.api.ReferralStatsDto? = null,
    val isLoadingReferrals: Boolean             = false,
    val sellerWallet: com.example.bpscnotes.data.remote.api.WalletData? = null,
    val isLoadingSellerWallet: Boolean          = false,
    val isWithdrawing: Boolean                  = false,
    val withdrawalSuccess: String?              = null
)

@HiltViewModel
class CoinWalletViewModel @Inject constructor(
    private val coinsApi: CoinsApiService,
    private val authApi:  com.example.bpscnotes.data.remote.api.AuthApiService,
    private val statsApi: com.example.bpscnotes.data.remote.api.UserStatsApiService,
    private val materialsApi: com.example.bpscnotes.data.remote.api.StudyMaterialsApiService,
    private val bus: RefreshEventBus,
    val coinsConfig: com.example.bpscnotes.core.config.CoinsConfigRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CoinWalletUiState())
    val uiState: StateFlow<CoinWalletUiState> = _uiState.asStateFlow()

    init { load()
        loadReferrals()
        loadSellerWallet()
        viewModelScope.launch { coinsConfig.fetch() }
        // ── Refresh on bus events ─────────────────────────────
        viewModelScope.launch {
            bus.events.collect { event ->
                when (event) {
                    is RefreshEvent.CoinsChanged,
                    is RefreshEvent.QuizCompleted,
                    is RefreshEvent.LessonCompleted,
                    is RefreshEvent.TargetUpdated -> load()
                    else -> {}
                }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                var balanceError: String? = null
                val balanceJob      = async {
                    try { coinsApi.getBalance().data }
                    catch (e: Exception) {
                        Log.e("CoinWalletVM", e.toUserMessage(""), e)
                        balanceError = e.toUserMessage("Failed to load wallet")
                        null
                    }
                }
                val tasksJob        = async { try { coinsApi.getEarnTasks().data?.tasks } catch (e: Exception) { emptyList() } }
                val userJob         = async { try { authApi.getMe().data?.user } catch (_: Exception) { null } }
                val transactionsJob = async { try { coinsApi.getTransactions().data?.transactions } catch (e: Exception) { emptyList() } }
                val adConfigJob     = async { try { coinsApi.getAdConfig().data } catch (_: Exception) { null } }

                val balanceData  = balanceJob.await()
                val tasks        = tasksJob.await() ?: emptyList()
                val transactions = transactionsJob.await() ?: emptyList()
                val adConfig     = adConfigJob.await()

                _uiState.update {
                    it.copy(
                        balance          = balanceData?.balance ?: 0,
                        totalEarned      = balanceData?.totalEarned ?: 0,
                        totalSpent       = balanceData?.totalSpent ?: 0,
                        checkInStreak    = balanceData?.checkInStreak ?: 0,
                        checkedInToday   = balanceData?.checkedInToday ?: false,
                        checkInDays      = balanceData?.checkInDays ?: emptyList(),
                        earnTasks        = tasks,
                        transactions     = transactions,
                        adCoinsPerAd     = adConfig?.coinsPerAd ?: it.adCoinsPerAd,
                        minAdsPerSession = adConfig?.minAdsPerSession ?: it.minAdsPerSession,
                        isLoading        = false,
                        error            = balanceError
                    )
                }
            } catch (e: Exception) {
                Log.e("CoinWalletVM", e.toUserMessage(""), e)
                _uiState.update { it.copy(isLoading = false, error = e.toUserMessage("Failed to load wallet")) }
            }
        }
    }

    /** Called when each rewarded ad completes — credits coins via API */
    /**
     * Called ONCE after all ads in the loop finish.
     * [totalCoins] is the sum across all ads (e.g. 2 ads × 10 = 20).
     */
    fun onAdRewardEarned(totalCoins: Int) {
        // Optimistic balance bump — user sees coins instantly after ads complete
        _uiState.update { s ->
            s.copy(
                balance         = s.balance + totalCoins,
                adsWatchedToday = s.adsWatchedToday + 1,  // +1 session (not per-ad)
                successMessage  = "🎉 +$totalCoins coins earned!"
            )
        }
        viewModelScope.launch {
            try {
                // POST /coins/ad-reward once for the full session
                // Backend reads ad_reward_coins from settings and records one transaction
                val result = coinsApi.recordAdReward(
                    com.example.bpscnotes.data.remote.api.AdRewardRequest(source = "wallet")
                )
                val data = result.data
                if (data != null) {
                    // Sync exact server balance
                    _uiState.update { s ->
                        s.copy(
                            balance     = data.balance,
                            totalEarned = data.totalEarned.takeIf { it > 0 } ?: s.totalEarned,
                        )
                    }
                    // Refresh history so new "Watched ad" row appears
                    refreshTransactions()
                } else {
                    android.util.Log.w("CoinWalletVM", "Ad reward: empty response — keeping optimistic")
                }
            } catch (e: Exception) {
                android.util.Log.e("CoinWalletVM", "Ad reward API failed: ${e.message}", e)
                _uiState.update { s ->
                    s.copy(
                        balance        = s.balance - totalCoins,
                        successMessage = "Failed to credit coins. Please try again."
                    )
                }
            }
        }
    }

    /** Reload just the transactions list without showing the full loading spinner */
    private fun refreshTransactions() {
        viewModelScope.launch {
            try {
                val txns = coinsApi.getTransactions(limit = 50, page = 1).data?.transactions
                if (txns != null) {
                    _uiState.update { it.copy(transactions = txns, transactionPage = 1, hasMoreTransactions = txns.size >= 50) }
                }
            } catch (e: Exception) {
                android.util.Log.w("CoinWalletVM", "refreshTransactions failed: ${e.message}")
            }
        }
    }

    fun showMessage(msg: String) {
        _uiState.update { it.copy(successMessage = msg) }
    }

    /**
     * Shown instead of sharing when the referral code hasn't arrived yet.
     * Also retries the fetch, since a failed /users/referrals call is the usual
     * reason it is missing.
     */
    fun showReferralCodeUnavailable() {
        _uiState.update { it.copy(error = "Referral code not loaded yet — please try again in a moment") }
        loadReferrals()
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

    /**
     * The current user's referral code, or blank if it hasn't loaded yet.
     *
     * This used to fall back to the literal "BPSCNOTES" when the code was
     * missing — which, combined with the DTO field-name bug that made it always
     * missing, meant every user in the app was sharing a code that belonged to
     * nobody. Every signup through those shares was credited to no one. A blank
     * result is the honest answer; callers disable sharing until it arrives.
     */
    fun getReferralCode(): String =
        _uiState.value.referralCode.ifBlank { _uiState.value.referralStats?.referralCode.orEmpty() }

    fun loadSellerWallet() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSellerWallet = true) }
            try {
                val wallet = materialsApi.getWallet().data
                _uiState.update { it.copy(sellerWallet = wallet, isLoadingSellerWallet = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingSellerWallet = false) }
            }
        }
    }

    fun requestWithdrawal(amount: Int, upiId: String?) {
        if (amount < 100) { showMessage("Minimum withdrawal is ₹100"); return }
        viewModelScope.launch {
            _uiState.update { it.copy(isWithdrawing = true) }
            try {
                val res = materialsApi.requestWithdrawal(
                    com.example.bpscnotes.data.remote.api.WithdrawalRequest(amount, upiId?.ifBlank { null })
                )
                _uiState.update { it.copy(isWithdrawing = false, withdrawalSuccess = res.message) }
                loadSellerWallet() // refresh balance
            } catch (e: Exception) {
                _uiState.update { it.copy(isWithdrawing = false, error = e.toUserMessage("Withdrawal failed")) }
            }
        }
    }

    fun clearWithdrawalSuccess() { _uiState.update { it.copy(withdrawalSuccess = null) } }

    fun loadReferrals() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingReferrals = true) }
            try {
                val stats = statsApi.getReferrals().data
                _uiState.update { it.copy(
                    referralStats       = stats,
                    referralCode        = stats?.referralCode ?: it.referralCode,
                    isLoadingReferrals  = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingReferrals = false) }
            }
        }
    }
    fun retry() = load()
}