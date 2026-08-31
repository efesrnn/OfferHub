package com.example.offerhub.ui.format

import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class OfferDateFormatterTest {
    @Test
    fun `ISO timestamp is formatted for display`() {
        val result = formatOfferTimestamp(
            value = "2026-08-24T10:30:00Z",
            zoneId = ZoneOffset.UTC,
            locale = Locale.US
        )

        assertEquals("24 Aug 2026, 10:30", result)
    }

    @Test
    fun `invalid timestamp is returned unchanged`() {
        assertEquals("unknown", formatOfferTimestamp("unknown"))
    }
}
