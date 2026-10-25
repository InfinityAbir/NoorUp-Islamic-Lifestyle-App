package com.example

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ManifestSecurityTest {
    private val manifest = File("src/main/AndroidManifest.xml").readText()

    @Test
    fun `app backup and cleartext traffic are disabled`() {
        assertTrue(manifest.contains("android:allowBackup=\"false\""))
        assertTrue(manifest.contains("android:usesCleartextTraffic=\"false\""))
    }

    @Test
    fun `boot receiver is not exported`() {
        val bootReceiver = manifest.substringAfter("android:name=\".ui.noorup.BootReceiver\"")
            .substringBefore("</receiver>")

        assertTrue(bootReceiver.contains("android:exported=\"false\""))
        assertFalse(bootReceiver.contains("android:exported=\"true\""))
    }
}
