package com.panicattheconsole.eventservice.event;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.openapitools.model.IncidentEvent;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventsController.class)
class EventsControllerIntegrationTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    IncidentEventService eventService;

    @Test
    void listIncidentEvents_returnsEventsInChronologicalOrder() throws Exception {
        UUID incidentId = UUID.randomUUID();
        OffsetDateTime t1 = OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime t2 = OffsetDateTime.of(2026, 1, 1, 11, 0, 0, 0, ZoneOffset.UTC);

        when(eventService.listForIncident(incidentId)).thenReturn(List.of(
                new IncidentEvent(t1, "incident_created", "Incident created"),
                new IncidentEvent(t2, "severity_escalated", "Severity escalated to SEV2")));

        mvc.perform(get("/incidents/{id}/events", incidentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("incident_created"))
                .andExpect(jsonPath("$[1].type").value("severity_escalated"))
                .andExpect(jsonPath("$[1].description").value("Severity escalated to SEV2"));
    }

    @Test
    void listIncidentEvents_returnsEmptyListForUnknownIncident() throws Exception {
        UUID incidentId = UUID.randomUUID();
        when(eventService.listForIncident(incidentId)).thenReturn(List.of());

        mvc.perform(get("/incidents/{id}/events", incidentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
