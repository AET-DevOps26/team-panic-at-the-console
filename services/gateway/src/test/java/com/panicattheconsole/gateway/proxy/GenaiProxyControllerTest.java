package com.panicattheconsole.gateway.proxy;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Autowired
    MockRestServiceServer genaiServer;

    @BeforeEach
    void resetServers() {
        incidentServer.reset();
        genaiServer.reset();
    }

    @Test
    void genaiHealth_proxiesGenaiService() throws Exception {
        genaiServer
                .expect(requestTo("http://localhost:8087/api/v1/genai/ollama/health"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(
                                        """
                                        {"status":"ok","ollamaReachable":true,"model":"qwen2.5:3b"}
                                        """));

        mvc.perform(get("/genai/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.ollamaReachable").value(true));

        genaiServer.verify();
    }

    @Test
    void genaiHealth_forwards503WhenOllamaUnreachable() throws Exception {
        genaiServer
                .expect(requestTo("http://localhost:8087/api/v1/genai/ollama/health"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(
                                        """
                                        {"status":"degraded","ollamaReachable":false,"model":"qwen2.5:3b"}
                                        """));

        mvc.perform(get("/genai/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("degraded"))
                .andExpect(jsonPath("$.ollamaReachable").value(false));

        genaiServer.verify();
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
