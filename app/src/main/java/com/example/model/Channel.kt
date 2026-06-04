package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChannelNetwork(
    @Json(name = "name") val name: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "logo") val logo: String? = null,
    @Json(name = "logo_url") val logoUrl: String? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "group") val group: String? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "stream") val stream: String? = null,
    @Json(name = "link") val link: String? = null
) {
    fun toChannel(): Channel {
        val finalUrl = url ?: stream ?: link ?: ""
        return Channel(
            id = finalUrl.hashCode().toString(),
            name = (name ?: title ?: "Unknown Channel").trim(),
            logo = (logo ?: logoUrl ?: "").trim(),
            category = (category ?: group ?: "General").trim(),
            country = (country ?: "Global").trim(),
            url = finalUrl.trim()
        )
    }
}

data class Channel(
    val id: String,
    val name: String,
    val logo: String,
    val category: String,
    val country: String,
    val url: String
)
