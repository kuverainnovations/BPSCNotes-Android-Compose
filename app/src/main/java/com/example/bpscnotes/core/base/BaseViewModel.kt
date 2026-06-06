package com.example.bpscnotes.core.base

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.stream.MalformedJsonException
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

abstract class BaseViewModel : ViewModel() {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    protected fun launchWithLoading(block: suspend () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                block()
            } catch (e: HttpException) {
                _error.value = e.response()?.errorBody()?.string() ?: "Server error"
            } catch (e: MalformedJsonException) {
                // Server returned HTML (503 nginx error page) instead of JSON
                // MalformedJsonException extends IOException — must catch BEFORE IOException
                Log.e("BaseVM", "Bad server response — HTML instead of JSON: ${e.message}")
                _error.value = "Server is temporarily unavailable. Please try again."
            } catch (e: UnknownHostException) {
                Log.e("BaseVM", "No DNS/internet: ${e.message}")
                _error.value = "No internet connection"
            } catch (e: SocketTimeoutException) {
                Log.e("BaseVM", "Timeout: ${e.message}")
                _error.value = "Request timed out. Please try again."
            } catch (e: IOException) {
                Log.e("BaseVM", "IO error: $e")
                _error.value = "No internet connection"
            } catch (e: Exception) {
                Log.e("BaseVM", "Unexpected: $e")
                _error.value = e.message ?: "Something went wrong"
            } finally {
                _isLoading.value = false
            }
        }
    }
}