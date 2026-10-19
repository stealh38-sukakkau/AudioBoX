package com.audiobox.player.data.jellyfin

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import java.util.Locale

object JellyfinHttpErrors {
    fun message(code: Int, authentication: Boolean = false): String = when (code) {
        400, 401 -> if (authentication) "Invalid username or password." else "Jellyfin session expired. Reconnect to continue."
        403 -> "Jellyfin denied access to this library."
        404 -> "The requested Jellyfin item is no longer available."
        in 500..599 -> "Jellyfin server error (HTTP $code). Try again later."
        else -> "Jellyfin request failed (HTTP $code)."
    }

    /** Produces a user-safe message for transport failures without exposing endpoint details. */
    fun requestMessage(endpoint: String, cause: Throwable): String {
        val isHttp = endpoint.startsWith("http://", ignoreCase = true)
        val isCleartextPolicyFailure = hasCleartextPolicyCause(cause)
        return if (isHttp && isCleartextPolicyFailure) {
            "Android blocked this HTTP connection under its network security policy. Use HTTPS for this server."
        } else {
            "Could not complete Jellyfin request. Check server and network diagnostics."
        }
    }

    /** Class-only diagnostics. Exception messages can contain server hostnames or request data. */
    fun diagnostic(exception: Throwable?): String {
        if (exception == null) return "exception_chain=HttpStatus failure=http_status"
        val chain = causeChain(exception)
        val classes = chain.joinToString(">") { it.javaClass.simpleName.ifBlank { "Throwable" } }
        val failure = when {
            hasCleartextPolicyCause(exception) -> "cleartext_policy"
            chain.any { it is SocketTimeoutException } -> "timeout"
            chain.any { it is UnknownHostException } -> "dns"
            chain.any { it is SSLException } -> "tls"
            chain.any { it is ConnectException } -> "connection"
            else -> "transport"
        }
        val safeMessage = chain.asSequence()
            .mapNotNull { it.message }
            .map { sanitizeExceptionMessage(it) }
            .firstOrNull { it != "message_redacted" }
            ?: "message_redacted"
        return "exception_chain=$classes failure=$failure message=$safeMessage"
    }

    /** Maps recognizable transport text to fixed phrases; raw exception text may contain secrets. */
    private fun sanitizeExceptionMessage(message: String): String {
        val normalized = message.lowercase(Locale.ROOT)
        return when {
            normalized.contains("cleartext") &&
                (normalized.contains("network security policy") || normalized.contains("not permitted")) ->
                "cleartext blocked by Android network security policy"
            normalized.contains("timed out") || normalized.contains("timeout") -> "connection timed out"
            normalized.contains("unable to resolve host") || normalized.contains("unknown host") -> "host resolution failed"
            normalized.contains("connection refused") -> "connection refused"
            normalized.contains("connection reset") -> "connection reset"
            normalized.contains("failed to connect") -> "connection failed"
            normalized.contains("handshake") -> "TLS handshake failed"
            normalized.contains("certificate") && normalized.contains("trust") -> "TLS certificate validation failed"
            else -> "message_redacted"
        }
    }

    private fun hasCleartextPolicyCause(cause: Throwable): Boolean = causeChain(cause)
        .mapNotNull { it.message }
        .any { message ->
            val normalized = message.lowercase(Locale.ROOT)
            normalized.contains("cleartext") &&
                (normalized.contains("network security policy") || normalized.contains("not permitted"))
        }

    private fun causeChain(cause: Throwable): List<Throwable> {
        val result = mutableListOf<Throwable>()
        val seen = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<Throwable, Boolean>())
        var current: Throwable? = cause
        while (current != null && seen.add(current)) {
            result += current
            current = current.cause
        }
        return result
    }
}

class JellyfinHttpException(
    val endpoint: String,
    val statusCode: Int,
    authentication: Boolean
) : IOException(JellyfinHttpErrors.message(statusCode, authentication))

class JellyfinRequestException(
    val endpoint: String,
    val statusCode: Int?,
    cause: Throwable
) : IOException(JellyfinHttpErrors.requestMessage(endpoint, cause), cause)
