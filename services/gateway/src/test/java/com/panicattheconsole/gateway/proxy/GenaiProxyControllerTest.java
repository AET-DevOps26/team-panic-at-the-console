package com.panicattheconsole.gateway.proxy;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;

import com.panicattheconsole.gateway.GatewayApplication;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = GatewayApplication.class)
@AutoConfigureMockMvc
@Import(MockDownstreamClientsConfig.class)
class GenaiProxyControllerTest {

    private static final UUID INCIDENT_ID =
            UUID.fromString("018e2c5f-1234-7abc-8def-000000000001");

    @Autowired
    MockMvc mvc;

    @Autowired
    MockRestServiceServer incidentServer;

    @BeforeEach
    void resetServers() {
        incidentServer.reset();
    }

    @Test
    void regenerateSummary_proxiesIncidentService() throws Exception {
        incidentServer
                .expect(
                        requestTo(
                                "http://localhost:8081/incidents/"
                                        + INCIDENT_ID
                                        + "/genai/summary"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withStatus(HttpStatus.ACCEPTED)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(
                                        """
                                        {"accepted":true,"task":"SUMMARY"}
                                        """));

        mvc.perform(post("/incidents/{id}/genai/summary", INCIDENT_ID))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.task").value("SUMMARY"));

        incidentServer.verify();
    }
}
