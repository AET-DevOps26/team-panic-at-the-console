package com.panicattheconsole.eventservice.event;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.panicattheconsole.eventservice.nats.NatsIncidentEventConsumer;

import io.nats.client.Connection;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class EventsControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    // NATS not available in tests — mock the connection and consumer
    @MockitoBean
    Connection natsConnection;

    @MockitoBean
    NatsIncidentEventConsumer natsIncidentEventConsumer;

    @Autowired
    MockMvc mvc;

    @Autowired
    IncidentEventService eventService;

    @Test
    void listIncidentEvents_returnsEventsInChronologicalOrder() throws Exception {
        UUID incidentId = UUID.randomUUID();
        Instant t1 = Instant.parse("2026-01-01T10:00:00Z");
        Instant t2 = Instant.parse("2026-01-01T11:00:00Z");

        eventService.append(incidentId, t1, "incident_created", "Incident created");
        eventService.append(incidentId, t2, "severity_escalated", "Severity escalated to SEV2");

        mvc.perform(get("/incidents/{id}/events", incidentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("incident_created"))
                .andExpect(jsonPath("$[1].type").value("severity_escalated"))
                .andExpect(jsonPath("$[1].description").value("Severity escalated to SEV2"));
    }

    @Test
    void listIncidentEvents_returnsEmptyListForUnknownIncident() throws Exception {
        mvc.perform(get("/incidents/{id}/events", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
