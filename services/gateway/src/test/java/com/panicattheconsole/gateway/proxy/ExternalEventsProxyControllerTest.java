package com.panicattheconsole.gateway.proxy;

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
import com.panicattheconsole.gateway.auth.TestSessions;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = GatewayApplication.class)
@AutoConfigureMockMvc
@Import(MockDownstreamClientsConfig.class)
class ExternalEventsProxyControllerTest {

    private static final String EVENT_ID = "018e2c5f-1234-7abc-8def-0000000000e1";

    private static final String LIST_JSON = """
            {
              "items":[{
                "id":"%s",
                "source":"github",
                "eventType":"ci_failure",
                "deliveryId":"delivery-1",
                "receivedAt":"2026-07-01T10:00:00Z",
                "publishedAt":"2026-07-01T10:00:01Z"
              }],
              "total":1,
              "page":0,
              "size":50
            }
            """.formatted(EVENT_ID);

    private static final String DETAIL_JSON = """
            {
              "id":"%s",
              "source":"github",
              "eventType":"ci_failure",
              "receivedAt":"2026-07-01T10:00:00Z",
              "rawPayload":{"action":"completed"}
            }
            """.formatted(EVENT_ID);

    @Autowired
    MockMvc mvc;

    @Autowired
    MockRestServiceServer webhookServer;

    @BeforeEach
    void resetServers() {
        webhookServer.reset();
    }

    @Test
    void listExternalEvents_proxiesWithFiltersAndIdentityHeader() throws Exception {
        webhookServer
                .expect(requestTo("http://localhost:8086/external-events?source=github&page=0&size=50"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-User-Id", TestSessions.USER_ID))
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(LIST_JSON));

        mvc.perform(get("/external-events")
                        .queryParam("source", "github")
                        .cookie(TestSessions.sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(EVENT_ID))
                .andExpect(jsonPath("$.items[0].eventType").value("ci_failure"));

        webhookServer.verify();
    }

    @Test
    void getExternalEvent_proxiesDetailWithRawPayload() throws Exception {
        webhookServer
                .expect(requestTo("http://localhost:8086/external-events/" + EVENT_ID))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-User-Id", TestSessions.USER_ID))
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(DETAIL_JSON));

        mvc.perform(get("/external-events/{id}", EVENT_ID).cookie(TestSessions.sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rawPayload.action").value("completed"));

        webhookServer.verify();
    }

    @Test
    void getExternalEvent_propagatesNotFound() throws Exception {
        webhookServer
                .expect(requestTo("http://localhost:8086/external-events/" + EVENT_ID))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        mvc.perform(get("/external-events/{id}", EVENT_ID).cookie(TestSessions.sessionCookie()))
                .andExpect(status().isNotFound());

        webhookServer.verify();
    }

    @Test
    void externalEvents_requireSession() throws Exception {
        mvc.perform(get("/external-events")).andExpect(status().isUnauthorized());
    }
}
