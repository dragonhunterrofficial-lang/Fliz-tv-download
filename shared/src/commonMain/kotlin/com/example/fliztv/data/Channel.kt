package com.example.fliztv.data

data class Channel(
    val id: String,
    val name: String,
    val url: String,
    val logo: String,
    val country: String,
    val category: String,
    val language: String,
    val userAgent: String = "",
    val referer: String = "",
    val status: String = "",
    val quality: String = "unknown",
    val tvgUrl: String = ""
) {
    val isSd: Boolean get() = quality == "sd"
}

fun detectQuality(name: String, url: String): String {
    val lowerName = name.lowercase()
    val lowerUrl = url.lowercase()
    val combined = "$lowerName $lowerUrl"
    return when {
        combined.contains("2160p") || combined.contains("4k") || combined.contains("uhd") -> "4k"
        combined.contains("1080p") || combined.contains("fhd") || combined.contains("full hd") -> "fhd"
        combined.contains("720p") || combined.contains("hd") -> "hd"
        combined.contains("576p") || combined.contains("480p") || combined.contains("360p") || combined.contains("240p") || combined.contains("sd") -> "sd"
        else -> "unknown"
    }
}
