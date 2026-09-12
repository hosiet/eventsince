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
package me.byang.eventsince.domain.model

enum class SortOrder(val key: String) {
    ASC("ASC"), DESC("DESC");

    fun flipped(): SortOrder = if (this == ASC) DESC else ASC

    companion object {
        fun fromKey(key: String?): SortOrder = if (key.equals("DESC", ignoreCase = true)) DESC else ASC
    }
}

data class Category(
    val id: String,
    /** Empty for the default category until the user renames it; the UI shows a localised fallback. */
    val name: String,
    val position: Int,
    val sort: SortOrder,
    val isDefault: Boolean,
)
