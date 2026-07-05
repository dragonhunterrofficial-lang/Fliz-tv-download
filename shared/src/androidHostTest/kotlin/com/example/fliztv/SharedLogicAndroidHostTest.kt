package com.example.fliztv

import com.example.fliztv.data.ChannelRepository
import com.example.fliztv.data.FavoritesRepository
import com.example.fliztv.data.RecordingManager
import com.example.fliztv.data.Recording
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
class SharedLogicAndroidHostTest {

    @Test
    fun testFavoritesAddAndRemove() {
        FavoritesRepository.loadFavorites(emptySet())
        assertTrue(FavoritesRepository.getFavorites().isEmpty())

        FavoritesRepository.addFavorite("channel1")
        assertTrue(FavoritesRepository.isFavorite("channel1"))
        assertEquals(1, FavoritesRepository.size)

        FavoritesRepository.addFavorite("channel2")
        assertEquals(2, FavoritesRepository.size)

        FavoritesRepository.removeFavorite("channel1")
        assertFalse(FavoritesRepository.isFavorite("channel1"))
        assertEquals(1, FavoritesRepository.size)
    }

    @Test
    fun testFavoritesToggle() {
        FavoritesRepository.loadFavorites(emptySet())

        FavoritesRepository.toggleFavorite("ch1")
        assertTrue(FavoritesRepository.isFavorite("ch1"))

        FavoritesRepository.toggleFavorite("ch1")
        assertFalse(FavoritesRepository.isFavorite("ch1"))
    }

    @Test
    fun testFavoritesClear() {
        FavoritesRepository.loadFavorites(setOf("a", "b", "c"))
        FavoritesRepository.clear()
        assertTrue(FavoritesRepository.getFavorites().isEmpty())
        assertEquals(0, FavoritesRepository.size)
    }

    @Test
    fun testFavoritesVersionIncrement() {
        FavoritesRepository.loadFavorites(emptySet())
        val v1 = FavoritesRepository.version
        FavoritesRepository.addFavorite("x")
        assertEquals(v1 + 1, FavoritesRepository.version)
        FavoritesRepository.removeFavorite("x")
        assertEquals(v1 + 2, FavoritesRepository.version)
    }

    @Test
    fun testRecordingLifecycle() {
        RecordingManager.clearAll()
        assertTrue(RecordingManager.getRecordings().isEmpty())

        val rec = RecordingManager.startRecording("Test Channel", "test.in", "http://stream.com/test.m3u8")
        assertNotNull(rec.id)
        assertTrue(rec.isDownloading)
        assertFalse(rec.isComplete)
        assertTrue(RecordingManager.isRecording("test.in"))

        RecordingManager.completeRecording(rec.id, "/path/to/file.ts", 1024000)
        val recordings = RecordingManager.getRecordings()
        assertEquals(1, recordings.size)
        assertTrue(recordings[0].isComplete)
        assertFalse(recordings[0].isDownloading)
        assertEquals(1024000, recordings[0].fileSizeBytes)
    }

    @Test
    fun testRecordingFail() {
        RecordingManager.clearAll()
        val rec = RecordingManager.startRecording("Fail Test", "fail.in", "http://fail.com/stream")
        RecordingManager.failRecording(rec.id)
        assertTrue(RecordingManager.getRecordings().isEmpty())
    }

    @Test
    fun testRecordingDelete() {
        RecordingManager.clearAll()
        val rec = RecordingManager.startRecording("Del Test", "del.in", "http://del.com/stream")
        RecordingManager.deleteRecording(rec.id)
        assertTrue(RecordingManager.getRecordings().isEmpty())
    }

    @Test
    fun testRecordingClearAll() {
        RecordingManager.startRecording("Ch1", "ch1", "http://ch1.com")
        RecordingManager.startRecording("Ch2", "ch2", "http://ch2.com")
        RecordingManager.clearAll()
        assertTrue(RecordingManager.getRecordings().isEmpty())
    }

    @Test
    fun testPlaylistSourcePersistence() {
        val sourcesBefore = com.example.fliztv.data.PlaylistManager.getSources()
        val count = sourcesBefore.size

        val newSource = com.example.fliztv.data.PlaylistManager.addSource("Custom Test", "http://custom.com/playlist.m3u")
        assertTrue(newSource.id.startsWith("custom_"))
    }

    @Test
    fun testChannelsEmpty() = runBlocking {
        val channels = ChannelRepository.getAllChannels(forceRefresh = true)
        // ChannelRepository returns empty when there's no cached data and no network
        // This just verifies it doesn't crash
        assertNotNull(channels)
    }
}
