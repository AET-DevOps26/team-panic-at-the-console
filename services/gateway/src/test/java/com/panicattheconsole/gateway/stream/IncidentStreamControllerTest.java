package com.panicattheconsole.gateway.stream;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.panicattheconsole.gateway.GatewayApplication;
import com.panicattheconsole.gateway.auth.TestSessions;
import com.panicattheconsole.gateway.proxy.MockDownstreamClientsConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = GatewayApplication.class)
@AutoConfigureMockMvc
// The stream itself needs no downstream clients, but the component-scanned
// proxy controllers do; tests disable the real ones (see test application.properties).
@Import(MockDownstreamClientsConfig.class)
class IncidentStreamControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    IncidentStreamBroadcaster broadcaster;

    @Test
    void stream_opensImmediatelyWithSseHeaders() throws Exception {
        MvcResult result = mvc.perform(get("/incidents/stream").cookie(TestSessions.sessionCookie()))
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk())
                .andExpect(header().string("X-Accel-Buffering", "no"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn();

        // The initial comment commits the response so EventSource fires onopen.
        assertThat(result.getResponse().getContentAsString()).contains(":connected");
    }

    @Test
    void stream_forwardsIncidentEventsAsEnvelopes() throws Exception {
        MvcResult result = mvc.perform(get("/incidents/stream").cookie(TestSessions.sessionCookie()))
                .andExpect(request().asyncStarted())
                .andReturn();

        broadcaster.broadcast(
                "incident.comment.added",
                """
                {"incidentId":"018e2c5f-1234-7abc-8def-000000000001","timestamp":"2026-07-06T10:00:00Z"}
                """.getBytes(StandardCharsets.UTF_8));

        assertThat(result.getResponse().getContentAsString())
                .contains("data:{\"type\":\"incident.comment.added\","
                        + "\"incidentId\":\"018e2c5f-1234-7abc-8def-000000000001\"}");
    }

    @Test
    void stream_forwardsEnvelopeWithoutIncidentIdOnUnparsablePayload() throws Exception {
        MvcResult result = mvc.perform(get("/incidents/stream").cookie(TestSessions.sessionCookie()))
                .andExpect(request().asyncStarted())
                .andReturn();

        broadcaster.broadcast("incident.updated", "not-json".getBytes(StandardCharsets.UTF_8));

        assertThat(result.getResponse().getContentAsString())
                .contains("data:{\"type\":\"incident.updated\",\"incidentId\":null}");
    }
}
