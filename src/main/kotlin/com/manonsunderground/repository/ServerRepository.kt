package com.manonsunderground.repository

import com.manonsunderground.entity.Server
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ServerRepository : JpaRepository<Server, Long> {
    
    /**
     * Find a server by its unique identifier (ip + port + hostname)
     */
    fun findByIpAndHostportAndHostname(ip: String, hostport: Int, hostname: String): Server?

    /**
     * Find a server by ip and hostport
     */
    fun findByIpAndHostport(ip: String, hostport: Int): Server?
}
