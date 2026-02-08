package com.manonsunderground.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ServerInfoParsingTest {

    private val objectMapper = ObjectMapper().registerKotlinModule()

    @Test
    fun `should parse ServerInfo with queryport and unknown fields`() {
        val json = """
            {
                "id": 1,
                "ip": "127.0.0.1",
                "hostport": 12203,
                "queryport": 12204,
                "hostname": "Test Server",
                "gamename": "mohaa",
                "gametype": "ffa",
                "label": "test",
                "country": "US",
                "numplayers": 5,
                "maxplayers": 32,
                "maptitle": "V2 Rocket Facility",
                "mapname": "obj_team2",
                "dt_added": 1600000000,
                "dt_updated": 1600001000,
                "some_new_field": "unknown_value"
            }
        """.trimIndent()

        val serverInfo = objectMapper.readValue(json, ServerInfo::class.java)

        assertEquals(1, serverInfo.id)
        assertEquals("127.0.0.1", serverInfo.ip)
        assertEquals(12204, serverInfo.queryport)
        assertEquals("mohaa", serverInfo.gamename)
        assertEquals(1600000000L, serverInfo.dtAdded)
    }

    @Test
    fun `should parse ServerInfo when queryport is missing`() {
        val json = """
            {
                "id": 2,
                "ip": "192.168.1.1",
                "hostport": 12203,
                "hostname": "Another Server",
                "gamename": "mohaa",
                "numplayers": 0,
                "maxplayers": 20,
                "dt_added": 1600000000,
                "dt_updated": 1600001000
            }
        """.trimIndent()

        val serverInfo = objectMapper.readValue(json, ServerInfo::class.java)

        assertEquals(2, serverInfo.id)
        assertNull(serverInfo.queryport)
    }
}
