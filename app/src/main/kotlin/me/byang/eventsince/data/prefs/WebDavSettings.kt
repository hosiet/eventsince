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
package me.byang.eventsince.data.prefs

data class WebDavSettings(
    val url: String = "",
    val username: String = "",
    /** Plain text in memory only; stored encrypted. */
    val password: String = "",
    val remoteDir: String = "/EventSince/",
    val trustAllCertificates: Boolean = false,
    val autoBackup: Boolean = false,
    val wifiOnly: Boolean = false,
    val keepCount: Int = 10,
    val lastBackupAt: Long? = null,
    val lastBackupError: String? = null,
) {
    val isConfigured: Boolean get() = url.isNotBlank()
}
