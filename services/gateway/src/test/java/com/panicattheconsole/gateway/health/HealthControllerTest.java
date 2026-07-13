package com.panicattheconsole.gateway.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.panicattheconsole.gateway.auth.SessionTokenValidator;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The MVC slice instantiates SessionAuthFilter, which needs the validator bean.
@WebMvcTest(HealthController.class)
@Import(SessionTokenValidator.class)
class HealthControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void health_returns200() throws Exception {
        mvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
