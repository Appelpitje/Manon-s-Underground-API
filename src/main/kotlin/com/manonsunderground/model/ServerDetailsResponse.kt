package com.manonsunderground.model

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Detailed server information including player list
 */
data class ServerDetailsResponse(
    val id: Int,
    val ip: String,
    val hostport: Int,
    val mapname: String?,
    val adminname: String?,
    val hostname: String,
    val mapurl: String?,
    val gamever: String?,
    val gametype: String?,
    val gamename: String,
    val country: String?,
    @JsonProperty("dt_updated")
    val dtUpdated: Long,
    val listenserver: String?,
    val adminemail: String?,
    val password: String?,
    val gamestyle: String?,
    val changelevels: String?,
    val maptitle: String?,
    val minplayers: Int?,
    val numplayers: Int?,
    val maxplayers: Int?,
    val botskill: String?,
    val balanceteams: String?,
    val playersbalanceteams: String?,
    val friendlyfire: String?,
    val maxteams: String?,
    val timelimit: String?,
    val goalteamscore: String?,
    val fraglimit: String?,
    val mutators: String?,
    val misc: String?
) {
    // Use @JsonAnySetter to capture dynamic player_N fields during deserialization
    @JsonAnySetter
    private val dynamicFields: MutableMap<String, Any> = mutableMapOf()
    
    fun extractPlayers(): List<PlayerInfo> {
        // Extract player_N fields from dynamic fields
        return dynamicFields
            .filterKeys { it.startsWith("player_") }
            .values
            .mapNotNull { it as? Map<*, *> }
            .map { playerMap ->
                PlayerInfo(
                    sid = playerMap["sid"] as? Int ?: 0,
                    name = playerMap["name"] as? String ?: "",
                    team = playerMap["team"] as? String,
                    frags = playerMap["frags"] as? Int ?: 0,
                    mesh = playerMap["mesh"] as? String,
                    skin = playerMap["skin"] as? String,
                    face = playerMap["face"] as? String,
                    ping = playerMap["ping"] as? Int ?: 0,
                    dtPlayer = playerMap["dt_player"] as? Long ?: 0,
                    misc = playerMap["misc"] as? String
                )
            }
    }
}

/**
 * Player information within a server
 */
data class PlayerInfo(
    val sid: Int,
    val name: String,
    val team: String?,
    val frags: Int,
    val mesh: String?,
    val skin: String?,
    val face: String?,
    val ping: Int,
    @JsonProperty("dt_player")
    val dtPlayer: Long,
    val misc: String?
)
