package com.audiobox.player.data.jellyfin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class JellyfinServerUrlTest {
    @Test fun defaultsToHttpsAndKeepsReverseProxyPath() {
        assertEquals("https://media.example/jellyfin", JellyfinServerUrl.normalize(" media.example/jellyfin/ "))
    }

    @Test fun preservesExplicitHttpForLocalServers() {
        assertEquals("http://192.168.1.2:8096", JellyfinServerUrl.normalize("http://192.168.1.2:8096/"))
        assertEquals("http://audiobox.tailnet-123.ts.net:8096", JellyfinServerUrl.normalize("http://audiobox.tailnet-123.ts.net:8096"))
    }

    @Test fun rejectsCredentialsAndQueryFromAddress() {
        assertThrows(IllegalArgumentException::class.java) { JellyfinServerUrl.normalize("https://user:pass@example.com") }
        assertThrows(IllegalArgumentException::class.java) { JellyfinServerUrl.normalize("https://example.com?api_key=secret") }
    }

    @Test fun createsExistingJellyfinAudioStreamEndpointWithoutTokenInUrl() {
        assertEquals("https://media.example/jellyfin/Audio/item123/stream?static=true", JellyfinServerUrl.audioStreamUrl("media.example/jellyfin", "item123"))
        assertEquals("audiobox-jellyfin://audio/item123", JellyfinServerUrl.playbackUri("item123"))
    }
}
