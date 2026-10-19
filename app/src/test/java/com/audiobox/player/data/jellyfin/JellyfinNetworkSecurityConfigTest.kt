package com.audiobox.player.data.jellyfin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class JellyfinNetworkSecurityConfigTest {
    @Test fun permitsHttpOnlyForTheConfiguredJellyfinHostname() {
        val config = parseXml("src/main/res/xml/network_security_config.xml")
        val baseConfig = config.getElementsByTagName("base-config").item(0).attributes
        val domainConfigs = config.getElementsByTagName("domain-config")

        assertEquals("false", baseConfig.getNamedItem("cleartextTrafficPermitted").nodeValue)
        assertEquals(1, domainConfigs.length)
        val jellyfinConfig = domainConfigs.item(0)
        assertEquals("true", jellyfinConfig.attributes.getNamedItem("cleartextTrafficPermitted").nodeValue)

        val domains = jellyfinConfig.childNodes
        val domain = (0 until domains.length)
            .map { domains.item(it) }
            .first { it.nodeName == "domain" }
        assertEquals("nabilnas.tailc6424e.ts.net", domain.textContent.trim())
        assertEquals("false", domain.attributes.getNamedItem("includeSubdomains").nodeValue)
    }

    @Test fun manifestReferencesTheNetworkSecurityConfig() {
        val manifest = parseXml("src/main/AndroidManifest.xml")
        val application = manifest.getElementsByTagName("application").item(0)
        val attribute = application.attributes.getNamedItemNS(
            "http://schemas.android.com/apk/res/android",
            "networkSecurityConfig"
        )
        assertTrue(attribute != null)
        assertEquals("@xml/network_security_config", attribute.nodeValue)
    }

    private fun parseXml(path: String) = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        setFeature("http://xml.org/sax/features/external-general-entities", false)
        setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    }.newDocumentBuilder().parse(File(path))
}
