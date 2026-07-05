package com.example.fliztv.data

object XmltvParser {

    fun parse(xml: String): List<EpgProgram> {
        val programs = mutableListOf<EpgProgram>()

        val programmeRegex = Regex(
            """<programme\s+channel="([^"]*)"\s+start="([^"]*)"\s+stop="([^"]*)">(.*?)</programme>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )

        programmeRegex.findAll(xml).forEach { match ->
            val channelId = match.groupValues[1]
            val startStr = match.groupValues[2]
            val stopStr = match.groupValues[3]
            val inner = match.groupValues[4]

            val startTime = parseXmltvTime(startStr) ?: return@forEach
            val endTime = parseXmltvTime(stopStr) ?: return@forEach

            val title = extractTag(inner, "title") ?: "Unknown"
            val description = extractTag(inner, "desc") ?: ""
            val category = extractTag(inner, "category") ?: ""
            val episodeNum = extractTag(inner, "episode-num") ?: ""
            val subTitle = extractTag(inner, "sub-title") ?: ""

            var season = 0
            var episode = 0
            if (episodeNum.isNotBlank()) {
                val parts = episodeNum.split(".")
                if (parts.size >= 2) {
                    season = parts[0].toIntOrNull() ?: 0
                    episode = parts[1].toIntOrNull() ?: 0
                }
            }

            programs.add(
                EpgProgram(
                    channelId = channelId,
                    title = title,
                    startTime = startTime,
                    endTime = endTime,
                    description = description,
                    category = category,
                    episodeTitle = subTitle,
                    season = season,
                    episode = episode
                )
            )
        }

        return programs
    }

    private fun parseXmltvTime(time: String): Long? {
        val cleaned = time.substringBefore(" ").trim()
        if (cleaned.length < 14) return null
        try {
            val year = cleaned.substring(0, 4).toInt()
            val month = cleaned.substring(4, 6).toInt()
            val day = cleaned.substring(6, 8).toInt()
            val hour = cleaned.substring(8, 10).toInt()
            val min = cleaned.substring(10, 12).toInt()
            val sec = cleaned.substring(12, 14).toInt()
            return epochMillis(year, month, day, hour, min, sec)
        } catch (_: Exception) {
            return null
        }
    }

    private fun epochMillis(year: Int, month: Int, day: Int, hour: Int, min: Int, sec: Int): Long {
        var y = year
        var m = month
        if (m <= 2) { y--; m += 12 }
        val era = (y / 400) * 146097L
        val yoe = (y % 400).toLong()
        val doy = (153L * (m - 3) + 2) / 5 + day - 1
        var d = era + yoe * 365 + yoe / 4 - yoe / 100 + doy
        d -= 719468L
        return (d * 86400L + hour * 3600L + min * 60L + sec) * 1000L
    }

    private fun extractTag(xml: String, tag: String): String? {
        val regex = Regex(
            """<$tag[^>]*>(.*?)</$tag>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )
        return regex.find(xml)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
    }
}
