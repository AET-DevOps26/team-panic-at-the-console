package com.panicattheconsole.eventservice.event;

import java.util.List;
import java.util.UUID;

import org.openapitools.api.IncidentsApi;
import org.openapitools.model.IncidentEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
class EventsController implements IncidentsApi {

    private final IncidentEventService service;

    EventsController(IncidentEventService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<List<IncidentEvent>> listIncidentEvents(UUID incidentId) {
        return ResponseEntity.ok(service.listForIncident(incidentId));
    }
}
