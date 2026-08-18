package com.example.bpscnotes.core.network

import com.google.gson.JsonParser
import com.google.gson.stream.MalformedJsonException
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

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

    // SSLException is an IOException, so it MUST be matched before the generic
    // branches below or it disappears into them.
    //
    // This mattered: when the api.bpscnotes.in certificate expired on 14 Aug
    // 2026, every request failed with SSLHandshakeException and the app told
    // users "No internet connection" on a perfectly good connection. The Play
    // reviewer saw exactly that under a working network, concluded the Send OTP
    // button did nothing, and filed a Broken Functionality violation. Blaming
    // the user's connection for a server-side TLS fault also hid the real cause
    // from us. Say what actually failed.
    is SSLException            -> "Couldn't establish a secure connection to the server. " +
                                  "This is a problem on our end — please try again shortly."

    // DNS lookup failed: almost always genuinely offline (or airplane mode).
    is UnknownHostException    -> "No internet connection. Please check your network and try again."

    is SocketTimeoutException  -> "Request timed out. Please try again."

    // The network is up but the server refused or could not be routed to.
    is ConnectException,
    is NoRouteToHostException  -> "Can't reach the server right now. Please try again shortly."

    // Anything else at the transport layer. Deliberately does not assert that
    // the user is offline, because most of the time they are not.
    is IOException             -> "Network error. Please check your connection and try again."

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