package com.example.offerhub.ui.format

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatOfferTimestamp(
    value: String,
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault()
): String = runCatching {
    DateTimeFormatter
        .ofPattern("dd MMM yyyy, HH:mm", locale)
        .format(Instant.parse(value).atZone(zoneId))
}.getOrDefault(value)
