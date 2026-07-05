package com.example.fliztv

import com.example.fliztv.data.Channel
import com.example.fliztv.data.EpgProgram
import com.example.fliztv.data.EpgSource
import com.example.fliztv.data.PlaylistSource
import com.example.fliztv.data.XmltvParser
import com.example.fliztv.data.detectQuality
import com.example.fliztv.data.parseM3U
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedCommonTest {

    @Test
    fun testM3uParsing() {
        val m3u = """
#EXTM3U
#EXTINF:-1 tvg-id="test.in" tvg-name="Test Channel" tvg-logo="http://logo.com/logo.png" group-title="Entertainment",Test Channel HD
http://stream.com/test.m3u8
        """.trimIndent()
        val channels = parseM3U(m3u)
        assertEquals(1, channels.size)
        val ch = channels[0]
        assertEquals("test.in", ch.id)
        assertEquals("Test Channel HD", ch.name)
        assertEquals("http://stream.com/test.m3u8", ch.url)
        assertEquals("http://logo.com/logo.png", ch.logo)
        assertEquals("India", ch.country)
        assertEquals("Entertainment", ch.category)
    }

    @Test
    fun testM3uParsingWithUserAgent() {
        val m3u = """
#EXTM3U
#EXTINF:-1 tvg-id="test.us" tvg-name="US News" group-title="News",US News HD
#EXTVLCOPT:http-user-agent=Mozilla/5.0
#EXTVLCOPT:http-referrer=https://example.com
http://stream.com/news.m3u8
        """.trimIndent()
        val channels = parseM3U(m3u)
        assertEquals(1, channels.size)
        assertEquals("Mozilla/5.0", channels[0].userAgent)
        assertEquals("https://example.com", channels[0].referer)
    }

    @Test
    fun testM3uDuplicateRemoval() {
        val m3u = """
#EXTM3U
#EXTINF:-1 tvg-id="dup1" tvg-name="Dup Channel" group-title="Movies",Dup Channel
http://stream.com/dup.m3u8
#EXTINF:-1 tvg-id="dup2" tvg-name="Dup Channel" group-title="Movies",Dup Channel 2
http://stream.com/dup.m3u8
        """.trimIndent()
        val channels = parseM3U(m3u)
        assertEquals(1, channels.size)
    }

    @Test
    fun testQualityDetection() {
        assertEquals("4k", detectQuality("Test 4K Channel", "http://stream.com/test"))
        assertEquals("fhd", detectQuality("Test FHD Channel", "http://stream.com/test"))
        assertEquals("hd", detectQuality("Test HD Channel", "http://stream.com/test"))
        assertEquals("sd", detectQuality("Test SD Channel", "http://stream.com/test"))
        assertEquals("unknown", detectQuality("Test Channel", "http://stream.com/test"))
    }

    @Test
    fun testEpgProgramNow() {
        val program = EpgProgram(
            channelId = "test.in",
            title = "Test Show",
            startTime = 1000,
            endTime = 2000,
            description = "A test program"
        )
        assertTrue(program.startTime < program.endTime)
    }

    @Test
    fun testEpgProgramProgressCalculated() {
        val program = EpgProgram(
            channelId = "test.in",
            title = "Progress Test",
            startTime = 1000,
            endTime = 2000
        )
        assertEquals(1000, program.endTime - program.startTime)
    }

    @Test
    fun testXmltvParsing() {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
<tv>
  <programme channel="test.in" start="20250101000000 +0000" stop="20250101010000 +0000">
    <title>Test Program</title>
    <desc>Test description</desc>
    <category>Entertainment</category>
  </programme>
</tv>"""
        val programs = XmltvParser.parse(xml)
        assertEquals(1, programs.size)
        assertEquals("test.in", programs[0].channelId)
        assertEquals("Test Program", programs[0].title)
        assertEquals("Test description", programs[0].description)
        assertEquals("Entertainment", programs[0].category)
    }

    @Test
    fun testXmltvParsingMalformed() {
        val xml = """<?xml version="1.0"?><tv></tv>"""
        val programs = XmltvParser.parse(xml)
        assertTrue(programs.isEmpty())
    }

    @Test
    fun testChannelDefaultValues() {
        val channel = Channel(
            id = "test.in",
            name = "Test",
            url = "http://test.com/stream",
            logo = "",
            country = "India",
            category = "",
            language = ""
        )
        assertEquals("", channel.userAgent)
        assertEquals("", channel.referer)
        assertEquals("", channel.status)
        assertEquals("unknown", channel.quality)
        assertFalse(channel.isSd)
    }

    @Test
    fun testPlaylistSourceDataClass() {
        val source = PlaylistSource("test", "Test Source", "http://test.com/playlist.m3u", isDefault = true)
        assertEquals("test", source.id)
        assertEquals("Test Source", source.name)
        assertEquals("http://test.com/playlist.m3u", source.url)
        assertTrue(source.isDefault)
    }

    @Test
    fun testEpgSourceDataClass() {
        val source = EpgSource("test-epg", "Test EPG", "http://test.com/epg.xml", isDefault = false)
        assertEquals("test-epg", source.id)
        assertEquals("Test EPG", source.name)
        assertFalse(source.isDefault)
    }

    @Test
    fun testEpgProgramProgressEdgeCases() {
        val program = EpgProgram(
            channelId = "test.in",
            title = "Edge Test",
            startTime = 1000,
            endTime = 1000
        )
        assertEquals(0f, program.progress)
    }

    @Test
    fun testPlaylistSourceDefault() {
        val source = PlaylistSource("test", "Test", "http://test.com", isDefault = true)
        assertTrue(source.isDefault)
    }
}
