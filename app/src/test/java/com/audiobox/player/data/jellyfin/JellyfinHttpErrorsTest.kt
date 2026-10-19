package com.audiobox.player.data.jellyfin

import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException
import java.net.UnknownServiceException

class JellyfinHttpErrorsTest {
    @Test fun mapsExpiredSessionAndUnavailableMediaWithoutSecrets() {
        val expired = JellyfinHttpErrors.message(401)
        assertTrue(expired.contains("session expired", ignoreCase = true))
        assertTrue(JellyfinHttpErrors.message(404).contains("no longer available"))
    }

    @Test fun authenticationFailureIsClearAndServerErrorsAreRetryable() {
        assertEquals("Invalid username or password.", JellyfinHttpErrors.message(401, authentication = true))
        assertTrue(JellyfinHttpErrors.message(503).contains("Try again later"))
    }

    @Test fun serverFailureKeepsItsStatusInsteadOfLookingLikeBadCredentials() {
        val error = JellyfinHttpException("/Users/AuthenticateByName", 500, authentication = true)
        assertEquals(500, error.statusCode)
        assertTrue(error.message.orEmpty().contains("HTTP 500"))
        assertTrue(!error.message.orEmpty().contains("username or password", ignoreCase = true))
    }

    @Test fun identifiesAndroidCleartextPolicyFailureWithoutLeakingEndpointOrCauseText() {
        val endpoint = "http://private-tailnet-host:8096/Users/AuthenticateByName"
        val cause = IOException(
            "outer message with private-tailnet-host",
            UnknownServiceException("CLEARTEXT communication to private-tailnet-host not permitted by network security policy")
        )
        val error = JellyfinRequestException(endpoint, null, cause)

        assertTrue(error.message.orEmpty().contains("Android blocked this HTTP connection"))
        assertTrue(!error.message.orEmpty().contains("private-tailnet-host"))
        assertEquals("IOException", error.cause?.javaClass?.simpleName.orEmpty())
        assertNull(error.statusCode)
        val diagnostic = JellyfinHttpErrors.diagnostic(cause)
        assertTrue(diagnostic.contains("exception_chain=IOException>UnknownServiceException"))
        assertTrue(diagnostic.contains("failure=cleartext_policy"))
        assertTrue(diagnostic.contains("message=cleartext blocked by Android network security policy"))
        assertTrue(!diagnostic.contains("private-tailnet-host"))
        assertTrue(!diagnostic.contains("CLEARTEXT"))
    }

    @Test fun classifiesOtherTransportFailuresWithoutLoggingTheirMessages() {
        val cause = java.net.SocketTimeoutException("timed out contacting private.example")
        val diagnostic = JellyfinHttpErrors.diagnostic(cause)
        assertTrue(diagnostic.contains("exception_chain=SocketTimeoutException"))
        assertTrue(diagnostic.contains("failure=timeout"))
        assertTrue(diagnostic.contains("message=connection timed out"))
        assertTrue(!diagnostic.contains("private.example"))
    }

    @Test fun keepsGenericMessageForOtherTransportFailures() {
        val error = JellyfinRequestException(
            "https://private.example/Users/AuthenticateByName",
            null,
            java.net.SocketTimeoutException("request timed out")
        )
        assertEquals("Could not complete Jellyfin request. Check server and network diagnostics.", error.message.orEmpty())
    }

    private fun assertEquals(expected: String, actual: String) = org.junit.Assert.assertEquals(expected, actual)
    private fun assertEquals(expected: Int, actual: Int) = org.junit.Assert.assertEquals(expected, actual)
}
