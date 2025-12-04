package com.manonsunderground.service

import com.manonsunderground.repository.ServerRepository
import com.manonsunderground.repository.ServerSnapshotRepository
import com.manonsunderground.util.DnsUtil
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
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
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import javax.imageio.spi.IIORegistry
import kotlin.math.max

@Service
class BannerService(
    private val serverRepository: ServerRepository,
    private val serverSnapshotRepository: ServerSnapshotRepository
) {
    // Cache for base64 encoded map images
    private val mapImageCache = ConcurrentHashMap<String, String>()
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

    @Cacheable(value = ["widgetData"], key = "#ip + ':' + #port")
    @Transactional(readOnly = true)
    fun getWidgetData(ip: String, port: Int): WidgetData {
        // Resolve hostname to IP if needed
        val resolvedIp = try {
            val resolved = DnsUtil.resolveToIp(ip)
            logger.info("DNS resolution: input='$ip', resolved='$resolved'")
            resolved
        } catch (e: Exception) {
            logger.error("Failed to resolve hostname: $ip", e)
            throw RuntimeException("Unable to resolve hostname: $ip", e)
        }
        
        logger.debug("Looking up server in database: ip='$resolvedIp', port=$port")
        val server = serverRepository.findTopByIpAndHostportOrderByIdDesc(resolvedIp, port)
            ?: throw RuntimeException("Server not found for IP: $resolvedIp and port: $port (original input: $ip)")

        val since = Instant.now().minus(24, ChronoUnit.HOURS)
        val snapshots = serverSnapshotRepository.findByServerIdAndSnapshotTimeAfter(server.id!!, since)
            .sortedBy { it.snapshotTime }

        val latestSnapshot = snapshots.lastOrNull()
        val mapName = latestSnapshot?.mapname ?: "unknown"
        
        // Get cached or load map image base64
        val mapImageBase64 = getMapImageBase64(mapName)
        
        val playersList = latestSnapshot?.players ?: emptyList()

        // Determine full game name
        var fullGameName = "Medal of Honor Allied Assault"
        val gn = server.gamename.lowercase()
        if (gn == "mohaas") fullGameName = "Medal of Honor Spearhead"
        else if (gn == "mohaab") fullGameName = "Medal of Honor Breakthrough"

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

        val gameIconBase64 = getGameIconBase64(server.gamename)
        
        // Check for OPM (Open Mohaa)
        val isOpm = latestSnapshot?.gamever?.contains("+") == true
        val opmIconBase64 = if (isOpm) getOpmIconBase64() else null

        return WidgetData(
            serverName = server.hostname,
            ip = ip,
            port = port,
            country = server.country?.uppercase() ?: "UNK",
            gameName = server.gamename,
            gameIconBase64 = gameIconBase64,
            opmIconBase64 = opmIconBase64,
            gameType = fullGameName,
            gameMode = gameMode,
            mapName = mapName,
            mapImageBase64 = mapImageBase64,
            currentPlayers = latestSnapshot?.numPlayers ?: 0,
            maxPlayers = latestSnapshot?.maxPlayers ?: 0,
            players = playersList.map { PlayerData(it.name, it.ping) }
        )
    }
    
    private fun getMapImageBase64(mapName: String): String {
        // Check cache first
        return mapImageCache.getOrPut(mapName) {
            val mapImage = loadMapImage(mapName)
            val baos = ByteArrayOutputStream()
            ImageIO.write(mapImage, "png", baos)
            val mimeType = "image/png"
            "data:$mimeType;base64," + java.util.Base64.getEncoder().encodeToString(baos.toByteArray())
        }
    }

    data class PlayerData(
        val name: String,
        val ping: Int
    )

    data class WidgetData(
        val serverName: String,
        val ip: String,
        val port: Int,
        val country: String,
        val gameName: String,
        val gameIconBase64: String,
        val opmIconBase64: String?,
        val gameType: String,
        val gameMode: String,
        val mapName: String,
        val mapImageBase64: String,
        val currentPlayers: Int,
        val maxPlayers: Int,
        val players: List<PlayerData>
    )

    fun generateBanner(ip: String, port: Int): Pair<ByteArray, String> {
        // Resolve hostname to IP if needed
        val resolvedIp = try {
            val resolved = DnsUtil.resolveToIp(ip)
            logger.info("DNS resolution (banner): input='$ip', resolved='$resolved'")
            resolved
        } catch (e: Exception) {
            logger.error("Failed to resolve hostname: $ip", e)
            throw RuntimeException("Unable to resolve hostname: $ip", e)
        }
        
        logger.debug("Looking up server in database (banner): ip='$resolvedIp', port=$port")
        val server = serverRepository.findTopByIpAndHostportOrderByIdDesc(resolvedIp, port)
            ?: throw RuntimeException("Server not found for IP: $resolvedIp and port: $port (original input: $ip)")

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
            
            // Players Progress Bar
            val current = latestSnapshot?.numPlayers ?: 0
            val max = latestSnapshot?.maxPlayers ?: 0
            
            val label = "Players: "
            g2d.drawString(label, 10, 56)
            val labelWidth = g2d.fontMetrics.stringWidth(label)
            
            val barX = 10 + labelWidth
            val barY = 45
            val barWidth = 100
            val barHeight = 13
            
            // Bar Background
            g2d.color = Color(255, 255, 255, 60)
            g2d.fillRect(barX, barY, barWidth, barHeight)
            
            // Bar Progress
            if (max > 0) {
                val progress = (current.toDouble() / max).coerceIn(0.0, 1.0)
                val progressWidth = (barWidth * progress).toInt()
                g2d.color = Color(0, 255, 0, 180)
                g2d.fillRect(barX, barY, progressWidth, barHeight)
            }
            
            // Bar Border
            g2d.color = Color.WHITE
            g2d.drawRect(barX, barY, barWidth, barHeight)
            
            // Count Text
            val playersText = "$current / $max"
            val textWidth = g2d.fontMetrics.stringWidth(playersText)
            val textX = barX + (barWidth - textWidth) / 2
            g2d.drawString(playersText, textX, 56)
            
            // Reset color for next items
            g2d.color = Color.WHITE
            
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

    private fun getGameIconBase64(gameName: String): String {
        val iconName = when (gameName.lowercase()) {
            "mohaa" -> "AA.png"
            "mohaas" -> "SH.png"
            "mohaab" -> "BT.png"
            else -> "AA.png"
        }
        
        try {
            val resource = ClassPathResource("icons/$iconName")
            if (resource.exists()) {
                val bytes = resource.inputStream.readBytes()
                return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(bytes)
            }
        } catch (e: Exception) {
            logger.warn("Could not load game icon for $gameName", e)
        }
        return ""
    }

    private fun getOpmIconBase64(): String? {
        try {
            val resource = ClassPathResource("icons/OPM.png")
            if (resource.exists()) {
                val bytes = resource.inputStream.readBytes()
                return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(bytes)
            }
        } catch (e: Exception) {
            logger.warn("Could not load OPM icon", e)
        }
        return null
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
