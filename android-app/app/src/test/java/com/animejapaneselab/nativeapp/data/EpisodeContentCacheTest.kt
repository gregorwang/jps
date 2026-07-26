package com.animejapaneselab.nativeapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EpisodeContentCacheTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun cache(): EpisodeContentCache = EpisodeContentCache(temporaryFolder.root)

    @Test
    fun writeThenReadReturnsSameBody() {
        val cache = cache()
        val path = "/api/works/k-on/episodes/1/vocab"
        cache.write(path, """[{"id":"v1","surface":"部活"}]""")
        assertEquals("""[{"id":"v1","surface":"部活"}]""", cache.read(path))
    }

    @Test
    fun differentQueriesAreDifferentEntries() {
        val cache = cache()
        cache.write("/api/linguistic-exercises?workSlug=k-on&episode=1", "[1]")
        cache.write("/api/linguistic-exercises?workSlug=k-on&episode=2", "[2]")
        assertEquals("[1]", cache.read("/api/linguistic-exercises?workSlug=k-on&episode=1"))
        assertEquals("[2]", cache.read("/api/linguistic-exercises?workSlug=k-on&episode=2"))
    }

    @Test
    fun missingEntryReadsNull() {
        assertNull(cache().read("/api/works/k-on/episodes/9/vocab"))
    }

    @Test
    fun blankBodyIsNotStored() {
        val cache = cache()
        cache.write("/api/works", "   ")
        assertNull(cache.read("/api/works"))
    }

    @Test
    fun overwriteReplacesPreviousBody() {
        val cache = cache()
        cache.write("/api/works", "[]")
        cache.write("/api/works", """[{"slug":"k-on"}]""")
        assertEquals("""[{"slug":"k-on"}]""", cache.read("/api/works"))
    }

    @Test
    fun cacheablePathsCoverContentButNotLiveEndpoints() {
        assertTrue(isCacheableContentPath("/api/works"))
        assertTrue(isCacheableContentPath("/api/works/k-on/episodes"))
        assertTrue(isCacheableContentPath("/api/works/re-zero/episodes/3/sentences"))
        assertTrue(isCacheableContentPath("/api/linguistic-exercises?workSlug=k-on&episode=1"))
        assertFalse(isCacheableContentPath("/api/progress"))
        assertFalse(isCacheableContentPath("/api/review/today"))
        assertFalse(isCacheableContentPath("/api/history?deviceId=x"))
        assertFalse(isCacheableContentPath("/api/ai/explain"))
        assertFalse(isCacheableContentPath("/api/rag/search"))
        assertFalse(isCacheableContentPath("/api/auth/me"))
    }
}
