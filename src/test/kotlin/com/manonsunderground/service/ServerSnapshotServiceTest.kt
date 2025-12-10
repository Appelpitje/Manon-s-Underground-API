package com.manonsunderground.service

import com.manonsunderground.entity.Server
import com.manonsunderground.entity.ServerSnapshot
import com.manonsunderground.repository.PlayerRepository
import com.manonsunderground.repository.ServerRepository
import com.manonsunderground.repository.ServerSnapshotRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.time.Instant

class ServerSnapshotServiceTest {

    @Mock
    lateinit var networksApiService: NetworksApiService

    @Mock
    lateinit var serverRepository: ServerRepository

    @Mock
    lateinit var serverSnapshotRepository: ServerSnapshotRepository

    @Mock
    lateinit var playerRepository: PlayerRepository

    lateinit var serverSnapshotService: ServerSnapshotService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        serverSnapshotService = ServerSnapshotService(
            networksApiService,
            serverRepository,
            serverSnapshotRepository,
            playerRepository
        )
    }

    @Test
    fun testGetPlayerHistoryAggregation() {
        val ip = "127.0.0.1"
        val port = 12203
        val serverId = 1L
        
        val server = Server(
            id = serverId,
            serverId = 12345,
            ip = ip,
            hostport = port,
            hostname = "Test Server",
            gamename = "mohaa",
            country = "US"
        )

        `when`(serverRepository.findTopByIpAndHostportOrderByIdDesc(ip, port)).thenReturn(server)

        val startTime = Instant.parse("2023-10-01T10:00:00Z")
        val endTime = Instant.parse("2023-10-01T11:00:00Z")

        // Create 3 snapshots
        // Snapshot 1: 10:05 (Bucket 10:00-10:15) - 5 players
        // Snapshot 2: 10:10 (Bucket 10:00-10:15) - 10 players (Max)
        // Snapshot 3: 10:20 (Bucket 10:15-10:30) - 8 players
        
        val s1 = createSnapshot(server, startTime.plusSeconds(300), 5)
        val s2 = createSnapshot(server, startTime.plusSeconds(600), 10)
        val s3 = createSnapshot(server, startTime.plusSeconds(1200), 8)
        
        `when`(serverSnapshotRepository.findAllByServerIdAndSnapshotTimeBetween(
            serverId, startTime, endTime
        )).thenReturn(listOf(s1, s2, s3))

        val result = serverSnapshotService.getPlayerHistory(ip, port, startTime, endTime)

        assertEquals(2, result.size, "Should have 2 data points")
        
        // Point 1: 10:00-10:15 bucket -> Should have max players = 10
        val p1 = result[0]
        assertEquals(10, p1.playerCount)
        assertEquals(32, p1.maxPlayers)
        // Timestamp should be start of bucket 10:00:00
        assertEquals(startTime.epochSecond / 900 * 900, p1.timestamp.epochSecond)
        
        // Point 2: 10:15-10:30 bucket -> Should have 8 players
        val p2 = result[1]
        assertEquals(8, p2.playerCount)
        assertEquals(32, p2.maxPlayers)
        // Timestamp should be start of bucket 10:15:00
        assertEquals((startTime.epochSecond / 900 + 1) * 900, p2.timestamp.epochSecond)
    }

    private fun createSnapshot(server: Server, time: Instant, players: Int): ServerSnapshot {
        return ServerSnapshot(
            server = server,
            gametype = "ffa",
            mapname = "obj_team2",
            mapurl = "",
            gamever = "1.11",
            numPlayers = players,
            maxPlayers = 32,
            password = "0",
            timelimit = "20",
            fraglimit = "0",
            snapshotTime = time,
            dtUpdated = time.toEpochMilli()
        )
    }
}
