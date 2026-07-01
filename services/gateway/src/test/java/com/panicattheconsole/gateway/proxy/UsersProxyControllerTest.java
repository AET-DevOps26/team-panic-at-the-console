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
class UsersProxyControllerTest {

    private static final String USER_JSON = """
            {
              "id":"018e2c5f-1234-7abc-8def-0000000000aa",
              "email":"alex@example.com",
              "displayName":"Alex",
              "role":"MEMBER",
              "createdAt":"2026-05-08T10:00:00Z"
            }
            """;

    @Autowired
    MockMvc mvc;

    @Autowired
    MockRestServiceServer userServer;

    @BeforeEach
    void resetServers() {
        userServer.reset();
    }

    @Test
    void getCurrentUser_proxiesUserServiceWithCookie() throws Exception {
        userServer
                .expect(requestTo("http://localhost:8084/users/me"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Cookie", "session=jwt123"))
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(USER_JSON));

        mvc.perform(get("/users/me")
                        .cookie(new jakarta.servlet.http.Cookie("session", "jwt123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alex@example.com"));

        userServer.verify();
    }

    @Test
    void getCurrentUser_propagatesUnauthorized() throws Exception {
        userServer
                .expect(requestTo("http://localhost:8084/users/me"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withStatus(HttpStatus.UNAUTHORIZED)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body("{\"message\":\"Not authenticated\"}"));

        mvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());

        userServer.verify();
    }

    @Test
    void listUsers_proxiesWithPaginationAndCookie() throws Exception {
        userServer
                .expect(requestTo("http://localhost:8084/users?limit=10&offset=0"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Cookie", "session=jwt123"))
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body("""
                                        {"items":[],"total":0,"limit":10,"offset":0}
                                        """));

        mvc.perform(get("/users")
                        .queryParam("limit", "10")
                        .queryParam("offset", "0")
                        .cookie(new jakarta.servlet.http.Cookie("session", "jwt123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));

        userServer.verify();
    }
}
