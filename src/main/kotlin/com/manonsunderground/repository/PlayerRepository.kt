package com.manonsunderground.repository

import com.manonsunderground.entity.Player
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface PlayerRepository : JpaRepository<Player, Long> {
    
    /**
     * Find all players after a specific snapshot time
     */
    fun findAllBySnapshotTimeAfter(snapshotTime: Instant): List<Player>
    
    /**
     * Find all players for a specific server snapshot
     */
    fun findAllByServerSnapshotId(serverSnapshotId: Long): List<Player>
}
