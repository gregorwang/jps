package com.animejapaneselab.nativeapp.data

import android.content.res.AssetManager

/**
 * Offline snapshot of the read-only content endpoints (works, episodes, vocab, grammar,
 * sentences, exercises, plan, subtitles, 活用 items, foundation): `assets/content/<sha1(path)>.json`,
 * written by `scripts/build-content-pack.py`. [RemoteLabClient.get] serves these without the
 * network; a path the pack lacks still goes to the worker.
 */
object ContentPack {
    @Volatile private var assets: AssetManager? = null
    @Volatile private var names: Set<String> = emptySet()

    fun init(assets: AssetManager) {
        this.assets = assets
        names = runCatching { assets.list("content")?.toSet() }.getOrNull().orEmpty()
    }

    fun read(path: String): String? {
        val manager = assets ?: return null
        val name = EpisodeContentCache.sha1(path) + ".json"
        if (name !in names) return null
        return runCatching { manager.open("content/$name").use { it.readBytes().toString(Charsets.UTF_8) } }
            .getOrNull()?.ifBlank { null }
    }
}
