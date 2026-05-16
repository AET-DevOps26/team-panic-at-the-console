# 📝 Problem Statement & Project Description

---

## 1. Problem Statement

Modern software teams struggle to detect, coordinate, and resolve incidents in a structured way. Detection is often delayed, context gets lost across tools, and post-incident analysis is manual and time-consuming.

This project builds a **lightweight incident management system** inspired by SRE tooling, that helps teams:

- detect and track incidents from multiple sources
- manage their full lifecycle with a clear audit trail
- understand failures faster using AI-assisted analysis

---

## 2. Main Functionality

The system will provide:

- incident creation (manual and automatic via external signals such as CI failures)
- lifecycle management (open → investigating → resolved)
- an immutable event log / timeline per incident
- basic collaboration (comments, assignments, status updates)
- AI-assisted incident analysis (summaries, severity suggestions, solution suggestions, postmortem drafts)

---

## Key features

- Auto-create incidents from CI failures or external alerts
- Immutable, append-only event log per incident for auditability
- Concise AI-assisted summaries, severity suggestions, and actionable solution suggestions
- Configurable automation for incident creation and routing
- Lightweight microservice design with CI/webhook integration

## 3. Intended Users

- Developers and students simulating DevOps / SRE workflows
- Teams that want structured visibility into system failures

---

## 4. Example Scenarios

### Scenario 1: CI Failure → Auto Incident

A GitHub Actions pipeline fails → the system receives the webhook → a rule engine evaluates severity → an incident is automatically created and users are notified.

### Scenario 2: Manual Incident

A user reports a production problem → creates an incident → team members collaborate and add updates → issue is resolved and the timeline is preserved.

### Scenario 3: AI-Assisted Triage

A user opens an active incident → the AI service reads the event log and metadata → generates a short summary, suggests severity, proposes concrete solution steps, and drafts a postmortem outline.

---

## User story

- Helps teams detect incidents, collect relevant context, and provide concise AI summaries for faster triage.

## Example incident walkthrough

A GitHub Actions job fails for the `deploy` workflow. The CI webhook is received and normalised.
An automation layer evaluates the event and triggers incident creation when appropriate.

1. CI failure detected (e.g., GitHub Actions job fails).
2. Incoming webhook is normalised and evaluated by an automation layer.
3. Incident is created and timeline entries appended to the event log.
4. AI component generates a short summary, suggested severity, solution suggestions (advisory only), and recommended next actions.
5. Notifications are sent and the frontend displays the incident with AI summary.
6. On-call triages, assigns, and updates the incident; each action is recorded.
7. After resolution, an AI-generated postmortem outline is stored for the final report.

## 5. High-Level System Idea

The system consists of **loosely coupled microservices**:

- a **frontend** dashboard for incident management
- **backend services** handling incidents, event logging, rule evaluation, and automation
- an **AI component** for AI-assisted analysis, running as an independent component
- an **event log** as the source of truth for all state changes

---

## 6. Goals of the Project

The project demonstrates:

- a modular, microservice-based system design
- event-driven architecture with an immutable audit log
- full DevOps workflow (CI/CD, containerisation, Kubernetes deployment)
- meaningful GenAI integration tied to a real user-facing use case

The focus is on **system structure, integration, and operational visibility**, not on building a production-ready tool.

---

## 7. Solution & Next Steps

### First-draft solution and next steps

- Proposal (MVP): three core services — `incident-service` (CRUD + lifecycle), `event-service` (append-only audit), and a minimal `frontend` — plus a small `genai-service` that returns structured summaries. Use webhooks → rule engine → incident flow and PostgreSQL for state.

- Next steps (first draft):
  1.  Finalize scope with the tutor.
  2.  Create repo skeleton (`/services`, `/infra`) and minimal READMEs.
  3.  Implement `incident-service` + `event-service` API and a basic frontend view.
  4.  Add a `genai-service` stub and a CI webhook demo.

Note: this is a short first draft — trim or expand before sharing with the tutor.
