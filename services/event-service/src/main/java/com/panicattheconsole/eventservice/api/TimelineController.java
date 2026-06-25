package com.panicattheconsole.eventservice.api;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.panicattheconsole.eventservice.domain.TimelineEvent;
import com.panicattheconsole.eventservice.service.TimelineService;

@RestController
@RequestMapping("/events")
public class TimelineController {

    private final TimelineService timelineService;

    public TimelineController(TimelineService timelineService) {
        this.timelineService = timelineService;
    }

    @GetMapping("/{incidentId}")
    public List<TimelineEvent> getTimeline(
            @PathVariable UUID incidentId) {
        return timelineService.getTimeline(incidentId);
    }
}
