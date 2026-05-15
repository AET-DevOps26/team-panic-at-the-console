# Project Meeting

**2026-05-15, 15:30 – 18:30**  
**Participants:** Manuel, Florian, Leon

---

## Repository & Infrastructure Walkthrough

Did a complete walkthrough of the current repository structure and infrastructure setup.

Topics covered:

- CI/CD workflows
- Docker setup
- Kubernetes deployment structure
- Helm charts
- Pixi environment management
- GitHub Actions workflows and deployment flow

The goal was mainly to align everyone on the current system state and reduce uncertainty around the growing tool stack and deployment pipeline.

---

## Open Questions & Decisions

### Pixi

Clarified the purpose of Pixi in the project:

- reproducible development environments
- dependency management across services
- easier onboarding/setup consistency

### Helm

Went through how Helm is currently used:

- deployment templating
- environment configuration
- simplifying Kubernetes manifests

### Publish on Tag

Clarified the deployment/release strategy:

- publishing and release workflows should trigger on Git tags
- allows clearer release boundaries

---

## High-Level Deployment Overview

Discussed the current and planned deployment architecture:

- local development via Docker Compose
- Kubernetes deployment as the main deployment target
- GitHub Actions pipeline handles testing/building/deployment steps

Main focus was understanding the full flow from:

developer push → CI → container build → deployment.

---

## Architecture Discussion

### Message Broker Decision

Decided to use **NATS** as the message broker.

Main discussion points:

- reducing direct coupling between services
- keeping communication event-driven where appropriate
- lightweight operational footprint compared to heavier alternatives

Agreed that this fits the architecture and MVP scope better than introducing something larger like Kafka this early.

The broker will primarily support:

- incident-related events
- automation/rule evaluation triggers
- asynchronous service communication

---

## Development Workflow

### Stacked Pull Requests

Discussed and agreed on using **stacked pull requests** where appropriate.

Main reasons:

- easier reviews
- smaller PR scope
- reduced merge conflicts
- avoiding developer blockage when dependent work exists

Agreed that PRs should remain small and focused whenever possible.

---

## Pull Request Review Session

Went through several currently open pull requests together.

---

## Frontend Planning

Started planning the frontend structure and communication flow.

Topics discussed:

- frontend ↔ backend endpoint communication
- incident overview pages
- event/timeline visualization
- API interaction patterns
- basic UI flow for the MVP demo

No final frontend architecture committed yet, but the general direction is now clearer.

We will start building the frontend foundation while backend business logic progresses in parallel.

---

## Next Steps

- Implement core business logic
- Start frontend implementation
- Continue backend service integration
- Refine event flow using NATS
- Continue reviewing and merging infrastructure-related PRs