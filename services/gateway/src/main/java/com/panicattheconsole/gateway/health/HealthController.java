package com.panicattheconsole.gateway.health;

import org.openapitools.api.HealthApi;
import org.openapitools.model.HealthCheck200Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HealthController implements HealthApi {

    @Override
    public ResponseEntity<HealthCheck200Response> healthCheck() {
        return ResponseEntity.ok(new HealthCheck200Response("ok"));
    }
}
