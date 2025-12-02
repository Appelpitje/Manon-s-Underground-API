package com.manonsunderground.service

import com.manonsunderground.entity.Player
import com.manonsunderground.entity.Server
import com.manonsunderground.entity.ServerSnapshot
import com.manonsunderground.model.ServerDetailsResponse
import com.manonsunderground.model.ServerListQuery
import com.manonsunderground.repository.PlayerRepository
import com.manonsunderground.repository.ServerRepository
import com.manonsunderground.repository.ServerSnapshotRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Service for creating and managing server snapshots
 */
@Service
class ServerSnapshotService(
    private val networksApiService: NetworksApiService,
    private val serverRepository: ServerRepository,
    private val serverSnapshotRepository: ServerSnapshotRepository,
    private val playerRepository: PlayerRepository
) {
    private val logger = LoggerFactory.getLogger(ServerSnapshotService::class.java)
    
    /**
     * Fetch and save snapshots for all MOHAA game variants
     * Only queries servers that have active players
     */
    @Transactional
    fun snapshotAllMohaaServers(): SnapshotResult {
        logger.info("Starting snapshot of all MOHAA servers")
        val startTime = Instant.now()
        var serversProcessed = 0
        var serversWithPlayers = 0
        var playersRecorded = 0
        var errors = 0
        
        val gamenames = listOf("mohaa", "mohaas", "mohaab")
        
        gamenames.forEach { gamename ->
            try {
                val result = snapshotGameServers(gamename)
                serversProcessed += result.serversProcessed
                serversWithPlayers += result.serversWithPlayers
                playersRecorded += result.playersRecorded
                errors += result.errors
            } catch (e: Exception) {
                logger.error("Error processing gamename: $gamename", e)
                errors++
            }
        }
        
        val duration = java.time.Duration.between(startTime, Instant.now())
        logger.info(
            "Snapshot completed in ${duration.toMillis()}ms. " +
            "Processed: $serversProcessed servers, " +
            "With players: $serversWithPlayers, " +
            "Players recorded: $playersRecorded, " +
            "Errors: $errors"
        )
        
        return SnapshotResult(
            serversProcessed,
            serversWithPlayers,
            playersRecorded,
            errors,
            duration.toMillis()
        )
    }
    
    /**
     * Snapshot servers for a specific game
     */
    @Transactional
    fun snapshotGameServers(gamename: String): SnapshotResult {
        logger.info("Fetching server list for: $gamename")
        var serversProcessed = 0
        var serversWithPlayers = 0
        var playersRecorded = 0
        var errors = 0
        
        try {
            // Get all servers for this game
            val serverList = networksApiService.getServerList(
                ServerListQuery(
                    gamename = gamename,
                    results = 1000 // Get max results
                )
            )
            
            serversProcessed = serverList.servers.size
            logger.info("Found ${serverList.servers.size} servers for $gamename")
            
            // Save all servers and snapshot those with players
            serverList.servers.forEach { serverInfo ->
                try {
                    // Ensure server entity exists (save all servers)
                    val server = findOrSaveServer(
                        serverInfo.id,
                        serverInfo.ip,
                        serverInfo.hostport,
                        serverInfo.hostname,
                        serverInfo.gamename,
                        serverInfo.country
                    )

                    // Only fetch details and snapshot if players are present
                    if (serverInfo.numplayers > 0) {
                        val serverDetails = networksApiService.getServerDetails(
                            gamename,
                            serverInfo.ip,
                            serverInfo.hostport
                        )
                        
                        val savedSnapshot = saveServerSnapshot(serverDetails, server)
                        serversWithPlayers++
                        playersRecorded += savedSnapshot.players.size
                        
                        logger.debug(
                            "Saved snapshot for ${serverInfo.hostname} with ${savedSnapshot.players.size} players"
                        )
                    }
                } catch (e: Exception) {
                    logger.error(
                        "Error processing server ${serverInfo.hostname} " +
                        "(${serverInfo.ip}:${serverInfo.hostport})", 
                        e
                    )
                    errors++
                }
            }
        } catch (e: Exception) {
            logger.error("Error fetching server list for $gamename", e)
            errors++
        }
        
        return SnapshotResult(serversProcessed, serversWithPlayers, playersRecorded, errors, 0)
    }

    /**
     * Find existing server or create new one
     */
    private fun findOrSaveServer(
        serverId: Int,
        ip: String,
        hostport: Int,
        hostname: String,
        gamename: String,
        country: String?
    ): Server {
        var server = serverRepository.findByIpAndHostportAndHostname(ip, hostport, hostname)
        
        if (server == null) {
            server = Server(
                serverId = serverId,
                ip = ip,
                hostport = hostport,
                hostname = hostname,
                gamename = gamename,
                country = country
            )
            server = serverRepository.save(server)
        }
        return server!!
    }
    
    /**
     * Save a server snapshot with its players
     */
    @Transactional
    fun saveServerSnapshot(serverDetails: ServerDetailsResponse, preLoadedServer: Server? = null): ServerSnapshot {
        val snapshotTime = Instant.now()
        
        // Find or create Server identity
        val server = preLoadedServer ?: findOrSaveServer(
            serverDetails.id,
            serverDetails.ip,
            serverDetails.hostport,
            serverDetails.hostname,
            serverDetails.gamename,
            serverDetails.country
        )
        
        // Create Snapshot
        val snapshot = ServerSnapshot(
            server = server!!,
            gametype = serverDetails.gametype,
            mapname = serverDetails.mapname,
            mapurl = serverDetails.mapurl,
            gamever = serverDetails.gamever,
            numPlayers = serverDetails.numplayers ?: 0,
            maxPlayers = serverDetails.maxplayers ?: 0,
            password = serverDetails.password,
            timelimit = serverDetails.timelimit,
            fraglimit = serverDetails.fraglimit,
            snapshotTime = snapshotTime,
            dtUpdated = serverDetails.dtUpdated,
            players = mutableListOf()
        )
        
        // Extract and add players
        val playerInfoList = serverDetails.extractPlayers()
        playerInfoList.forEach { playerInfo ->
            val player = Player(
                serverSnapshot = snapshot,
                name = playerInfo.name,
                frags = playerInfo.frags,
                ping = playerInfo.ping,
                snapshotTime = snapshotTime
            )
            snapshot.players.add(player)
        }
        
        return serverSnapshotRepository.save(snapshot)
    }
}

/**
 * Result of a snapshot operation
 */
data class SnapshotResult(
    val serversProcessed: Int,
    val serversWithPlayers: Int,
    val playersRecorded: Int,
    val errors: Int,
    val durationMs: Long
)
