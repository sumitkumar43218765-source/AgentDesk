package com.agentdesk.app.feature.build

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.build.data.parse.ManifestReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ManifestReaderTest {

    private lateinit var reader: ManifestReader

    @Before
    fun setup() {
        reader = ManifestReader()
    }

    @Test
    fun testParseValidManifest() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="com.example.hello"
                android:versionCode="2"
                android:versionName="1.1">
                <uses-sdk android:minSdkVersion="24" android:targetSdkVersion="34" />
                <application android:label="HelloApp" />
            </manifest>
        """.trimIndent()

        val result = reader.readFromString(xml)
        assertTrue(result is AppResult.Success)
        val info = (result as AppResult.Success).value
        assertEquals("com.example.hello", info.packageName)
        assertEquals(24, info.minSdkVersion)
        assertEquals(34, info.targetSdkVersion)
        assertEquals(2, info.versionCode)
        assertEquals("1.1", info.versionName)
    }

    @Test
    fun testMissingPackageReturnsFailure() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <application />
            </manifest>
        """.trimIndent()

        val result = reader.readFromString(xml)
        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun testMalformedXmlReturnsFailure() {
        val xml = "<manifest package='com.test'><unclosed"
        val result = reader.readFromString(xml)
        assertTrue(result is AppResult.Failure)
    }
}
