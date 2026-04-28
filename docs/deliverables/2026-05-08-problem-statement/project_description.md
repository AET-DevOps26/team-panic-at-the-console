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
- AI-assisted incident analysis (summaries, severity suggestions, postmortem drafts)

---

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
A user opens an active incident → the AI service reads the event log and metadata → generates a short summary, suggests severity, and drafts a postmortem outline.

---

## 5. High-Level System Idea

The system consists of **loosely coupled microservices**:

- a **frontend** dashboard for incident management
- **backend services** handling incidents, event logging, and rule evaluation
- a **GenAI service** for AI-assisted analysis, running as an independent component
- an **event log** as the source of truth for all state changes

---

## 6. Goals of the Project

The project demonstrates:

- a modular, microservice-based system design
- event-driven architecture with an immutable audit log
- full DevOps workflow (CI/CD, containerisation, Kubernetes deployment)
- meaningful GenAI integration tied to a real user-facing use case

The focus is on **system structure, integration, and operational visibility**, not on building a production-ready tool.