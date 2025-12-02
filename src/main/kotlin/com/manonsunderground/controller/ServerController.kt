package com.manonsunderground.controller

import com.manonsunderground.model.ServerDetailsResponse
import com.manonsunderground.model.ServerListQuery
import com.manonsunderground.model.ServerListResponse
import com.manonsunderground.model.MotdResponse
import com.manonsunderground.service.NetworksApiService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST Controller for accessing 333networks server information
 * 
 * Data source: 333networks (https://www.333networks.com)
 */
@RestController
@RequestMapping("/api/servers")
class ServerController(
    private val networksApiService: NetworksApiService
) {
    
    /**
     * Get Message of the Day for a game
     * 
     * @param gamename Game identifier (mohaa, mohaas, mohaab, etc.)
     * @return MOTD response with HTML content
     */
    @GetMapping("/{gamename}/motd")
    fun getMotd(@PathVariable gamename: String): ResponseEntity<MotdResponse> {
        val motd = networksApiService.getMotd(gamename)
        return ResponseEntity.ok(motd)
    }
    
    /**
     * Get server list for a specific game with optional filters
     * 
     * @param gamename Game identifier
     * @param sortBy Sort by field (country, hostname, gametype, ip, hostport, numplayers, mapname)
     * @param order Sort order ('a' ascending, 'd' descending)
     * @param results Number of results (1-1000, default 50)
     * @param page Page number
     * @param query Search query (max 90 chars)
     * @param gametype Filter by gametype (max 90 chars)
     * @param hostname Filter by hostname (max 90 chars)
     * @param mapname Filter by mapname (max 90 chars)
     * @param country Filter by country code (2 letter ISO 3166)
     * @return Server list response
     */
    @GetMapping("/{gamename}")
    fun getServerList(
        @PathVariable gamename: String,
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(required = false) order: String?,
        @RequestParam(required = false) results: Int?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) gametype: String?,
        @RequestParam(required = false) hostname: String?,
        @RequestParam(required = false) mapname: String?,
        @RequestParam(required = false) country: String?
    ): ResponseEntity<ServerListResponse> {
        val serverListQuery = ServerListQuery(
            gamename = gamename,
            sortBy = sortBy,
            order = order,
            results = results,
            page = page,
            query = query,
            gametype = gametype,
            hostname = hostname,
            mapname = mapname,
            country = country
        )
        
        val serverList = networksApiService.getServerList(serverListQuery)
        return ResponseEntity.ok(serverList)
    }
    
    /**
     * Get detailed information for a specific server
     * 
     * @param gamename Game identifier
     * @param ip Server IP address (IPv4)
     * @param port Server port
     * @return Detailed server information including player list
     */
    @GetMapping("/{gamename}/{ip}/{port}")
    fun getServerDetails(
        @PathVariable gamename: String,
        @PathVariable ip: String,
        @PathVariable port: Int
    ): ResponseEntity<ServerDetailsResponse> {
        val serverDetails = networksApiService.getServerDetails(gamename, ip, port)
        return ResponseEntity.ok(serverDetails)
    }
    
    /**
     * Get all Medal of Honor Allied Assault servers (mohaa, mohaas, mohaab)
     * 
     * @param sortBy Sort by field
     * @param order Sort order ('a' ascending, 'd' descending)
     * @param results Number of results per game (1-1000, default 50)
     * @param page Page number
     * @param query Search query (max 90 chars)
     * @param gametype Filter by gametype (max 90 chars)
     * @param hostname Filter by hostname (max 90 chars)
     * @param mapname Filter by mapname (max 90 chars)
     * @param country Filter by country code (2 letter ISO 3166)
     * @return Map of gamename to server list
     */
    @GetMapping("/mohaa/all")
    fun getAllMohaServers(
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(required = false) order: String?,
        @RequestParam(required = false) results: Int?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) gametype: String?,
        @RequestParam(required = false) hostname: String?,
        @RequestParam(required = false) mapname: String?,
        @RequestParam(required = false) country: String?
    ): ResponseEntity<Map<String, ServerListResponse>> {
        val queryTemplate = ServerListQuery(
            gamename = "", // Will be overridden per game
            sortBy = sortBy,
            order = order,
            results = results,
            page = page,
            query = query,
            gametype = gametype,
            hostname = hostname,
            mapname = mapname,
            country = country
        )
        
        val allServers = networksApiService.getAllMohaServers(queryTemplate)
        return ResponseEntity.ok(allServers)
    }
    
    /**
     * Clear the cache for better testing or force refresh
     * Note: Use sparingly to respect API rate limits
     */
    @PostMapping("/cache/clear")
    fun clearCache(@RequestParam(required = false) key: String?): ResponseEntity<Map<String, String>> {
        networksApiService.clearCache(key)
        return ResponseEntity.ok(mapOf("message" to "Cache cleared successfully"))
    }
}
