package com.example.fliztv.data

object FavoritesRepository {
    @Volatile private var snapshot: Set<String> = emptySet()
    private var onChanged: ((Set<String>) -> Unit)? = null
    @Volatile var version: Int = 0
        private set

    fun setOnChangedListener(listener: (Set<String>) -> Unit) {
        synchronized(this) { onChanged = listener }
    }

    fun loadFavorites(ids: Set<String>) {
        synchronized(this) {
            snapshot = ids.toSet()
            version++
        }
    }

    fun getFavorites(): List<String> = synchronized(this) { snapshot.toList() }

    fun isFavorite(channelId: String): Boolean = synchronized(this) { channelId in snapshot }

    fun toggleFavorite(channelId: String) {
        synchronized(this) {
            val current = snapshot
            val newSet = if (channelId in current) {
                current - channelId
            } else {
                current + channelId
            }
            snapshot = newSet
            version++
            onChanged?.invoke(newSet)
        }
    }

    fun addFavorite(channelId: String) {
        synchronized(this) {
            val newSet = snapshot + channelId
            snapshot = newSet
            version++
            onChanged?.invoke(newSet)
        }
    }

    fun removeFavorite(channelId: String) {
        synchronized(this) {
            val newSet = snapshot - channelId
            snapshot = newSet
            version++
            onChanged?.invoke(newSet)
        }
    }

    fun clear() {
        synchronized(this) {
            snapshot = emptySet()
            version++
            onChanged?.invoke(emptySet())
        }
    }

    val size: Int get() = synchronized(this) { snapshot.size }
}
