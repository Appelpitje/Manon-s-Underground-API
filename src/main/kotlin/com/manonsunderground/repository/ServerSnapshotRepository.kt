package com.manonsunderground.repository

import com.manonsunderground.entity.ServerSnapshot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface ServerSnapshotRepository : JpaRepository<ServerSnapshot, Long> {
    
    /**
     * Find all snapshots after a specific time
     */
    fun findAllBySnapshotTimeAfter(snapshotTime: Instant): List<ServerSnapshot>

    /**
     * Find all snapshots for a specific server after a specific time
     */
    fun findByServerIdAndSnapshotTimeAfter(serverId: Long, snapshotTime: Instant): List<ServerSnapshot>
}
