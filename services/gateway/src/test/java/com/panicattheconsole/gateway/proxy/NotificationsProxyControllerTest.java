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

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = GatewayApplication.class)
@AutoConfigureMockMvc
@Import(MockDownstreamClientsConfig.class)
class NotificationsProxyControllerTest {

    private static final String NOTIFICATION_ID = "018e2c5f-1234-7abc-8def-0000000000bb";
    private static final String RECIPIENT_ID = "018e2c5f-1234-7abc-8def-0000000000cc";

    private static final String LIST_JSON = """
            {
              "items":[{
                "id":"%s",
                "incidentId":"018e2c5f-1234-7abc-8def-0000000000dd",
                "type":"INCIDENT_CREATED",
                "recipientId":null,
                "message":"A new incident was opened.",
                "read":false,
                "createdAt":"2026-07-01T10:00:00Z"
              }],
              "total":1,
              "page":0,
              "size":50,
              "unreadCount":1
            }
            """.formatted(NOTIFICATION_ID);

    @Autowired
    MockMvc mvc;

    @Autowired
    MockRestServiceServer notificationServer;

    @BeforeEach
    void resetServers() {
        notificationServer.reset();
    }

    @Test
    void listNotifications_proxiesWithFiltersAndPagination() throws Exception {
        notificationServer
                .expect(requestTo("http://localhost:8085/notifications?recipientId=" + RECIPIENT_ID
                        + "&unreadOnly=true&page=0&size=50"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withStatus(HttpStatus.OK)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(LIST_JSON));

        mvc.perform(get("/notifications")
                        .queryParam("recipientId", RECIPIENT_ID)
                        .queryParam("unreadOnly", "true")
                        .cookie(TestSessions.sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.items[0].id").value(NOTIFICATION_ID));

        notificationServer.verify();
    }

    @Test
    void markNotificationRead_proxiesAndPreservesNoContent() throws Exception {
        notificationServer
                .expect(requestTo("http://localhost:8085/notifications/" + NOTIFICATION_ID + "/read"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        mvc.perform(post("/notifications/{id}/read", NOTIFICATION_ID).cookie(TestSessions.sessionCookie()))
                .andExpect(status().isNoContent());

        notificationServer.verify();
    }

    @Test
    void markNotificationRead_propagatesNotFound() throws Exception {
        notificationServer
                .expect(requestTo("http://localhost:8085/notifications/" + NOTIFICATION_ID + "/read"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        mvc.perform(post("/notifications/{id}/read", NOTIFICATION_ID).cookie(TestSessions.sessionCookie()))
                .andExpect(status().isNotFound());

        notificationServer.verify();
    }

    @Test
    void markAllNotificationsRead_proxiesWithRecipientScope() throws Exception {
        notificationServer
                .expect(requestTo("http://localhost:8085/notifications/read-all?recipientId=" + RECIPIENT_ID))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        mvc.perform(post("/notifications/read-all")
                        .queryParam("recipientId", RECIPIENT_ID)
                        .cookie(TestSessions.sessionCookie()))
                .andExpect(status().isNoContent());

        notificationServer.verify();
    }
}
