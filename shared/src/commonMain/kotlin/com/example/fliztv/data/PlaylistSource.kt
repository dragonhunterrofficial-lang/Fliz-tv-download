package com.example.fliztv.data

data class PlaylistSource(
    val id: String,
    val name: String,
    val url: String,
    val isDefault: Boolean = false
)
