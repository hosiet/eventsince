/*
 * Copyright 2026 Boyuan Yang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.byang.eventsince.core.time

import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import org.junit.Test
import kotlin.test.assertEquals

class DateFormatsTest {
    private val zone = ZoneId.of("UTC")
    private val millis = LocalDateTime.of(2024, 3, 7, 9, 5).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `manual formats`() {
        assertEquals("07/03/2024", DateFormats.formatDate(millis, DateFormatPreference.DAY_MONTH_YEAR, Locale.US, zone))
        assertEquals("03/07/2024", DateFormats.formatDate(millis, DateFormatPreference.MONTH_DAY_YEAR, Locale.US, zone))
        assertEquals("03/07/2024 09:05", DateFormats.formatDateTime(millis, DateFormatPreference.MONTH_DAY_YEAR, Locale.US, zone))
        assertEquals("2024-03-07", DateFormats.formatDate(millis, DateFormatPreference.ISO, Locale.US, zone))
        assertEquals(DateFormatPreference.ISO, DateFormatPreference.fromKey("yyyy-MM-dd"))
    }

    @Test
    fun `system format follows locale`() {
        assertEquals("Mar 7, 2024", DateFormats.formatDate(millis, DateFormatPreference.SYSTEM, Locale.US, zone))
    }
}
