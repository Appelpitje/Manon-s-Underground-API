package com.manonsunderground.service

import com.manonsunderground.model.MapFrequency
import com.manonsunderground.repository.ServerSnapshotRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class StatisticsService(
    private val serverSnapshotRepository: ServerSnapshotRepository
) {
    
    private val mapCodeToName = mapOf(
        // Medal of Honor: Allied Assault
        "dm/mohdm1" to "Southern France",
        "dm/mohdm2" to "Destroyed Village",
        "dm/mohdm3" to "Remagen",
        "dm/mohdm4" to "The Crossroads",
        "dm/mohdm5" to "Snowy Park",
        "dm/mohdm6" to "Stalingrad",
        "dm/mohdm7" to "Algiers",
        "obj/obj_team1" to "The Hunt",
        "obj/obj_team2" to "V2 Rocket Facility",
        "obj/obj_team3" to "Omaha Beach",
        "obj/obj_team4" to "The Bridge",

        // Medal of Honor: Spearhead
        "mp_bahnhof_dm" to "Bahnhof",
        "mp_bazaar_dm" to "Bazaar",
        "mp_brest_dm" to "Brest",
        "mp_gewitter_dm" to "Gewitter",
        "mp_holland_dm" to "Holland",
        "mp_malta_dm" to "Malta",
        "mp_stadt_dm" to "Stadt",
        "mp_unterseite_dm" to "Unterseite",
        "mp_verschneit_dm" to "Verschneit",
        "mp_ardennes_tow" to "Ardennes",
        "mp_berlin_tow" to "Berlin",
        "mp_druckkammern_tow" to "Druckkammern",
        "mp_flughafen_tow" to "Flughafen",

        // Medal of Honor: Breakthrough
        "mp_anzio_lib" to "Anzio",
        "mp_bizerteharbor_lib" to "Bizerte Harbor",
        "mp_ship_lib" to "Ship (Stuckguter)",
        "mp_tunisia_lib" to "Tunisia",
        "mp_bizertefort_obj" to "Bizerte Fort",
        "mp_bologna_obj" to "Bologna",
        "mp_castello_obj" to "Castello",
        "mp_palermo_obj" to "Palermo",
        "mp_kasserline_tow" to "Kasserine Pass",
        "mp_montebattaglia_tow" to "Monte Battaglia",
        "mp_montecassino_tow" to "Monte Cassino"
    )

    fun getMostPlayedMaps24h(): Map<String, MapFrequency?> {
        val yesterday = Instant.now().minus(24, ChronoUnit.HOURS)
        
        val mostPlayedMohaa = enrichWithFullName(serverSnapshotRepository.findMostPlayedMapsAfter(yesterday, "mohaa").firstOrNull())
        val mostPlayedMohaas = enrichWithFullName(serverSnapshotRepository.findMostPlayedMapsAfter(yesterday, "mohaas").firstOrNull())
        val mostPlayedMohaab = enrichWithFullName(serverSnapshotRepository.findMostPlayedMapsAfter(yesterday, "mohaab").firstOrNull())
        val mostPlayedAll = enrichWithFullName(serverSnapshotRepository.findMostPlayedMapsAfter(yesterday, null).firstOrNull())
        
        return mapOf(
            "mohaa" to mostPlayedMohaa,
            "mohaas" to mostPlayedMohaas,
            "mohaab" to mostPlayedMohaab,
            "all" to mostPlayedAll
        )
    }

    private fun enrichWithFullName(frequency: MapFrequency?): MapFrequency? {
        if (frequency == null || frequency.mapname == null) return frequency
        
        val strictMatch = mapCodeToName[frequency.mapname]
        val fuzzyMatch = strictMatch ?: mapCodeToName.entries.find { it.key.contains(frequency.mapname, ignoreCase = true) }?.value
        
        return frequency.copy(mapFullName = fuzzyMatch)
    }
}
