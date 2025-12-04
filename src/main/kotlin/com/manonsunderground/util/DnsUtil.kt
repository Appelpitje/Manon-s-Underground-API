package com.manonsunderground.util

import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Utility class for DNS resolution
 */
object DnsUtil {
    
    /**
     * Resolves a hostname or IP address to an IP address.
     * If the input is already an IP address, it returns it as-is.
     * If it's a hostname, it performs a DNS lookup and returns the resolved IP.
     * 
     * @param host The hostname or IP address to resolve
     * @return The resolved IP address
     * @throws UnknownHostException if the hostname cannot be resolved
     */
    fun resolveToIp(host: String): String {
        return try {
            // This will work for both IP addresses and hostnames
            // If it's already an IP, it returns the same
            // If it's a hostname, it performs DNS lookup
            InetAddress.getByName(host).hostAddress
        } catch (e: UnknownHostException) {
            throw RuntimeException("Unable to resolve hostname: $host", e)
        }
    }
    
    /**
     * Checks if a string is a valid IP address (IPv4 or IPv6)
     * 
     * @param host The string to check
     * @return true if the string is an IP address, false otherwise
     */
    fun isIpAddress(host: String): Boolean {
        return try {
            val addr = InetAddress.getByName(host)
            // If the original input equals the host address, it's an IP
            // Otherwise, it was a hostname that got resolved
            addr.hostAddress == host
        } catch (e: UnknownHostException) {
            false
        }
    }
}
