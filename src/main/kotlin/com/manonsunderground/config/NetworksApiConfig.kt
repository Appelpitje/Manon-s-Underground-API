package com.manonsunderground.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "networks.api")
data class NetworksApiConfig(
    var baseUrl: String = "https://master.333networks.com/json",
    var defaultResultsPerPage: Int = 50,
    var minUpdateIntervalMinutes: Double = 7.5,
    var userAgent: String = "Manon's Underground API - Data from 333networks"
)
