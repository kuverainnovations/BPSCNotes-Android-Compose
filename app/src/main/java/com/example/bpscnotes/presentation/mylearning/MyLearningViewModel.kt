package com.example.bpscnotes.presentation.mylearning

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bpscnotes.data.remote.api.AuthApiService
import com.example.bpscnotes.data.remote.api.CourseDto
import com.example.bpscnotes.data.remote.api.CoursesApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyLearningUiState(
    val storeCourses: List<CourseDto>    = emptyList(),   // all published courses (store tab)
    val enrolledCourses: List<CourseDto> = emptyList(),   // user's enrolled courses
    val userCoins: Int                   = 0,
    val isLoading: Boolean               = true,
    val error: String?                   = null
)

@HiltViewModel
class MyLearningViewModel @Inject constructor(
    private val coursesApi: CoursesApiService,
    private val authApi: AuthApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyLearningUiState())
    val uiState: StateFlow<MyLearningUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Parallel: all courses + enrolled courses + user profile for coins
                val allCoursesJob     = async { coursesApi.getCourses(limit = 20).data?.courses ?: emptyList() }
                val enrolledJob       = async {
                    try { coursesApi.getCourses(limit = 20).data?.courses?.filter { it.enrollment?.status=="active" } ?: emptyList() }
                    catch (e: Exception) { emptyList() }
                }
                val userJob           = async {
                    try { authApi.getMe().data?.user?.coins ?: 0 } catch (e: Exception) { 0 }
                }

                _uiState.update {
                    it.copy(
                        storeCourses    = allCoursesJob.await(),
                        enrolledCourses = enrolledJob.await(),
                        userCoins       = userJob.await(),
                        isLoading       = false
                    )
                }
            } catch (e: Exception) {
                Log.e("MyLearningVM", e.message ?: "", e)
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load courses") }
            }
        }
    }

    fun retry() = load()
}
