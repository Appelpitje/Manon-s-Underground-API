package com.manonsunderground.controller

import com.manonsunderground.model.MapFrequency
import com.manonsunderground.service.StatisticsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/statistics")
class StatisticsController(
    private val statisticsService: StatisticsService
) {
    
    @GetMapping("/most-played-maps")
    fun getMostPlayedMaps24h(): Map<String, MapFrequency?> {
        return statisticsService.getMostPlayedMaps24h()
    }
}
