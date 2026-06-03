# AGENTS.md

This file provides guidance to AI assistants when working with code in this repository.

## Project Overview

Lightweight incident management system: detect, track, and resolve incidents with an immutable event log and AI-assisted analysis.

TUM DevOps Project · Spring 2026 · Team Panic! At the Console

## Package Management

This repository uses [pixi](https://pixi.sh) for tooling. Use `pixi run` for project tasks. If you change `pixi.toml`, run `pixi lock` afterwards.

## Development Commands

```bash
pixi install                   # Install tooling and set up the Pixi environment
pixi run pre-commit-install    # Register lefthook git hooks
pixi run lint                  # Run all linters (same as CI)
pixi run test-genai            # Run genai-service tests (manifest in services/genai-service/)
pixi run compose-up            # Start full stack locally (builds from source)
pixi run compose-down          # Stop and remove containers
pixi run compose-validate      # Validate docker-compose files
pixi run openapi-lint          # Lint api/openapi.yaml if present
```

## Architecture

### Tech Stack

| Layer            | Technology                                                          |
| ---------------- | ------------------------------------------------------------------- |
| Frontend         | React + Vite + TypeScript + shadcn/ui + tanstack-query              |
| Backend services | Java Spring Boot (latest)                                           |
| GenAI service    | Python + FastAPI + nats.py                                          |
| LLM              | Ollama `qwen2.5:3b` (runs in cluster, no cloud LLM)                 |
| Database         | PostgreSQL (shared instance, one DB per stateful service)           |
| Event bus        | NATS JetStream (side effects: event log, notifications, genai, SSE) |
| Observability    | kube-prometheus-stack + loki-stack                                  |

See `CONTEXT.md` for full architectural decisions and `docs/adr/` for key trade-off records.

### Services

| Service                | Port | Description                             |
| ---------------------- | ---- | --------------------------------------- |
| `frontend`             | 3000 | Web dashboard                           |
| `gateway`              | 8080 | Single API entry point                  |
| `incident-service`     | 8081 | Core incident CRUD + lifecycle          |
| `event-service`        | 8082 | Append-only event log                   |
| `rule-engine`          | 8083 | Evaluates signals → incident decisions  |
| `user-service`         | 8084 | Auth + role management                  |
| `notification-service` | 8085 | Notifies users on incident events       |
| `webhook-service`      | 8086 | Receives CI/CD webhook events           |
| `genai-service`        | 8087 | AI summaries, triage, postmortem drafts |
| `swagger-ui`           | (via `:8080/swagger`) | OpenAPI explorer; routed by compose `edge` / Helm ingress |

### Infrastructure

- **Postgres** (`localhost:5432`) - shared instance; one database per stateful service (`incidents`, `events`, `users`, `notifications`, `rules`); initialized by `infra/helm/devops-platform/files/init-dbs.sh`
- **NATS** (`localhost:4222`, monitoring: `localhost:8222`) - event bus with JetStream; used for all side effects between services
- **Ollama** (`localhost:11434`) - local LLM inference; model `qwen2.5:3b` pulled on startup

### Key Paths

```
api/                  # OpenAPI specs
services/             # Application services (one dir per service)
infra/helm/           # Helm chart for Kubernetes deployment
infra/helm/devops-platform/files/  # Postgres DB init script (compose + Helm)
.github/workflows/    # CI/CD pipelines
```

## CI/CD

- PRs run: lint, lockfile check, container build validation, semantic PR title check
- Merges to `main` build and push all images to GHCR tagged `:main` and `:<full-commit-sha>`
- Publishing a GitHub Release triggers `release-deploy.yml`: builds versioned images + deploys via Helm to the `production` environment
- Publishing a GitHub Release also triggers `deploy-azure-vm.yml` with action `deploy` (Ansible only; VM must exist in Terraform state). First-time VM setup: run `deploy-azure-vm.yml` manually with action `full`
- Manual Helm deploys: `deploy-helm-sops.yml` (workflow_dispatch, requires `KUBECONFIG_B64` and `SOPS_AGE_KEY` secrets)

## Code Review

When reviewing PRs, flag:

- New services missing a `Dockerfile` or not added to the matrix in `container-ci.yml`
- Workflows missing `merge_group` trigger (required for merge queue compatibility)
- Services reading/writing a database they don't own (each stateful service has one DB; check `infra/helm/devops-platform/files/init-dbs.sh` for current list)
- Secrets or credentials committed or hardcoded: use environment variables or SOPS
- Changes to CI job names without a corresponding branch ruleset update

## Code Standards

- Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/): `<type>(<scope>): <subject>`
- Avoid em dashes (U+2014) in docs and comments; prefer `:` or parentheses.
- Do not rename CI jobs without updating branch ruleset required status checks in the same PR
- Do not add root-level language/runtime toolchains until corresponding service manifests exist
- Keep `README.md` and docs aligned with actual repository state: no aspirational documentation
