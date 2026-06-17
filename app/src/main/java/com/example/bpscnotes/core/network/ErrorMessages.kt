package com.example.bpscnotes.core.network

import com.google.gson.JsonParser
import com.google.gson.stream.MalformedJsonException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Maps a caught [Throwable] to a short, user-facing message - never the raw
 * exception text for network-level failures (e.g. UnknownHostException's
 * "Unable to resolve host \"api-stg.bpscnotes.in\": No address associated
 * with hostname", which is meaningless to an end user and was previously
 * shown verbatim under a generic "Something went wrong" title).
 *
 * Mirrors the mapping in [com.example.bpscnotes.core.base.BaseViewModel]'s
 * launchWithLoading - use this directly in ViewModels that don't extend
 * BaseViewModel (most don't). Replace `e.message ?: fallback` with
 * `e.toUserMessage(fallback)` to pick up friendly network-error messages.
 */
fun Throwable.toUserMessage(default: String = "Something went wrong"): String = when (this) {
    is HttpException        -> httpErrorMessage()
    // MalformedJsonException extends IOException - server returned HTML
    // (e.g. a 503 nginx error page) instead of JSON. Must be checked
    // before the generic IOException branch below.
    is MalformedJsonException -> "Server is temporarily unavailable. Please try again."
    is UnknownHostException    -> "No internet connection"
    is SocketTimeoutException  -> "Request timed out. Please try again."
    is IOException             -> "No internet connection"
    else                        -> message ?: default
}

/**
 * Extracts the `message` field from a NestJS error response body
 * (`{"statusCode":400,"message":"...","error":"Bad Request"}`), instead of
 * returning the raw JSON. `message` can also be a string array (returned by
 * class-validator for multi-field validation errors) - those are joined
 * with newlines. Falls back to "Server error" if the body isn't JSON or has
 * no `message` field.
 */
private fun HttpException.httpErrorMessage(): String {
    val body = runCatching { response()?.errorBody()?.string() }.getOrNull()
    val parsed = body?.let {
        runCatching {
            val msg = JsonParser.parseString(it).asJsonObject.get("message")
            when {
                msg == null || msg.isJsonNull -> null
                msg.isJsonArray -> msg.asJsonArray.joinToString("\n") { e -> e.asString }
                else -> msg.asString
            }
        }.getOrNull()
    }
    return parsed ?: "Server error"
}