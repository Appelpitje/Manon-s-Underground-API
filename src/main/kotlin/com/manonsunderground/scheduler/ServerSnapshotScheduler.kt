package com.manonsunderground.scheduler

import com.manonsunderground.service.ServerSnapshotService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Scheduled task to snapshot MOHAA servers every 7.5 minutes
 * 
 * This aligns with the 333networks API update interval and respects their terms of use.
 */
@Component
class ServerSnapshotScheduler(
    private val serverSnapshotService: ServerSnapshotService
) {
    private val logger = LoggerFactory.getLogger(ServerSnapshotScheduler::class.java)
    
    companion object {
        // 7.5 minutes in milliseconds
        const val SNAPSHOT_INTERVAL_MS = 450000L
    }
    
    /**
     * Run every 7.5 minutes to snapshot all MOHAA servers
     * Initial delay of 30 seconds to allow application to fully start
     */
    @Scheduled(fixedRate = SNAPSHOT_INTERVAL_MS, initialDelay = 30000)
    fun snapshotServers() {
        logger.info("=== Starting scheduled server snapshot ===")
        
        try {
            val result = serverSnapshotService.snapshotAllMohaaServers()
            
            logger.info(
                "=== Snapshot completed successfully ===" +
                "\n  Servers processed: ${result.serversProcessed}" +
                "\n  Servers with players: ${result.serversWithPlayers}" +
                "\n  Players recorded: ${result.playersRecorded}" +
                "\n  Errors: ${result.errors}" +
                "\n  Duration: ${result.durationMs}ms"
            )
            
            if (result.errors > 0) {
                logger.warn("Snapshot completed with ${result.errors} errors. Check logs for details.")
            }
        } catch (e: Exception) {
            logger.error("Fatal error during scheduled snapshot", e)
        }
    }
}
