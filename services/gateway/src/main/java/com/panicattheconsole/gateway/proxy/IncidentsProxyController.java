package com.panicattheconsole.gateway.proxy;

import java.util.List;
import java.util.UUID;

import org.openapitools.api.IncidentsApi;
import org.openapitools.model.AssignIncidentRequest;
import org.openapitools.model.Comment;
import org.openapitools.model.CommentListResponse;
import org.openapitools.model.CreateCommentRequest;
import org.openapitools.model.CreateIncidentRequest;
import org.openapitools.model.EscalateSeverityRequest;
import org.openapitools.model.Incident;
import org.openapitools.model.IncidentEvent;
import org.openapitools.model.IncidentListResponse;
import org.openapitools.model.IncidentStatus;
import org.openapitools.model.PostmortemPatch;
import org.openapitools.model.Severity;
import org.openapitools.model.SeverityPatch;
import org.openapitools.model.SolutionsPatch;
import org.openapitools.model.SummaryPatch;
import org.openapitools.model.UpdateStatusRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Proxies public incident REST routes to incident-service. Genai write-back PATCH routes are
 * cluster-internal only and are not forwarded (see OpenAPI descriptions on genai result paths).
 */
@RestController
class IncidentsProxyController implements IncidentsApi {

    private static final ParameterizedTypeReference<List<IncidentEvent>> INCIDENT_EVENTS =
            new ParameterizedTypeReference<>() {};

    private static final ResponseEntity<Void> NOT_EXPOSED_ON_GATEWAY =
            ResponseEntity.status(HttpStatus.FORBIDDEN).build();

    private final RestClient incidentServiceClient;

    IncidentsProxyController(@Qualifier("incidentServiceClient") RestClient incidentServiceClient) {
        this.incidentServiceClient = incidentServiceClient;
    }

    @Override
    public ResponseEntity<IncidentListResponse> listIncidents(
            IncidentStatus status, Severity severity, Integer page, Integer size) {
        return DownstreamProxy.get(
                incidentServiceClient, listIncidentsPath(status, severity, page, size), IncidentListResponse.class);
    }

    @Override
    public ResponseEntity<Incident> createIncident(CreateIncidentRequest createIncidentRequest) {
        return DownstreamProxy.post(
                incidentServiceClient,
                "/incidents",
                createIncidentRequest,
                Incident.class);
    }

    @Override
    public ResponseEntity<Incident> getIncident(UUID incidentId) {
        return DownstreamProxy.get(
                incidentServiceClient, "/incidents/{incidentId}", Incident.class, incidentId);
    }

    @Override
    public ResponseEntity<List<IncidentEvent>> listIncidentEvents(UUID incidentId) {
        return DownstreamProxy.get(
                incidentServiceClient,
                "/incidents/{incidentId}/events",
                INCIDENT_EVENTS,
                incidentId);
    }

    @Override
    public ResponseEntity<Incident> updateIncidentStatus(
            UUID incidentId, UpdateStatusRequest updateStatusRequest) {
        return DownstreamProxy.patch(
                incidentServiceClient,
                "/incidents/{incidentId}/status",
                updateStatusRequest,
                Incident.class,
                incidentId);
    }

    @Override
    public ResponseEntity<Incident> escalateIncidentSeverity(
            UUID incidentId, EscalateSeverityRequest escalateSeverityRequest) {
        return DownstreamProxy.patch(
                incidentServiceClient,
                "/incidents/{incidentId}/severity",
                escalateSeverityRequest,
                Incident.class,
                incidentId);
    }

    @Override
    public ResponseEntity<Incident> assignIncident(UUID incidentId, AssignIncidentRequest assignIncidentRequest) {
        return DownstreamProxy.patch(
                incidentServiceClient,
                "/incidents/{incidentId}/assign",
                assignIncidentRequest,
                Incident.class,
                incidentId);
    }

    @Override
    public ResponseEntity<CommentListResponse> listComments(UUID incidentId, Integer page, Integer size) {
        return DownstreamProxy.get(
                incidentServiceClient,
                listCommentsPath(incidentId, page, size),
                CommentListResponse.class);
    }

    @Override
    public ResponseEntity<Comment> addComment(UUID incidentId, CreateCommentRequest createCommentRequest) {
        return DownstreamProxy.post(
                incidentServiceClient,
                "/incidents/{incidentId}/comments",
                createCommentRequest,
                Comment.class,
                incidentId);
    }

    @Override
    public ResponseEntity<Void> writeIncidentSummary(UUID incidentId, SummaryPatch summaryPatch) {
        return NOT_EXPOSED_ON_GATEWAY;
    }

    @Override
    public ResponseEntity<Void> writeIncidentSeveritySuggestion(UUID incidentId, SeverityPatch severityPatch) {
        return NOT_EXPOSED_ON_GATEWAY;
    }

    @Override
    public ResponseEntity<Void> writeIncidentSolutions(UUID incidentId, SolutionsPatch solutionsPatch) {
        return NOT_EXPOSED_ON_GATEWAY;
    }

    @Override
    public ResponseEntity<Void> writeIncidentPostmortem(UUID incidentId, PostmortemPatch postmortemPatch) {
        return NOT_EXPOSED_ON_GATEWAY;
    }

    private static String listIncidentsPath(
            IncidentStatus status, Severity severity, Integer page, Integer size) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/incidents");
        if (status != null) {
            builder.queryParam("status", status.getValue());
        }
        if (severity != null) {
            builder.queryParam("severity", severity.getValue());
        }
        if (page != null) {
            builder.queryParam("page", page);
        }
        if (size != null) {
            builder.queryParam("size", size);
        }
        return builder.build().toUriString();
    }

    private static String listCommentsPath(UUID incidentId, Integer page, Integer size) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.fromPath("/incidents/{incidentId}/comments");
        if (page != null) {
            builder.queryParam("page", page);
        }
        if (size != null) {
            builder.queryParam("size", size);
        }
        return builder.buildAndExpand(incidentId).toUriString();
    }
}
