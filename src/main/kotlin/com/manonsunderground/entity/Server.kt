package com.manonsunderground.entity

import jakarta.persistence.*

@Entity
@Table(name = "servers")
data class Server(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(name = "server_id", nullable = false)
    val serverId: Int,
    
    @Column(name = "ip", nullable = false)
    val ip: String,
    
    @Column(name = "hostport", nullable = false)
    val hostport: Int,
    
    @Column(name = "hostname", nullable = false)
    val hostname: String,
    
    @Column(name = "gamename", nullable = false)
    val gamename: String,
    
    @Column(name = "country")
    val country: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Server
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
    
    override fun toString(): String {
        return "Server(id=$id, serverId=$serverId, hostname='$hostname', ip='$ip', hostport=$hostport)"
    }
}
