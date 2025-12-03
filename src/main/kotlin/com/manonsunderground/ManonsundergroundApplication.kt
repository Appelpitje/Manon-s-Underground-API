package com.manonsunderground

import com.manonsunderground.config.NetworksApiConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableConfigurationProperties(NetworksApiConfig::class)
@EnableScheduling
@EnableCaching
class ManonsundergroundApplication

fun main(args: Array<String>) {
	runApplication<ManonsundergroundApplication>(*args)
}
