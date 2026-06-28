package com.example.bpscnotes.core.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.bpscnotes.data.remote.api.CaMcqAnswerSubmitDto
import com.example.bpscnotes.data.remote.api.CaMcqSubmitRequest
import com.example.bpscnotes.di.ApiServiceEntryPoint
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.EntryPointAccessors

class CaMcqSubmitWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_AFFAIR_ID = "affair_id"
        const val KEY_ANSWERS_JSON = "answers_json"
    }

    override suspend fun doWork(): Result {
        val affairId = inputData.getString(KEY_AFFAIR_ID) ?: return Result.failure()
        val answersJson = inputData.getString(KEY_ANSWERS_JSON) ?: return Result.failure()

        return try {
            val api = EntryPointAccessors
                .fromApplication(applicationContext, ApiServiceEntryPoint::class.java)
                .currentAffairsApiService()

            val type = object : TypeToken<Map<String, String>>() {}.type
            val answers: Map<String, String> = Gson().fromJson(answersJson, type)

            val payload = CaMcqSubmitRequest(
                answers = answers.map { (qId, ans) ->
                    CaMcqAnswerSubmitDto(questionId = qId, answer = ans)
                }
            )
            api.submitMcqAttempt(affairId, payload)
            Log.d("CaMcqSubmitWorker", "Retry succeeded for affairId=$affairId")
            Result.success()
        } catch (e: Exception) {
            Log.w("CaMcqSubmitWorker", "Retry attempt $runAttemptCount failed: ${e.message}")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
