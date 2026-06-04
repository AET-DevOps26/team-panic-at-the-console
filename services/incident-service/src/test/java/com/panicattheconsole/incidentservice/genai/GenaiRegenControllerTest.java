package com.panicattheconsole.incidentservice.genai;

import com.panicattheconsole.incidentservice.incident.IncidentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GenaiRegenController.class)
class GenaiRegenControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    IncidentService incidentService;

    private static final String INCIDENT_ID = "018e2c5f-1234-7abc-8def-000000000001";

    @Test
    void summary_returns202() throws Exception {
        mvc.perform(post("/incidents/{id}/genai/summary", INCIDENT_ID))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.accepted").value(true))
                .andExpect(jsonPath("$.task").value("SUMMARY"));
    }

    @Test
    void severity_returns202() throws Exception {
        mvc.perform(post("/incidents/{id}/genai/severity", INCIDENT_ID))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.task").value("SEVERITY_SUGGESTION"));
    }

    @Test
    void solutions_returns202() throws Exception {
        mvc.perform(post("/incidents/{id}/genai/solutions", INCIDENT_ID))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.task").value("SOLUTION_SUGGESTIONS"));
    }

    @Test
    void postmortem_returns202() throws Exception {
        mvc.perform(post("/incidents/{id}/genai/postmortem", INCIDENT_ID))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.task").value("POSTMORTEM"));
    }

        @Test
        void summary_returns404_whenNotFound() throws Exception {
        org.mockito.Mockito.doThrow(new NoSuchElementException("incident missing"))
            .when(incidentService)
            .requestRegeneration(
                    java.util.UUID.fromString(INCIDENT_ID),
                    org.openapitools.model.RegenAccepted.TaskEnum.SUMMARY);

        mvc.perform(post("/incidents/{id}/genai/summary", INCIDENT_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("incident missing"));
        }

        @Test
        void postmortem_returns409_whenNotAllowed() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalStateException("not allowed"))
            .when(incidentService).requestPostmortemRegeneration(java.util.UUID.fromString(INCIDENT_ID));

        mvc.perform(post("/incidents/{id}/genai/postmortem", INCIDENT_ID))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("CONFLICT"))
            .andExpect(jsonPath("$.message").value("not allowed"));
        }
}
