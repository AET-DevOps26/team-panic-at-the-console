package com.panicattheconsole.gateway.auth;

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
import com.panicattheconsole.gateway.proxy.MockDownstreamClientsConfig;

import jakarta.servlet.http.Cookie;

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
class SessionAuthFilterTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    MockRestServiceServer incidentServer;

    @BeforeEach
    void resetServers() {
        incidentServer.reset();
    }

    @Test
    void protectedRoute_withoutCookieReturns401() throws Exception {
        mvc.perform(get("/incidents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Not authenticated"));
    }

    @Test
    void protectedRoute_withInvalidTokenReturns401() throws Exception {
        mvc.perform(get("/incidents").cookie(new Cookie("session", "garbage.token.value")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_withExpiredTokenReturns401() throws Exception {
        mvc.perform(get("/incidents").cookie(new Cookie("session", TestSessions.expiredToken())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_withValidSessionInjectsIdentityHeaders() throws Exception {
        incidentServer
                .expect(requestTo("http://localhost:8081/incidents?page=0&size=10"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(IdentityHeaderRelay.USER_ID_HEADER, TestSessions.USER_ID))
                .andExpect(header(IdentityHeaderRelay.USER_ROLE_HEADER, TestSessions.ROLE))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"items\":[],\"total\":0,\"page\":0,\"size\":10}"));

        mvc.perform(get("/incidents")
                        .queryParam("page", "0").queryParam("size", "10")
                        .cookie(TestSessions.sessionCookie()))
                .andExpect(status().isOk());

        incidentServer.verify();
    }

    @Test
    void spoofedIdentityHeaders_areNotForwardedWithoutSession() throws Exception {
        // Inbound X-User-Id must never reach a downstream service: identity only
        // comes from the validated session cookie.
        mvc.perform(get("/incidents").header(IdentityHeaderRelay.USER_ID_HEADER, "attacker"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void spoofedIdentityHeaders_areOverriddenByValidatedSession() throws Exception {
        incidentServer
                .expect(requestTo("http://localhost:8081/incidents?page=0&size=10"))
                .andExpect(header(IdentityHeaderRelay.USER_ID_HEADER, TestSessions.USER_ID))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"items\":[],\"total\":0,\"page\":0,\"size\":10}"));

        mvc.perform(get("/incidents")
                        .queryParam("page", "0").queryParam("size", "10")
                        .cookie(TestSessions.sessionCookie())
                        .header(IdentityHeaderRelay.USER_ID_HEADER, "attacker"))
                .andExpect(status().isOk());

        incidentServer.verify();
    }

    @Test
    void health_isPublic() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk());
    }
}
