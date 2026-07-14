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

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = GatewayApplication.class)
@AutoConfigureMockMvc
@Import(MockDownstreamClientsConfig.class)
class WebhookSourcesProxyControllerTest {

    private static final String LIST_JSON = """
            {
              "items":[{
                "slug":"github",
                "createdAt":"2026-07-01T10:00:00Z",
                "lastEventAt":"2026-07-02T08:30:00Z"
              }]
            }
            """;

    private static final String CREATED_JSON = """
            {
              "slug":"grafana",
              "secret":"6bc1bee22e409f96e93d7e117393172aad4c8f10b0e6371b2b647a2f45c7c463",
              "createdAt":"2026-07-01T10:00:00Z"
            }
            """;

    @Autowired
    MockMvc mvc;

    @Autowired
    MockRestServiceServer webhookServer;

    @BeforeEach
    void resetServers() {
        webhookServer.reset();
    }

    @Test
    void listWebhookSources_proxiesWithIdentityHeader() throws Exception {
        webhookServer
                .expect(requestTo("http://localhost:8086/webhook-sources"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-User-Id", TestSessions.USER_ID))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(LIST_JSON));

        mvc.perform(get("/webhook-sources").cookie(TestSessions.sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].slug").value("github"));

        webhookServer.verify();
    }

    @Test
    void createWebhookSource_forwardsBodyAndPassesSecretThrough() throws Exception {
        webhookServer
                .expect(requestTo("http://localhost:8086/webhook-sources"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"slug\":\"grafana\"}"))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(CREATED_JSON));

        mvc.perform(post("/webhook-sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"grafana\"}")
                        .cookie(TestSessions.sessionCookie()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.secret").isNotEmpty());

        webhookServer.verify();
    }

    @Test
    void rotateSecret_proxiesAndPropagatesNotFound() throws Exception {
        webhookServer
                .expect(requestTo("http://localhost:8086/webhook-sources/grafana/rotate-secret"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        mvc.perform(post("/webhook-sources/grafana/rotate-secret").cookie(TestSessions.sessionCookie()))
                .andExpect(status().isNotFound());

        webhookServer.verify();
    }

    @Test
    void deleteWebhookSource_proxiesNoContent() throws Exception {
        webhookServer
                .expect(requestTo("http://localhost:8086/webhook-sources/grafana"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("X-User-Id", TestSessions.USER_ID))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        mvc.perform(delete("/webhook-sources/grafana").cookie(TestSessions.sessionCookie()))
                .andExpect(status().isNoContent());

        webhookServer.verify();
    }

    @Test
    void webhookSources_requireSession() throws Exception {
        mvc.perform(get("/webhook-sources")).andExpect(status().isUnauthorized());
        mvc.perform(post("/webhook-sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"grafana\"}"))
                .andExpect(status().isUnauthorized());
    }
}
