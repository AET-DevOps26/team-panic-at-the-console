package com.panicattheconsole.eventservice.api;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.panicattheconsole.eventservice.service.TimelineService;

/**
 * Public timeline read model. The gateway proxies
 * GET /incidents/{incidentId}/events here; the raw envelope endpoint under
 * /events stays for debugging and internal use.
 */
@RestController
public class IncidentTimelineController {

    private final TimelineService timelineService;

    public IncidentTimelineController(TimelineService timelineService) {
        this.timelineService = timelineService;
    }

    @GetMapping("/incidents/{incidentId}/events")
    public List<IncidentEventDto> listIncidentEvents(@PathVariable UUID incidentId) {
        return timelineService.getPublicTimeline(incidentId);
    }
}
