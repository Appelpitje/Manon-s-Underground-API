package com.manonsunderground

import com.manonsunderground.config.NetworksApiConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(NetworksApiConfig::class)
class ManonsundergroundApplication

fun main(args: Array<String>) {
	runApplication<ManonsundergroundApplication>(*args)
}
