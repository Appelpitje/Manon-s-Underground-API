package com.manonsunderground.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Response from the serverlist endpoint
 */
data class ServerListResponse(
    val servers: List<ServerInfo>,
    val metadata: ServerListMetadata
)

/**
 * Individual server information in the list
 */
data class ServerInfo(
    val id: Int,
    val ip: String,
    val hostport: Int,
    val hostname: String,
    val gamename: String,
    val gametype: String?,
    val label: String?,
    val country: String?,
    val numplayers: Int,
    val maxplayers: Int,
    val maptitle: String?,
    val mapname: String?,
    @JsonProperty("dt_added")
    val dtAdded: Long,
    @JsonProperty("dt_updated")
    val dtUpdated: Long
)

/**
 * Metadata about the serverlist response
 */
data class ServerListMetadata(
    val players: Int,
    val total: Int
)

/**
 * Query parameters for serverlist requests
 */
data class ServerListQuery(
    val gamename: String,
    val sortBy: String? = null, // country, hostname, gametype, ip, hostport, numplayers, mapname
    val order: String? = null, // 'a' for ascending, 'd' for descending
    val results: Int? = null, // 1-1000, default 50
    val page: Int? = null,
    val query: String? = null, // search query, max 90 chars
    val gametype: String? = null, // filter by gametype, max 90 chars
    val hostname: String? = null, // filter by server name, max 90 chars
    val mapname: String? = null, // filter by map name, max 90 chars
    val country: String? = null // 2 letter ISO 3166 code
) {
    fun toQueryParams(): Map<String, String> {
        val params = mutableMapOf<String, String>()
        sortBy?.let { params["s"] = it }
        order?.let { params["o"] = it }
        results?.let { params["r"] = it.toString() }
        page?.let { params["p"] = it.toString() }
        query?.let { if (it.length <= 90) params["q"] = it }
        gametype?.let { if (it.length <= 90) params["gametype"] = it }
        hostname?.let { if (it.length <= 90) params["hostname"] = it }
        mapname?.let { if (it.length <= 90) params["mapname"] = it }
        country?.let { params["country"] = it }
        return params
    }
}
