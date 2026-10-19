package com.audiobox.player.data.jellyfin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JellyfinAuthorizationHeadersTest {
    @Test fun sendsModernAuthorizationHeaderWithoutLegacyFallbackHeader() {
        val headers = JellyfinApiClient("device-123").authorizationHeaders()
        val value = headers.getValue("Authorization")

        assertEquals(setOf("Authorization"), headers.keys)
        assertTrue(value.startsWith("MediaBrowser "))
        assertTrue(value.contains("Client=\"AudioBoX\""))
        assertTrue(value.contains("DeviceId=\"device-123\""))
        assertTrue(!value.contains("Token="))
    }

    @Test fun includesAccessTokenOnlyForAuthenticatedRequests() {
        val value = JellyfinApiClient("device-123").authorizationHeaders("fixture-token").getValue("Authorization")
        assertTrue(value.contains("Token=\"fixture-token\""))
    }
}
