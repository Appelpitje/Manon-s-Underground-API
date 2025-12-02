package com.manonsunderground.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "players")
data class Player(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "server_snapshot_id", nullable = false)
    val serverSnapshot: ServerSnapshot,
    
    @Column(name = "player_name", nullable = false)
    val name: String,
    
    @Column(name = "frags", nullable = false)
    val frags: Int,
    
    @Column(name = "ping", nullable = false)
    val ping: Int,
    
    @Column(name = "snapshot_time", nullable = false)
    val snapshotTime: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Player
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
    
    override fun toString(): String {
        return "Player(id=$id, name='$name', frags=$frags, ping=$ping)"
    }
}
