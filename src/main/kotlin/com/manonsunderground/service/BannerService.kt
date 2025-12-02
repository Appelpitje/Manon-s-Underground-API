package com.manonsunderground.service

import com.manonsunderground.repository.ServerRepository
import com.manonsunderground.repository.ServerSnapshotRepository
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.imageio.ImageIO
import javax.imageio.spi.IIORegistry
import kotlin.math.max

@Service
class BannerService(
    private val serverRepository: ServerRepository,
    private val serverSnapshotRepository: ServerSnapshotRepository
) {
    private val logger = LoggerFactory.getLogger(BannerService::class.java)

    @PostConstruct
    fun init() {
        try {
            val registry = IIORegistry.getDefaultInstance()
            
            // Register Writer
            val writerSpiClass = Class.forName("com.twelvemonkeys.imageio.plugins.webp.WebPImageWriterSpi")
            val writerSpi = writerSpiClass.getDeclaredConstructor().newInstance()
            registry.registerServiceProvider(writerSpi)
            
            // Register Reader
            val readerSpiClass = Class.forName("com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi")
            val readerSpi = readerSpiClass.getDeclaredConstructor().newInstance()
            registry.registerServiceProvider(readerSpi)
            
            logger.info("Manually registered WebP ImageIO SPIs")
        } catch (e: Exception) {
            logger.warn("Could not manually register WebP SPIs: ${e.message}")
        }
    }

    @Transactional(readOnly = true)
    fun getWidgetData(ip: String, port: Int): WidgetData {
        val server = serverRepository.findByIpAndHostport(ip, port)
            ?: throw RuntimeException("Server not found")

        val since = Instant.now().minus(24, ChronoUnit.HOURS)
        val snapshots = serverSnapshotRepository.findByServerIdAndSnapshotTimeAfter(server.id!!, since)
            .sortedBy { it.snapshotTime }

        val latestSnapshot = snapshots.lastOrNull()
        val mapName = latestSnapshot?.mapname ?: "unknown"
        val mapImage = loadMapImage(mapName)
        val playersList = latestSnapshot?.players ?: emptyList()

        // Convert map image to base64
        val baos = ByteArrayOutputStream()
        // Force PNG for reliability
        ImageIO.write(mapImage, "png", baos)
        val mimeType = "image/png"
        val mapImageBase64 = java.util.Base64.getEncoder().encodeToString(baos.toByteArray())

        // Determine full game name
        var fullGameName = "Medal of Honor Allied Assault"
        val gn = server.gamename.lowercase()
        if (gn == "mohsh") fullGameName = "Medal of Honor Spearhead"
        else if (gn == "mohbt") fullGameName = "Medal of Honor Breakthrough"
        else if (gn == "cod") fullGameName = "Call of Duty"
        else if (gn == "cod2") fullGameName = "Call of Duty 2"

        // Determine Game Mode
        val rawGametype = latestSnapshot?.gametype?.lowercase() ?: "dm"
        val gameMode = when {
            rawGametype.contains("obj") -> "Objective"
            rawGametype.contains("tow") -> "Tug of War"
            rawGametype.contains("tdm") -> "Team Deathmatch"
            rawGametype.contains("dm") -> "Free-For-All"
            rawGametype.contains("lib") -> "Liberation"
            rawGametype.contains("ft") -> "Freeze Tag"
            rawGametype.contains("sd") -> "Search & Destroy"
            else -> rawGametype.uppercase()
        }

        return WidgetData(
            serverName = server.hostname,
            ip = ip,
            port = port,
            country = server.country?.uppercase() ?: "UNK",
            gameType = fullGameName,
            gameMode = gameMode,
            mapName = mapName,
            mapImageBase64 = "data:$mimeType;base64,$mapImageBase64",
            currentPlayers = latestSnapshot?.numPlayers ?: 0,
            maxPlayers = latestSnapshot?.maxPlayers ?: 0,
            players = playersList.map { it.name }
        )
    }

    data class WidgetData(
        val serverName: String,
        val ip: String,
        val port: Int,
        val country: String,
        val gameType: String,
        val gameMode: String,
        val mapName: String,
        val mapImageBase64: String,
        val currentPlayers: Int,
        val maxPlayers: Int,
        val players: List<String>
    )

    fun generateBanner(ip: String, port: Int): Pair<ByteArray, String> {
        val server = serverRepository.findByIpAndHostport(ip, port)
            ?: throw RuntimeException("Server not found")

        val since = Instant.now().minus(24, ChronoUnit.HOURS)
        val snapshots = serverSnapshotRepository.findByServerIdAndSnapshotTimeAfter(server.id!!, since)
            .sortedBy { it.snapshotTime }

        val latestSnapshot = snapshots.lastOrNull()
        val mapName = latestSnapshot?.mapname ?: "unknown"
        val mapImage = loadMapImage(mapName)

        // Fixed banner size 560x95
        val width = 560
        val height = 95
        val banner = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g2d = banner.createGraphics()

        try {
            // Draw map image (cover/crop to fill)
            val imgWidth = mapImage.width
            val imgHeight = mapImage.height
            val scale = max(width.toDouble() / imgWidth, height.toDouble() / imgHeight)
            val scaledWidth = (imgWidth * scale).toInt()
            val scaledHeight = (imgHeight * scale).toInt()
            val x = (width - scaledWidth) / 2
            val y = (height - scaledHeight) / 2
            
            g2d.drawImage(mapImage, x, y, scaledWidth, scaledHeight, null)

            // Dark overlay for readability
            g2d.color = Color(0, 0, 0, 150)
            g2d.fillRect(0, 0, width, height)

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

            // Left Side Text
            g2d.color = Color.WHITE
            
            // Server Name
            g2d.font = Font("SansSerif", Font.BOLD, 14)
            g2d.drawString(server.hostname, 10, 20)

            // IP:Port
            g2d.font = Font("SansSerif", Font.PLAIN, 11)
            g2d.drawString("$ip:$port", 10, 38)
            
            // Players
            val players = "${latestSnapshot?.numPlayers ?: 0} / ${latestSnapshot?.maxPlayers ?: 0}"
            g2d.drawString("Players: $players", 10, 56)
            
            // Map
            g2d.drawString("Map: $mapName", 10, 74)

            // Allied Intel Branding (Top Right)
            g2d.font = Font("SansSerif", Font.BOLD, 12)
            val brandText = "ALLIED INTEL"
            val brandWidth = g2d.fontMetrics.stringWidth(brandText)
            g2d.drawString(brandText, width - brandWidth - 10, 20)

            // Graph (Right Side)
            val graphWidth = 200
            val graphHeight = 60
            val graphX = width - graphWidth - 10
            val graphY = 28 // below branding
            
            drawGraph(g2d, snapshots, graphX, graphY, graphWidth, graphHeight)

        } finally {
            g2d.dispose()
        }

        // Write to WebP
        val baos = ByteArrayOutputStream()
        val writers = ImageIO.getImageWritersByFormatName("webp")
        
        if (writers.hasNext()) {
            val success = ImageIO.write(banner, "webp", baos)
            if (success) {
                return Pair(baos.toByteArray(), "image/webp")
            }
            logger.error("Failed to write WebP image even though writer was found.")
        } else {
            logger.warn("No WebP writer found! Available formats: ${ImageIO.getWriterFormatNames().joinToString()}")
        }
        
        // Fallback to PNG
        logger.info("Falling back to PNG format")
        ImageIO.write(banner, "png", baos)
        return Pair(baos.toByteArray(), "image/png")
    }

    private fun loadMapImage(mapName: String): BufferedImage {
        // Sanitize map name (remove path if present, e.g. "obj/obj_team2" -> "obj_team2")
        val cleanMapName = mapName.substringAfterLast('/')
        val resourcePath = "images/maps/$cleanMapName.webp"
        try {
            val resource = ClassPathResource(resourcePath)
            if (resource.exists()) {
                return ImageIO.read(resource.inputStream)
            }
        } catch (e: Exception) {
            logger.warn("Could not load map image for $cleanMapName", e)
        }
        
        // Fallback image
        val fallback = BufferedImage(560, 95, BufferedImage.TYPE_INT_RGB)
        val g = fallback.createGraphics()
        g.color = Color.DARK_GRAY
        g.fillRect(0, 0, 560, 95)
        g.dispose()
        return fallback
    }

    private fun drawGraph(
        g2d: Graphics2D, 
        snapshots: List<com.manonsunderground.entity.ServerSnapshot>, 
        x: Int, y: Int, w: Int, h: Int
    ) {
        // Background for graph
        g2d.color = Color(0, 0, 0, 100)
        g2d.fillRect(x, y, w, h)
        g2d.color = Color.WHITE
        g2d.drawRect(x, y, w, h)

        if (snapshots.isEmpty()) return

        val now = Instant.now()
        val start = now.minus(24, ChronoUnit.HOURS)
        val totalTime = 24 * 60 * 60 * 1000L // 24h in ms

        val maxPlayers = snapshots.maxOfOrNull { it.maxPlayers } ?: 32
        val yMax = if (maxPlayers == 0) 32 else maxPlayers
        
        g2d.color = Color.GREEN
        
        var prevX = -1
        var prevY = -1

        for (snapshot in snapshots) {
            val timeDiff = java.time.Duration.between(start, snapshot.snapshotTime).toMillis()
            val normalizedX = (timeDiff.toDouble() / totalTime.toDouble() * w).toInt()
            val px = x + normalizedX
            
            val normalizedY = (snapshot.numPlayers.toDouble() / yMax.toDouble() * h).toInt()
            val py = (y + h) - normalizedY
            
            val safePx = px.coerceIn(x, x + w)
            val safePy = py.coerceIn(y, y + h)

            if (prevX != -1) {
                g2d.drawLine(prevX, prevY, safePx, safePy)
            }
            
            prevX = safePx
            prevY = safePy
        }
    }
}
