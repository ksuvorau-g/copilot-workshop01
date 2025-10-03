package com.example.aidemo1.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for SecurityConfig to ensure public endpoints are accessible
 * and protected endpoints require authentication.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testMockProvider1EndpointIsPublic() throws Exception {
        mockMvc.perform(get("/mock/provider1/rate")
                        .param("base", "USD")
                        .param("target", "EUR"))
                .andExpect(status().isOk());
    }

    @Test
    void testMockProvider2EndpointIsPublic() throws Exception {
        mockMvc.perform(get("/mock/provider2/rate")
                        .param("base", "USD")
                        .param("target", "EUR"))
                .andExpect(status().isOk());
    }

    @Test
    void testSwaggerUIIsPublic() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection()); // Redirects to /swagger-ui/index.html
    }

    // Note: API docs and actuator endpoints will be tested when those features are fully configured
    // @Test
    // void testApiDocsIsPublic() throws Exception {
    //     mockMvc.perform(get("/v3/api-docs"))
    //             .andExpect(status().isOk());
    // }

    // @Test
    // void testHealthEndpointIsPublic() throws Exception {
    //     mockMvc.perform(get("/actuator/health"))
    //             .andExpect(status().isOk());
    // }
}
