## Incident Management System - Deployment Diagram

```mermaid
flowchart TB
    user["Browser"] --> ingress["Kubernetes Ingress<br/>TLS"]

    subgraph namespace["Kubernetes namespace: devops-platform Helm release"]
        ingress --> frontend["frontend<br/>React + Vite"]
        ingress --> gateway["gateway<br/>Spring Boot :8080"]
        ingress --> swagger["swagger-ui"]
        ingress --> grafana["Grafana"]
        ingress --> prometheus["Prometheus"]

        subgraph applications["Application Deployments"]
            frontend
            gateway
            incident["incident-service :8081<br/>including webhook rules"]
            events["event-service :8082"]
            users["user-service :8084"]
            notifications["notification-service :8085"]
            webhooks["webhook-service :8086"]
            genai["genai-service :8087"]
        end

        subgraph infrastructure["Namespace-local infrastructure"]
            postgres[("PostgreSQL<br/>one DB per stateful service")]
            nats["NATS JetStream :4222"]
            prometheus
            grafana
        end

        gateway --> incident
        gateway --> events
        gateway --> users
        gateway --> notifications
        gateway --> webhooks
        incident --> postgres
        events --> postgres
        users --> postgres
        notifications --> postgres
        webhooks --> postgres
        incident <--> nats
        events <--> nats
        notifications <--> nats
        webhooks --> nats
        genai <--> nats
        gateway <--> nats
        prometheus -->|scrapes /metrics| applications
        grafana -->|PromQL| prometheus
    end

    genai --> logos["TUM Logos<br/>Kubernetes LLM provider"]

```

The Helm chart self-hosts Prometheus and Grafana as namespace-local plain Deployments. Ollama runs with the local Compose stack; the Kubernetes deployment uses TUM Logos because the cluster quota cannot accommodate an in-cluster model. `incident-service` includes webhook-rule evaluation.
