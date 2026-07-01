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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = GatewayApplication.class)
@AutoConfigureMockMvc
@Import(MockDownstreamClientsConfig.class)
class AuthProxyControllerTest {

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
    void register_proxiesUserService() throws Exception {
        userServer
                .expect(requestTo("http://localhost:8084/auth/register"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withStatus(HttpStatus.CREATED)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(USER_JSON));

        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alex@example.com\",\"displayName\":\"Alex\",\"password\":\"secret-abc\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("alex@example.com"));

        userServer.verify();
    }

    @Test
    void login_proxiesUserServiceAndForwardsSetCookie() throws Exception {
        userServer
                .expect(requestTo("http://localhost:8084/auth/login"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Set-Cookie", "session=jwt123; Path=/; HttpOnly; SameSite=Strict")
                                .body(USER_JSON));

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alex@example.com\",\"password\":\"secret-abc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alex@example.com"))
                .andExpect(cookie().exists("session"));

        userServer.verify();
    }

    @Test
    void login_forwardsCookieHeader() throws Exception {
        userServer
                .expect(requestTo("http://localhost:8084/auth/login"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Cookie", "session=existing"))
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(USER_JSON));

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(new jakarta.servlet.http.Cookie("session", "existing"))
                        .content("{\"email\":\"alex@example.com\",\"password\":\"secret-abc\"}"))
                .andExpect(status().isOk());

        userServer.verify();
    }

    @Test
    void logout_proxiesUserServiceAndClearsCookie() throws Exception {
        userServer
                .expect(requestTo("http://localhost:8084/auth/logout"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withStatus(HttpStatus.NO_CONTENT)
                                .header("Set-Cookie", "session=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0"));

        mvc.perform(post("/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("session", 0));

        userServer.verify();
    }

    @Test
    void login_propagatesErrorStatus() throws Exception {
        userServer
                .expect(requestTo("http://localhost:8084/auth/login"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withStatus(HttpStatus.UNAUTHORIZED)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body("{\"message\":\"Invalid email or password\"}"));

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bad@example.com\",\"password\":\"wrong-pwd\"}"))
                .andExpect(status().isUnauthorized());

        userServer.verify();
    }
}
