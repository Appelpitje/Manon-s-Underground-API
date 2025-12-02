package com.manonsunderground.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "server_snapshots")
data class ServerSnapshot(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_id", nullable = false)
    val server: Server,
    
    @Column(name = "gametype")
    val gametype: String?,
    
    @Column(name = "mapname")
    val mapname: String?,
    
    @Column(name = "mapurl")
    val mapurl: String?,
    
    @Column(name = "gamever")
    val gamever: String?,
    
    @Column(name = "num_players", nullable = false)
    val numPlayers: Int,
    
    @Column(name = "max_players", nullable = false)
    val maxPlayers: Int,
    
    @Column(name = "password")
    val password: String?,
    
    @Column(name = "timelimit")
    val timelimit: String?,
    
    @Column(name = "fraglimit")
    val fraglimit: String?,
    
    @Column(name = "snapshot_time", nullable = false)
    val snapshotTime: Instant = Instant.now(),
    
    @Column(name = "dt_updated")
    val dtUpdated: Long?,
    
    @OneToMany(mappedBy = "serverSnapshot", cascade = [CascadeType.ALL], orphanRemoval = true)
    val players: MutableList<Player> = mutableListOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ServerSnapshot
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
    
    override fun toString(): String {
        return "ServerSnapshot(id=$id, snapshotTime=$snapshotTime)"
    }
}
