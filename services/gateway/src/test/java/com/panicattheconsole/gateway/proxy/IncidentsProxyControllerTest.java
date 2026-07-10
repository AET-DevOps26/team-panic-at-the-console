package com.panicattheconsole.gateway.proxy;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;

import com.panicattheconsole.gateway.GatewayApplication;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = GatewayApplication.class)
@AutoConfigureMockMvc
@Import(MockDownstreamClientsConfig.class)
class IncidentsProxyControllerTest {

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
    void listIncidents_proxiesIncidentService() throws Exception {
        incidentServer
                .expect(requestTo("http://localhost:8081/incidents?page=0&size=10"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(
                                        """
                                        {"items":[],"total":0,"page":0,"size":10}
                                        """));

        mvc.perform(get("/incidents").queryParam("page", "0").queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));

        incidentServer.verify();
    }

    @Test
    void getIncident_proxiesIncidentService() throws Exception {
        incidentServer
                .expect(requestTo("http://localhost:8081/incidents/" + INCIDENT_ID))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(
                                        """
                                        {
                                          "id":"018e2c5f-1234-7abc-8def-000000000001",
                                          "title":"Checkout spike",
                                          "status":"open",
                                          "severity":"SEV2",
                                          "createdAt":"2026-05-08T10:00:00Z"
                                        }
                                        """));

        mvc.perform(get("/incidents/{id}", INCIDENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Checkout spike"));

        incidentServer.verify();
    }

    @Test
    void updateIncidentDescription_proxiesIncidentService() throws Exception {
        incidentServer
                .expect(requestTo("http://localhost:8081/incidents/" + INCIDENT_ID + "/description"))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(
                                        """
                                        {
                                          "id":"018e2c5f-1234-7abc-8def-000000000001",
                                          "title":"Checkout spike",
                                          "description":"Rollback in progress",
                                          "status":"open",
                                          "severity":"SEV2",
                                          "createdAt":"2026-05-08T10:00:00Z"
                                        }
                                        """));

        mvc.perform(
                        patch("/incidents/{id}/description", INCIDENT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"description\":\"Rollback in progress\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Rollback in progress"));

        incidentServer.verify();
    }
}
