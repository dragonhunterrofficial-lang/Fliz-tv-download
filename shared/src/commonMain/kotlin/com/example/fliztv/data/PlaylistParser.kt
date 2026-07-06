package com.example.fliztv.data

fun parseM3U(content: String): List<Channel> {
    val channels = mutableListOf<Channel>()
    val seenUrls = mutableSetOf<String>()
    var currentExtinf: String? = null
    var pendingUserAgent = ""
    var pendingReferer = ""

    content.lineSequence().forEach { line ->
        val trimmed = line.trim()
        when {
            trimmed.startsWith("#EXTINF:") -> {
                currentExtinf = trimmed
            }
            trimmed.startsWith("#EXTVLCOPT:http-user-agent=") -> {
                pendingUserAgent = trimmed.substringAfter("=")
            }
            trimmed.startsWith("#EXTVLCOPT:http-referrer=") -> {
                pendingReferer = trimmed.substringAfter("=")
            }
            (trimmed.startsWith("http://") || trimmed.startsWith("https://")) && currentExtinf != null -> {
                if (trimmed !in seenUrls) {
                    seenUrls.add(trimmed)
                    channels.add(parseExtinf(currentExtinf, trimmed, pendingUserAgent, pendingReferer))
                }
                currentExtinf = null
                pendingUserAgent = ""
                pendingReferer = ""
            }
        }
    }

    return channels
}

fun extractHeaderTvgUrl(content: String): String {
    val firstLine = content.lineSequence().firstOrNull()?.trim() ?: return ""
    if (!firstLine.startsWith("#EXTM3U")) return ""
    return extractQuotedAttr(firstLine, "url-tvg")
        ?: extractQuotedAttr(firstLine, "x-tvg-url")
        ?: ""
}

private fun parseExtinf(extinf: String, url: String, pendingUserAgent: String, pendingReferer: String): Channel {
    val id = extinf.substringAfter("tvg-id=\"").substringBefore("\"")
    val nameAttr = extinf.substringAfter("tvg-name=\"").substringBefore("\"")
    val logo = extinf.substringAfter("tvg-logo=\"").substringBefore("\"")
    val category = extinf.substringAfter("group-title=\"").substringBefore("\"")
    val displayName = extinf.substringAfterLast(",").trim()
    val tvgLanguage = extractQuotedAttr(extinf, "tvg-language") ?: ""
    val tvgUrl = extractQuotedAttr(extinf, "tvg-url") ?: ""

    val userAgent = extractQuotedAttr(extinf, "http-user-agent") ?: pendingUserAgent.ifEmpty { "" }
    val referer = extractQuotedAttr(extinf, "http-referrer") ?: pendingReferer.ifEmpty { "" }

    val cleanName = displayName.ifEmpty { nameAttr }

    val status = when {
        "[Not 24/7]" in cleanName -> "Not 24/7"
        else -> ""
    }

    val country = extractCountryFromId(id, extinf)
    val language = inferLanguage(cleanName, tvgLanguage, country, category)

    return Channel(
        id = id,
        name = cleanName,
        url = url,
        logo = logo,
        country = country,
        category = category,
        language = language,
        userAgent = userAgent,
        referer = referer,
        status = status,
        quality = detectQuality(cleanName, url),
        tvgUrl = tvgUrl
    )
}

private fun extractQuotedAttr(extinf: String, attr: String): String? {
    val search = "$attr=\""
    val start = extinf.indexOf(search)
    if (start == -1) return null
    val valueStart = start + search.length
    val end = extinf.indexOf("\"", valueStart)
    if (end == -1) return null
    return extinf.substring(valueStart, end)
}

private fun extractCountryFromId(tvgId: String, extinf: String = ""): String {
    val directCountry = extractQuotedAttr(extinf, "tvg-country")
    if (!directCountry.isNullOrBlank()) {
        val code = directCountry.trim().lowercase()
        val mapped = COUNTRY_NAMES[code]
        if (mapped != null) return mapped
    }
    val code = extractCountryCode(tvgId)
    if (!code.isNullOrBlank()) return countryCodeToName(code)
    return ""
}

private fun extractCountryCode(tvgId: String): String? {
    val match = COUNTRY_CODE_REGEX.find(tvgId)
    return match?.groupValues?.get(1)?.lowercase()
}

private val COUNTRY_CODE_REGEX = Regex("\\.([a-z]{2})(@|$)")

private fun countryCodeToName(code: String): String {
    return COUNTRY_NAMES[code] ?: code.uppercase()
}

private val COUNTRY_NAMES = mapOf(
    "ad" to "Andorra", "ae" to "UAE", "af" to "Afghanistan", "ag" to "Antigua", "ai" to "Anguilla",
    "al" to "Albania", "am" to "Armenia", "ao" to "Angola", "aq" to "Antarctica", "ar" to "Argentina",
    "as" to "Samoa", "at" to "Austria", "au" to "Australia", "aw" to "Aruba", "ax" to "Aland",
    "az" to "Azerbaijan", "ba" to "Bosnia", "bb" to "Barbados", "bd" to "Bangladesh", "be" to "Belgium",
    "bf" to "Burkina Faso", "bg" to "Bulgaria", "bh" to "Bahrain", "bi" to "Burundi", "bj" to "Benin",
    "bl" to "St. Barths", "bm" to "Bermuda", "bn" to "Brunei", "bo" to "Bolivia", "bq" to "Caribbean NL",
    "br" to "Brazil", "bs" to "Bahamas", "bt" to "Bhutan", "bw" to "Botswana", "by" to "Belarus",
    "bz" to "Belize", "ca" to "Canada", "cc" to "Cocos", "cd" to "DR Congo", "cf" to "CAR",
    "cg" to "Congo", "ch" to "Switzerland", "ci" to "Ivory Coast", "ck" to "Cook Islands",
    "cl" to "Chile", "cm" to "Cameroon", "cn" to "China", "co" to "Colombia", "cr" to "Costa Rica",
    "cu" to "Cuba", "cv" to "Cape Verde", "cw" to "Curacao", "cx" to "Christmas Island",
    "cy" to "Cyprus", "cz" to "Czechia", "de" to "Germany", "dj" to "Djibouti", "dk" to "Denmark",
    "dm" to "Dominica", "do" to "Dominican Rep.", "dz" to "Algeria", "ec" to "Ecuador",
    "ee" to "Estonia", "eg" to "Egypt", "eh" to "W. Sahara", "er" to "Eritrea", "es" to "Spain",
    "et" to "Ethiopia", "fi" to "Finland", "fj" to "Fiji", "fk" to "Falklands", "fm" to "Micronesia",
    "fo" to "Faroe Islands", "fr" to "France", "ga" to "Gabon", "gb" to "UK", "gd" to "Grenada",
    "ge" to "Georgia", "gf" to "French Guiana", "gg" to "Guernsey", "gh" to "Ghana", "gi" to "Gibraltar",
    "gl" to "Greenland", "gm" to "Gambia", "gn" to "Guinea", "gp" to "Guadeloupe", "gq" to "Equatorial Guinea",
    "gr" to "Greece", "gt" to "Guatemala", "gu" to "Guam", "gw" to "Guinea-Bissau", "gy" to "Guyana",
    "hk" to "Hong Kong", "hn" to "Honduras", "hr" to "Croatia", "ht" to "Haiti", "hu" to "Hungary",
    "id" to "Indonesia", "ie" to "Ireland", "il" to "Israel", "im" to "Isle of Man", "in" to "India",
    "io" to "BIOT", "iq" to "Iraq", "ir" to "Iran", "is" to "Iceland", "it" to "Italy",
    "je" to "Jersey", "jm" to "Jamaica", "jo" to "Jordan", "jp" to "Japan", "ke" to "Kenya",
    "kg" to "Kyrgyzstan", "kh" to "Cambodia", "ki" to "Kiribati", "km" to "Comoros",
    "kn" to "St. Kitts", "kp" to "North Korea", "kr" to "South Korea", "kw" to "Kuwait",
    "ky" to "Cayman Islands", "kz" to "Kazakhstan", "la" to "Laos", "lb" to "Lebanon",
    "lc" to "St. Lucia", "li" to "Liechtenstein", "lk" to "Sri Lanka", "lr" to "Liberia",
    "ls" to "Lesotho", "lt" to "Lithuania", "lu" to "Luxembourg", "lv" to "Latvia",
    "ly" to "Libya", "ma" to "Morocco", "mc" to "Monaco", "md" to "Moldova", "me" to "Montenegro",
    "mf" to "St. Martin", "mg" to "Madagascar", "mh" to "Marshall Islands", "mk" to "North Macedonia",
    "ml" to "Mali", "mm" to "Myanmar", "mn" to "Mongolia", "mo" to "Macau", "mp" to "N. Mariana Is.",
    "mq" to "Martinique", "mr" to "Mauritania", "ms" to "Montserrat", "mt" to "Malta",
    "mu" to "Mauritius", "mv" to "Maldives", "mw" to "Malawi", "mx" to "Mexico", "my" to "Malaysia",
    "mz" to "Mozambique", "na" to "Namibia", "nc" to "New Caledonia", "ne" to "Niger",
    "nf" to "Norfolk Island", "ng" to "Nigeria", "ni" to "Nicaragua", "nl" to "Netherlands",
    "no" to "Norway", "np" to "Nepal", "nr" to "Nauru", "nu" to "Niue", "nz" to "New Zealand",
    "om" to "Oman", "pa" to "Panama", "pe" to "Peru", "pf" to "French Polynesia", "pg" to "Papua New Guinea",
    "ph" to "Philippines", "pk" to "Pakistan", "pl" to "Poland", "pm" to "St. Pierre",
    "pn" to "Pitcairn", "pr" to "Puerto Rico", "ps" to "Palestine", "pt" to "Portugal",
    "pw" to "Palau", "py" to "Paraguay", "qa" to "Qatar", "re" to "Reunion", "ro" to "Romania",
    "rs" to "Serbia", "ru" to "Russia", "rw" to "Rwanda", "sa" to "Saudi Arabia",
    "sb" to "Solomon Islands", "sc" to "Seychelles", "sd" to "Sudan", "se" to "Sweden",
    "sg" to "Singapore", "sh" to "St. Helena", "si" to "Slovenia", "sk" to "Slovakia",
    "sl" to "Sierra Leone", "sm" to "San Marino", "sn" to "Senegal", "so" to "Somalia",
    "sr" to "Suriname", "ss" to "South Sudan", "st" to "Sao Tome", "sv" to "El Salvador",
    "sx" to "Sint Maarten", "sy" to "Syria", "sz" to "Eswatini", "tc" to "Turks & Caicos",
    "td" to "Chad", "tf" to "French S. Terr.", "tg" to "Togo", "th" to "Thailand",
    "tj" to "Tajikistan", "tk" to "Tokelau", "tl" to "Timor-Leste", "tm" to "Turkmenistan",
    "tn" to "Tunisia", "to" to "Tonga", "tr" to "Turkey", "tt" to "Trinidad & Tobago",
    "tv" to "Tuvalu", "tw" to "Taiwan", "tz" to "Tanzania", "ua" to "Ukraine",
    "ug" to "Uganda", "uk" to "United Kingdom", "us" to "United States", "uy" to "Uruguay",
    "uz" to "Uzbekistan", "va" to "Vatican", "vc" to "St. Vincent", "ve" to "Venezuela",
    "vg" to "British Virgin Is.", "vi" to "US Virgin Is.", "vn" to "Vietnam", "vu" to "Vanuatu",
    "wf" to "Wallis & Futuna", "ws" to "Samoa", "ye" to "Yemen", "yt" to "Mayotte",
    "za" to "South Africa", "zm" to "Zambia", "zw" to "Zimbabwe"
)

private val LANGUAGE_KEYWORD_PAIRS = listOf(
    "urdu news" to "Urdu", "urdu" to "Urdu", "dd urdu" to "Urdu",
    "assamese" to "Assamese", "asomiya" to "Assamese",
    "odia" to "Odia", "oriya" to "Odia", "orissa" to "Odia",
    "bhojpuri" to "Bhojpuri",
    "tamil" to "Tamil", "tamizh" to "Tamil", "tamilan" to "Tamil", "thamizh" to "Tamil",
    "telugu" to "Telugu", "tollywood" to "Telugu",
    "malayalam" to "Malayalam", "kairali" to "Malayalam", "manorama" to "Malayalam",
    "kannada" to "Kannada", "chandana" to "Kannada",
    "marathi" to "Marathi",
    "bengali" to "Bengali", "bangla" to "Bengali", "bengal" to "Bengali",
    "gujarati" to "Gujarati",
    "punjabi" to "Punjabi",

    "zee tamil" to "Tamil", "star vijay" to "Tamil", "sun tv" to "Tamil",
    "raj tv" to "Tamil", "kala tv" to "Tamil",
    "polimer" to "Tamil", "sangam" to "Tamil", "kalaignar" to "Tamil",
    "vendhar" to "Tamil", "murasu" to "Tamil", "jothi" to "Tamil",
    "dhinamalar" to "Tamil", "chutti" to "Tamil",
    "sirippoli" to "Tamil", "velicham" to "Tamil", "imayam" to "Tamil",
    "mega tv" to "Tamil", "makkal tv" to "Tamil", "pothigai" to "Tamil",
    "dd podhigai" to "Tamil", "raj digital" to "Tamil",
    "sathiyam" to "Tamil", "new tamil" to "Tamil", "7s tv" to "Tamil",

    "zee telugu" to "Telugu", "star ma" to "Telugu", "gemini tv" to "Telugu",
    "etv" to "Telugu", "tv9 telugu" to "Telugu", "t news" to "Telugu",
    "sakshi" to "Telugu", "v6 news" to "Telugu", "v9 news" to "Telugu",
    "abn" to "Telugu", "dd yadagiri" to "Telugu",
    "telugu news" to "Telugu", "telugu one" to "Telugu",
    "hmtv" to "Telugu", "urs tv" to "Telugu", "devi tv" to "Telugu",
    "bhavani tv" to "Telugu", "vasavi" to "Telugu",
    "vissa" to "Telugu", "toli" to "Telugu",

    "asianet" to "Malayalam", "kairali" to "Malayalam",
    "kaumudy" to "Malayalam", "zee keralam" to "Malayalam",
    "flowers tv" to "Malayalam", "amrita tv" to "Malayalam",
    "janam tv" to "Malayalam", "mathrubhumi" to "Malayalam",
    "darshana" to "Malayalam", "jaihind" to "Malayalam",
    "kerala tv" to "Malayalam", "mazhavil" to "Malayalam",
    "surya tv" to "Malayalam", "media one" to "Malayalam",
    "news18 kerala" to "Malayalam", "doordarshan kerala" to "Malayalam",
    "dd malayalam" to "Malayalam", "asianet news" to "Malayalam",
    "manorama news" to "Malayalam", "twenty four" to "Malayalam",
    "zee kerala" to "Malayalam",

    "zee kannada" to "Kannada", "star suvarna" to "Kannada",
    "colors kannada" to "Kannada", "tv9 kannada" to "Kannada",
    "public tv" to "Kannada", "public movies" to "Kannada", "public music" to "Kannada",
    "news18 kannada" to "Kannada",
    "kasturi" to "Kannada", "kasthuri" to "Kannada",
    "dd chandana" to "Kannada", "suvarna news" to "Kannada",
    "raj music kannada" to "Kannada",

    "zee marathi" to "Marathi", "colors marathi" to "Marathi",
    "star pravah" to "Marathi",
    "dd sahyadri" to "Marathi", "tv9 marathi" to "Marathi",
    "news18 lokmat" to "Marathi", "saam tv" to "Marathi",
    "abp majha" to "Marathi", "zee 24 tas" to "Marathi",
    "jaihind marathi" to "Marathi",

    "zee bangla" to "Bengali", "colors bangla" to "Bengali",
    "star jalsha" to "Bengali", "abp ananda" to "Bengali",
    "news18 bangla" to "Bengali", "aakaash aath" to "Bengali",
    "dd bangla" to "Bengali",
    "sun bangla" to "Bengali", "banga durbar" to "Bengali",

    "zee 24 kalak" to "Gujarati", "colors gujarati" to "Gujarati",
    "tv9 gujarat" to "Gujarati", "sandesh news" to "Gujarati",
    "abp asmita" to "Gujarati", "dd girnar" to "Gujarati",
    "news18 gujarat" to "Gujarati",

    "zee punjabi" to "Punjabi", "abp sanjha" to "Punjabi",
    "dd punjabi" to "Punjabi",

    "zee oriya" to "Odia", "zee odisha" to "Odia",
    "tarang tv" to "Odia", "dd odia" to "Odia",
    "kanak tv" to "Odia", "news7 odia" to "Odia",
    "prameya" to "Odia", "kalinga tv" to "Odia",
    "odisha tv" to "Odia", "nandighosha" to "Odia",
    "news18 odia" to "Odia",

    "prag tv" to "Assamese", "dy365" to "Assamese",
    "dd assam" to "Assamese", "ramdhenu" to "Assamese",
    "news live assam" to "Assamese",

    "zee bhojpuri" to "Bhojpuri", "mahuaa bhojpuri" to "Bhojpuri",
    "b4u bhojpuri" to "Bhojpuri",

    "aaj tak" to "Hindi", "zee news" to "Hindi", "abp news" to "Hindi",
    "ndtv india" to "Hindi", "india tv" to "Hindi", "republic bharat" to "Hindi",
    "sony tv" to "Hindi", "zee tv" to "Hindi", "star plus" to "Hindi",
    "colors tv" to "Hindi", "sab tv" to "Hindi",
    "dd national" to "Hindi", "doordarshan" to "Hindi",
    "dangal" to "Hindi", "big magic" to "Hindi",
    "shemaroo" to "Hindi", "star gold" to "Hindi",
    "zee cinema" to "Hindi", "zee anmol" to "Hindi",
    "&tv" to "Hindi", "rishtey" to "Hindi",
    "sony max" to "Hindi", "sony pal" to "Hindi", "sony sab" to "Hindi",
    "star bharat" to "Hindi", "colors rishtey" to "Hindi",
    "sadhna" to "Hindi", "astha" to "Hindi", "sanskar" to "Hindi",
    "news18 india" to "Hindi", "zeel" to "Hindi",
    "zee action" to "Hindi", "zee smile" to "Hindi",
    "zee yuva" to "Hindi", "zee classic" to "Hindi",
    "zee bollywood" to "Hindi",
    "nick" to "Hindi", "pogo" to "Hindi", "sonic" to "Hindi",
    "cartoon network" to "Hindi", "disney" to "Hindi", "hungama" to "Hindi",
    "sony yay" to "Hindi",
    "discovery" to "Hindi",
    "sports18" to "Hindi", "star sports" to "Hindi",
    "ten sports" to "Hindi", "sony sports" to "Hindi",
    "dd sports" to "Hindi",
    "ndtv good times" to "Hindi", "food food" to "Hindi",
    "zoom tv" to "Hindi", "epic tv" to "Hindi",
    "baby tv" to "Hindi",
    "tv9" to "Hindi",
    "news nation" to "Hindi", "sudarshan news" to "Hindi",
    "jan tv" to "Hindi",
    "shemaroo" to "Hindi",
    "mtv india" to "Hindi",
    "travelxp" to "Hindi",
    "bindass" to "Hindi",
    "sangeet" to "Hindi",
    "music india" to "Hindi",
    "showbox" to "Hindi",
    "filmy" to "Hindi",
    "istream" to "Hindi",

    "times now" to "English", "ndtv 24x7" to "English",
    "republic world" to "English", "wion" to "English",
    "mirror now" to "English", "india today" to "English",
    "one india" to "English", "et now" to "English",
    "news9" to "English", "news nation english" to "English",
    "hbo" to "English", "star world" to "English",
    "comedy central" to "English", "zee cafe" to "English",
    "cnbc tv18" to "English", "cnbc" to "English", "cnn" to "English", "bbc" to "English", "history tv18" to "English",

    // Hindi channels - extended
    "star utsav movies" to "Hindi", "star utsav" to "Hindi",
    "colors infinity" to "Hindi", "colors super" to "Hindi",
    "sony bbc earth" to "Hindi", "zee zest" to "Hindi",
    "dd kisan" to "Hindi", "dd india" to "Hindi",
    "sansad tv" to "Hindi",
    "shemaroo umang" to "Hindi",
    "manoranjan grand" to "Hindi", "manoranjan prime" to "Hindi",
    "&flix" to "Hindi", "&prive" to "Hindi", "&xplore" to "Hindi",
    "times now navbharat" to "Hindi", "tv9 bharatvarsh" to "Hindi",
    "bharat 24" to "Hindi", "bharat express" to "Hindi",
    "good news today" to "Hindi", "india news" to "Hindi",
    "news 24" to "Hindi",
    "zee business" to "Hindi", "et now swadesh" to "Hindi",
    "cnbc awaaz" to "Hindi",
    "investigation discovery" to "Hindi",
    "sony sports ten" to "Hindi", "star sports select" to "Hindi",
    "sanskar tv" to "Hindi", "vedic" to "Hindi",
    "maha movie" to "Hindi", "mastiii" to "Hindi",
    "shemaroo marathi bana" to "Marathi",
    "abp ganga" to "Hindi",
    "all time movies" to "Hindi", "amar ujala" to "Hindi",
    "bansal news" to "Hindi", "bharat samachar" to "Hindi",
    "dhamaka movies" to "Hindi",
    "dhoom music" to "Hindi", "digi shala" to "Hindi",
    "disha tv" to "Hindi",
    "fateh tv" to "Hindi", "first india news" to "Hindi",
    "gangaur tv" to "Hindi", "goldmines" to "Hindi",
    "hnn 24" to "Hindi", "hindi khabar" to "Hindi",
    "ibc 24" to "Hindi", "india daily live" to "Hindi",
    "india voice" to "Hindi", "inh 24" to "Hindi",
    "ishwar bhakti" to "Hindi", "janta tv" to "Hindi",
    "jinvani" to "Hindi", "jk 24" to "Hindi",
    "jus hindi" to "Hindi", "k news india" to "Hindi",
    "kanshi tv" to "Hindi", "kashish news" to "Hindi",
    "khabar fast" to "Hindi", "khabrain" to "Hindi",
    "nazara" to "Hindi", "news 1 india" to "Hindi",
    "prime news" to "Hindi", "raftaar media" to "Hindi",
    "raj pariwar" to "Hindi", "rongeen" to "Hindi",
    "sach bedhadak" to "Hindi", "sadhna plus" to "Hindi",
    "shraddha" to "Hindi", "sikka news" to "Hindi",
    "srs music" to "Hindi", "swaraj tv" to "Hindi",
    "tez tv" to "Hindi", "the faith" to "Hindi",
    "the q" to "Hindi", "vande gujarat" to "Hindi",
    "yuva tejas" to "Hindi",
    "ek din" to "Hindi", "ekdant" to "Hindi",
    "divya tv" to "Hindi", "news track" to "Hindi",
    "apex news" to "Hindi", "delhi aaj tak" to "Hindi",
    "sat sahitya" to "Hindi", "savera india" to "Hindi",
    "shubh tv" to "Hindi", "sadhna bhakti" to "Hindi",
    "e vidya" to "Hindi", "haa tv" to "Hindi",
    "ind 24" to "Hindi",
    "namo tv" to "Hindi", "shiv shakti" to "Hindi",
    "svbc" to "Telugu",

    // Tamil channels - extended
    "colors tamil" to "Tamil", "sun music" to "Tamil",
    "sun news" to "Tamil", "zee thirai" to "Tamil",
    "vijay super" to "Tamil", "star vijay music" to "Tamil",
    "adithya tv" to "Tamil", "aadithya tv" to "Tamil",
    "news7 tamil" to "Tamil", "puthiya thalaimurai" to "Tamil",
    "thanthi tv" to "Tamil", "captain news" to "Tamil",
    "chithiram tv" to "Tamil", "d tamil" to "Tamil",
    "anjan tv" to "Tamil",
    "7s music" to "Tamil",
    "jaya tv" to "Tamil",
    "jaya max" to "Tamil",
    "travelxp tamil" to "Tamil", "sri sankara" to "Tamil",
    "ntv tamil" to "Tamil",
    "vasanth tv" to "Tamil", "velicham tv" to "Tamil",
    "aaseervatham" to "Tamil", "aastha tamil" to "Tamil",
    "angel tv" to "Tamil", "anb news" to "Tamil",
    "ayush tv" to "Tamil", "ccv" to "Tamil",
    "channel divya" to "Tamil", "darshana tamil" to "Tamil",
    "divyavani" to "Tamil", "mercy tv" to "Tamil",
    "isko tv" to "Tamil", "munoth" to "Tamil",
    "sidal" to "Tamil", "dmk" to "Tamil",
    "dharisanam" to "Tamil", "neem" to "Tamil",
    "thanipodhu" to "Tamil", "tvcm" to "Tamil",

    // Telugu channels - extended
    "zee cinemalu" to "Telugu", "star maa movies" to "Telugu",
    "etv cinema" to "Telugu", "star maa music" to "Telugu",
    "gemini movies" to "Telugu", "gemini music" to "Telugu",
    "tv5 news" to "Telugu", "prime9 news" to "Telugu",
    "studio n" to "Telugu", "mojo tv" to "Telugu",
    "jai telangana" to "Telugu", "i news telugu" to "Telugu",
    "maa tv" to "Telugu", "ntv telugu" to "Telugu",
    "10tv" to "Telugu", "10 tv" to "Telugu",
    "4tv" to "Telugu",
    "vanitha tv" to "Telugu", "tv1 telugu" to "Telugu",
    "cvr news" to "Telugu", "cvr health" to "Telugu", "cvr english" to "Telugu",
    "subhavaartha" to "Telugu",
    "aastha telugu" to "Telugu", "sri venkateswara" to "Telugu",
    "99tv" to "Telugu", "suvarna" to "Telugu",
    "telugu360" to "Telugu", "shakti tv" to "Telugu",
    "bharat tv" to "Telugu", "sri tv" to "Telugu",

    // Malayalam channels - extended
    "zee keralam cinema" to "Malayalam", "surya movies" to "Malayalam",
    "asianet movies" to "Malayalam", "asianet music" to "Malayalam",
    "kappa tv" to "Malayalam", "reporter tv" to "Malayalam",
    "indiavision" to "Malayalam",
    "kairali news" to "Malayalam", "kerala vision" to "Malayalam",
    "shalom tv" to "Malayalam", "yes india vision" to "Malayalam",
    "aastha malayalam" to "Malayalam",
    "24 news" to "Malayalam",
    "salem tv" to "Malayalam", "jesus" to "Malayalam",

    // Kannada channels - extended
    "zee picchar" to "Kannada", "star suvarna movies" to "Kannada",
    "udaya tv" to "Kannada", "udaya movies" to "Kannada",
    "udaya music" to "Kannada", "udaya news" to "Kannada",
    "aastha kannada" to "Kannada",

    // Marathi channels - extended
    "star pravah movies" to "Marathi",
    "zee talkies" to "Marathi", "mi marathi" to "Marathi",
    "9x jhakaas" to "Marathi",
    "sam tv" to "Marathi",
    "mh one shraddha" to "Marathi",
    "maiboli" to "Marathi",
    "jai maharashtra" to "Marathi",
    "lokshahi news" to "Marathi",

    // Bengali channels - extended
    "zee bangla cinema" to "Bengali", "jalsha movies" to "Bengali",
    "republic bangla" to "Bengali", "sangeet bangla" to "Bengali",
    "channel 10" to "Bengali", "news time" to "Bengali",
    "kolkata tv" to "Bengali", "aamar bangla" to "Bengali",
    "24 ghanta" to "Bengali",

    // Gujarati channels - extended
    "zee gujarati" to "Gujarati",
    "vtv news" to "Gujarati",

    // Punjabi channels - extended
    "ptc news" to "Punjabi", "ptc chak de" to "Punjabi",
    "chardikla" to "Punjabi", "just punjabi" to "Punjabi",
    "nazara punjab" to "Punjabi", "punjabi zindabad" to "Punjabi",
    "apna punjab" to "Punjabi",

    // Odia channels - extended
    "tarang music" to "Odia",
    "odisha news" to "Odia", "focus odisha" to "Odia",
    "mbctv" to "Odia",

    // Assamese channels - extended
    "pratidin" to "Assamese", "northeast live" to "Assamese",
    "ne news" to "Assamese", "assam talks" to "Assamese",

    // Bhojpuri channels - extended
    "bhojpuri cinema" to "Bhojpuri",

    // Additional Indian languages
    "konkani" to "Konkani",
    "manipuri" to "Meitei", "meitei" to "Meitei", "rang cn" to "Meitei",
    "nepali" to "Nepali",
    "haryanvi" to "Haryanvi",
    "rajasthani" to "Rajasthani",
    "maithili" to "Maithili",
    "sindhi" to "Sindhi",
    "sanskrit" to "Sanskrit", "gyandarshan" to "Sanskrit",
    "bodo" to "Bodo", "mizo" to "Mizo",
    "kashmiri" to "Kashmiri", "dogri" to "Dogri",
    "kokborok" to "Kokborok", "tulu" to "Tulu"
).sortedByDescending { it.first.length }

private fun inferLanguage(name: String, tvgLanguage: String, country: String, category: String): String {
    val nameLower = name.lowercase()
    val catLower = category.lowercase()
    val tvgLangLower = tvgLanguage.lowercase().trim()

    if (tvgLangLower.isNotBlank()) {
        val matched = LANGUAGES_BY_TVG[tvgLangLower]
        if (matched != null) return matched
        if (tvgLangLower in LANGUAGES_BY_TVG_LOWERCASE) {
            return tvgLangLower.replaceFirstChar { it.uppercase() }
        }
    }

    for ((kw, lang) in LANGUAGE_KEYWORD_PAIRS) {
        if (nameLower.contains(kw)) return lang
    }

    for ((kw, lang) in LANGUAGE_KEYWORD_PAIRS) {
        if (catLower.contains(kw)) return lang
    }

    return countryToLanguage(country)
}

private val LANGUAGES_BY_TVG = mapOf(
    "hi" to "Hindi", "hin" to "Hindi", "hindi" to "Hindi",
    "en" to "English", "eng" to "English", "english" to "English",
    "ta" to "Tamil", "tam" to "Tamil", "tamil" to "Tamil",
    "te" to "Telugu", "tel" to "Telugu", "telugu" to "Telugu",
    "ml" to "Malayalam", "mal" to "Malayalam", "malayalam" to "Malayalam",
    "kn" to "Kannada", "kan" to "Kannada", "kannada" to "Kannada",
    "mr" to "Marathi", "mar" to "Marathi", "marathi" to "Marathi",
    "bn" to "Bengali", "ben" to "Bengali", "bengali" to "Bengali",
    "gu" to "Gujarati", "guj" to "Gujarati", "gujarati" to "Gujarati",
    "pa" to "Punjabi", "pan" to "Punjabi", "punjabi" to "Punjabi",
    "or" to "Odia", "ori" to "Odia", "odia" to "Odia", "oriya" to "Odia",
    "as" to "Assamese", "asm" to "Assamese", "assamese" to "Assamese",
    "ur" to "Urdu", "urd" to "Urdu", "urdu" to "Urdu",
    "bh" to "Bhojpuri", "bho" to "Bhojpuri", "bhojpuri" to "Bhojpuri",
    "kok" to "Konkani", "gom" to "Konkani", "konkani" to "Konkani",
    "mni" to "Meitei", "meitei" to "Meitei", "manipuri" to "Meitei",
    "ne" to "Nepali", "nep" to "Nepali", "nepali" to "Nepali",
    "bgc" to "Haryanvi", "haryanvi" to "Haryanvi",
    "rajasthani" to "Rajasthani",
    "mai" to "Maithili", "maithili" to "Maithili",
    "sd" to "Sindhi", "snd" to "Sindhi", "sindhi" to "Sindhi",
    "sa" to "Sanskrit", "san" to "Sanskrit", "sanskrit" to "Sanskrit",
    "brx" to "Bodo", "bodo" to "Bodo",
    "lus" to "Mizo", "mizo" to "Mizo",
    "ks" to "Kashmiri", "kas" to "Kashmiri", "kashmiri" to "Kashmiri",
    "doi" to "Dogri", "dogri" to "Dogri",
    "tru" to "Tulu", "tulu" to "Tulu"
)

private val LANGUAGES_BY_TVG_LOWERCASE = LANGUAGES_BY_TVG.values.mapTo(hashSetOf()) { it.lowercase() }

private fun countryToLanguage(country: String): String {
    return when (country) {
        "United States", "UK", "Australia", "New Zealand", "Canada" -> "English"
        "India" -> "Hindi"
        "Spain", "Mexico", "Argentina", "Colombia", "Peru", "Chile", "Venezuela", "Ecuador",
            "Guatemala", "Cuba", "Bolivia", "Dominican Rep.", "Honduras", "Paraguay", "El Salvador",
            "Nicaragua", "Costa Rica", "Puerto Rico", "Panama", "Uruguay" -> "Spanish"
        "France", "Belgium", "Switzerland" -> "French"
        "Germany", "Austria" -> "German"
        "Italy" -> "Italian"
        "Brazil", "Portugal" -> "Portuguese"
        "Russia" -> "Russian"
        "China", "Taiwan" -> "Chinese"
        "Japan" -> "Japanese"
        "South Korea" -> "Korean"
        "Turkey" -> "Turkish"
        "Netherlands" -> "Dutch"
        "Poland" -> "Polish"
        "Ukraine" -> "Ukrainian"
        "Sweden" -> "Swedish"
        "Norway" -> "Norwegian"
        "Denmark" -> "Danish"
        "Finland" -> "Finnish"
        "Indonesia" -> "Indonesian"
        "Vietnam" -> "Vietnamese"
        "Thailand" -> "Thai"
        "Philippines" -> "Filipino"
        "Malaysia" -> "Malay"
        "Romania" -> "Romanian"
        "Greece" -> "Greek"
        "Czechia" -> "Czech"
        "Hungary" -> "Hungarian"
        "Iran" -> "Persian"
        "Saudi Arabia", "UAE", "Egypt", "Iraq", "Kuwait", "Qatar", "Bahrain", "Oman", "Jordan",
            "Lebanon", "Algeria", "Morocco", "Tunisia", "Libya", "Sudan", "Syria", "Palestine",
            "Yemen" -> "Arabic"
        "Pakistan" -> "Urdu"
        "Bangladesh" -> "Bengali"
        "Israel" -> "Hebrew"
        "Bulgaria" -> "Bulgarian"
        "Serbia" -> "Serbian"
        "Croatia" -> "Croatian"
        "Slovakia" -> "Slovak"
        "Slovenia" -> "Slovene"
        "Lithuania" -> "Lithuanian"
        "Latvia" -> "Latvian"
        "Estonia" -> "Estonian"
        "Albania" -> "Albanian"
        "Armenia" -> "Armenian"
        "Georgia" -> "Georgian"
        "Azerbaijan" -> "Azerbaijani"
        "Kazakhstan" -> "Kazakh"
        "Nigeria", "Ghana", "Kenya", "South Africa", "Ethiopia", "Tanzania", "Uganda", "Angola",
            "Mozambique", "Zambia", "Senegal", "Cameroon", "Ivory Coast", "DR Congo", "Congo",
            "Zimbabwe", "Somalia", "Rwanda", "Malawi", "Botswana", "Niger", "Mali", "Madagascar",
            "Sierra Leone", "Guinea", "Togo", "Benin", "Chad", "Burkina Faso", "Eritrea",
            "South Sudan", "Mauritius", "Namibia", "Liberia", "Lesotho", "Gabon", "Mozambique",
            "Gambia", "Mauritania", "Eswatini" -> "English"
        else -> ""
    }
}
