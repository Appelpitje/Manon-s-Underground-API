package com.manonsunderground.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.mock.web.MockHttpServletRequest

@SpringBootTest
class CorsIntegrationTest {

    @Autowired
    private lateinit var corsConfigurationSource: CorsConfigurationSource

    @Test
    fun `should have correct configuration for localhost 3000`() {
        val request = MockHttpServletRequest()
        request.scheme = "http"
        request.serverName = "localhost"
        request.serverPort = 3000
        // CorsCallback usually checks against the request origin header if using CorsUriTemplate?
        // But UrlBasedCorsConfigurationSource usually matches by path.
        // And checks the config object itself.
        
        // Let's retrieve the config for a request
        val config = corsConfigurationSource.getCorsConfiguration(request)
        
        // Since we registered for "/**", it should return a config
        assertNotNull(config, "CORS config should not be null")
        
        // Verify allowed origins
        assertTrue(config!!.allowedOrigins!!.contains("http://localhost:3000"), "Should allow localhost:3000")
        assertTrue(config.allowedOrigins!!.contains("https://allied-intel.appelpitje.dev"), "Should allow dev domain")
        
        // Verify allowed methods
        assertTrue(config.allowedMethods!!.contains("*"), "Should allow all methods")
    }
}
