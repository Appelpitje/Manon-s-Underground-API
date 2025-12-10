package com.manonsunderground.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.manonsunderground.config.NetworksApiConfig
import com.manonsunderground.model.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Service to interact with the 333networks JSON API
 * 
 * This service respects the API terms of use:
 * - Server information is updated every 7.5 minutes
 * - Implements caching to avoid flooding the API
 * - Credits 333networks as required
 * 
 * @see <a href="https://www.333networks.com/json.php">333networks JSON API Documentation</a>
 */
@Service
class NetworksApiService(
    private val config: NetworksApiConfig,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(NetworksApiService::class.java)
    private val restTemplate = RestTemplate()
    private val cache = ConcurrentHashMap<String, CachedResponse<*>>()
    
    companion object {
        const val CACHE_DURATION_MS = 450000L // 7.5 minutes in milliseconds
    }
    
    data class CachedResponse<T>(
        val data: T,
        val timestamp: Long
    )
    
    /**
     * Get the Message of the Day for a specific game
     * 
     * @param gamename Game identifier (e.g., "mohaa", "mohaas", "mohaab")
     * @return MotdResponse containing HTML content and server/player counts
     */
    fun getMotd(gamename: String): MotdResponse {
        val cacheKey = "motd_$gamename"
        return getCachedOrFetch(cacheKey) {
            val url = "${config.baseUrl}/$gamename/motd"
            logger.info("Fetching MOTD for gamename: $gamename from $url")
            
            val response = restTemplate.getForObject(url, String::class.java)
                ?: throw NetworksApiException(
                    ApiError(1, "network_error"),
                    "Failed to fetch MOTD"
                )
            
            parseMotdResponse(response)
        }
    }
    
    /**
     * Get a list of servers for a specific game
     * 
     * @param query ServerListQuery containing gamename and optional filters
     * @return ServerListResponse containing list of servers and metadata
     */
    fun getServerList(query: ServerListQuery): ServerListResponse {
        val cacheKey = "serverlist_${query.gamename}_${query.hashCode()}"
        return getCachedOrFetch(cacheKey) {
            val url = buildServerListUrl(query)
            logger.info("Fetching server list from $url")
            
            val response = restTemplate.getForObject(url, String::class.java)
                ?: throw NetworksApiException(
                    ApiError(1, "network_error"),
                    "Failed to fetch server list"
                )
            
            parseServerListResponse(response)
        }
    }
    
    /**
     * Get detailed information for a specific server
     * 
     * @param gamename Game identifier
     * @param ip Server IP address (IPv4)
     * @param port Server port
     * @return ServerDetailsResponse containing detailed server and player information
     */
    fun getServerDetails(gamename: String, ip: String, port: Int): ServerDetailsResponse {
        // Resolve hostname to IP if needed - consistent with BannerService
        val resolvedIp = try {
            val resolved = com.manonsunderground.util.DnsUtil.resolveToIp(ip)
            if (resolved != ip) {
                logger.info("DNS resolution (api): input='$ip', resolved='$resolved'")
            }
            resolved
        } catch (e: Exception) {
            logger.warn("Failed to resolve hostname: $ip, using original input", e)
            ip
        }
        
        val cacheKey = "server_${gamename}_${resolvedIp}_$port"
        return getCachedOrFetch(cacheKey) {
            val url = "${config.baseUrl}/$gamename/$resolvedIp:$port"
            logger.info("Fetching server details from $url")
            
            val response = restTemplate.getForObject(url, String::class.java)
                ?: throw NetworksApiException(
                    ApiError(1, "network_error"),
                    "Failed to fetch server details"
                )
            
            parseServerDetailsResponse(response)
        }
    }
    
    /**
     * Get all servers for Medal of Honor Allied Assault (all variations)
     * 
     * @param query Optional filters (excluding gamename which is set automatically)
     * @return Map of gamename to ServerListResponse
     */
    fun getAllMohaServers(query: ServerListQuery? = null): Map<String, ServerListResponse> {
        val mohaaGamenames = listOf("mohaa", "mohaas", "mohaab")
        return mohaaGamenames.associateWith { gamename ->
            getServerList(
                query?.copy(gamename = gamename) 
                    ?: ServerListQuery(gamename = gamename)
            )
        }
    }
    
    private fun <T> getCachedOrFetch(key: String, fetchFunction: () -> T): T {
        val cached = cache[key]
        val now = Instant.now().toEpochMilli()
        
        if (cached != null && (now - cached.timestamp) < CACHE_DURATION_MS) {
            logger.debug("Returning cached response for key: $key")
            @Suppress("UNCHECKED_CAST")
            return cached.data as T
        }
        
        val fresh = fetchFunction()
        cache[key] = CachedResponse(fresh, now)
        return fresh
    }
    
    private fun buildServerListUrl(query: ServerListQuery): String {
        val baseUrl = "${config.baseUrl}/${query.gamename}"
        val params = query.toQueryParams()
        
        if (params.isEmpty()) {
            return baseUrl
        }
        
        val queryString = params.entries.joinToString("&") { (key, value) ->
            "$key=${URLEncoder.encode(value, StandardCharsets.UTF_8)}"
        }
        
        return "$baseUrl?$queryString"
    }
    
    private fun parseMotdResponse(json: String): MotdResponse {
        return try {
            val node = objectMapper.readTree(json)
            MotdResponse(
                html = node.get("html")?.asText() ?: "",
                servers = node.get("servers")?.asInt(),
                players = node.get("players")?.asInt()
            )
        } catch (e: Exception) {
            logger.error("Failed to parse MOTD response", e)
            throw NetworksApiException(
                ApiError(1, "parse_error"),
                "Failed to parse MOTD response: ${e.message}"
            )
        }
    }
    
    private fun parseServerListResponse(json: String): ServerListResponse {
        return try {
            val rootNode = objectMapper.readTree(json)
            
            // Check for error
            if (rootNode.has("error")) {
                val error = objectMapper.treeToValue(rootNode, ApiError::class.java)
                throw NetworksApiException(error)
            }
            
            // Response is an array: [servers[], metadata]
            if (rootNode.isArray && rootNode.size() == 2) {
                val serversNode = rootNode[0]
                val metadataNode = rootNode[1]
                
                val servers: List<ServerInfo> = objectMapper.readValue(serversNode.toString())
                val metadata = ServerListMetadata(
                    players = metadataNode.get("players")?.asInt() ?: 0,
                    total = metadataNode.get("total")?.asInt() ?: 0
                )
                
                ServerListResponse(servers, metadata)
            } else {
                throw IllegalStateException("Unexpected response format")
            }
        } catch (e: NetworksApiException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to parse server list response", e)
            throw NetworksApiException(
                ApiError(1, "parse_error"),
                "Failed to parse server list response: ${e.message}"
            )
        }
    }
    
    private fun parseServerDetailsResponse(json: String): ServerDetailsResponse {
        return try {
            val rootNode = objectMapper.readTree(json)
            
            // Check for error
            if (rootNode.has("error")) {
                val error = objectMapper.treeToValue(rootNode, ApiError::class.java)
                throw NetworksApiException(error)
            }
            
            objectMapper.readValue(json)
        } catch (e: NetworksApiException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to parse server details response", e)
            throw NetworksApiException(
                ApiError(1, "parse_error"),
                "Failed to parse server details response: ${e.message}"
            )
        }
    }
    
    /**
     * Clear the cache for a specific key or all cache if key is null
     */
    fun clearCache(key: String? = null) {
        if (key != null) {
            cache.remove(key)
            logger.info("Cleared cache for key: $key")
        } else {
            cache.clear()
            logger.info("Cleared all cache")
        }
    }
}
