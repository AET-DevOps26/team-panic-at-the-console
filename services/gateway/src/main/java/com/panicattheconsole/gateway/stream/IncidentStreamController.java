package com.panicattheconsole.gateway.stream;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE stream of incident change notifications, fed by the NATS incident.*
 * subjects. Not part of the OpenAPI spec: it is a push channel with a single
 * fixed envelope shape, not a REST resource.
 */
@RestController
class IncidentStreamController {

    private final IncidentStreamBroadcaster broadcaster;

    IncidentStreamController(IncidentStreamBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @GetMapping(path = "/incidents/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    ResponseEntity<SseEmitter> streamIncidents() {
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                // Tells nginx (compose edge and k8s ingress) not to buffer the stream.
                .header("X-Accel-Buffering", "no")
                .body(broadcaster.register());
    }
}
