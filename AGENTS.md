# AGENTS.md

This file provides guidance to AI assistants when working with code in this repository.

## Project Overview

Lightweight incident management system: detect, track, and resolve incidents with an immutable event log and AI-assisted analysis.

TUM DevOps Project · Spring 2026 · Team Panic! At the Console

## Package Management

This repository uses [pixi](https://pixi.sh) for tooling. Use `pixi run` for project tasks. If you change `pixi.toml`, run `pixi lock` afterwards.

### Pixi Usage Rules

**Always use Pixi to run builds, tests, and development commands.**

Do not invoke language-specific tooling directly (e.g. `mvn`, `gradle`, `npm`, `pnpm`, `pytest`) if an equivalent Pixi task exists.

Each service maintains its own Pixi environment. Before working on a service, change into the service directory and use the local Pixi tasks.

Example:

```bash
cd services/incident-service
pixi run
```

Available tasks:

```text
build
start
test
```

Standard service commands:

```bash
pixi run start   # Start the service locally
pixi run build   # Build the service
pixi run test    # Run the service test suite
```

When modifying a service:

1. Use the service-local Pixi environment.
2. Run `pixi run test` before considering the task complete.
3. Run `pixi run build` when changes affect compilation, packaging, or dependencies.
4. Report any failing tests or build errors.
5. Prefer existing Pixi tasks over direct invocation of language-specific tooling.

A change is not considered complete until the relevant Pixi test and build commands succeed.

## Development Commands

### Repository-Level Tasks

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
| Observability    | self-hosted namespace-local Prometheus + Grafana (plain Deployments, no operator/CRDs; optional operatorCrds compatibility mode) |

See `CONTEXT.md` for full architectural decisions and `docs/adr/` for key trade-off records.

### Services

| Service                | Port                  | Description                                               |
| ---------------------- | --------------------- | --------------------------------------------------------- |
| `frontend`             | 3000                  | Web dashboard                                             |
| `gateway`              | 8080                  | Single API entry point                                    |
| `incident-service`     | 8081                  | Core incident CRUD + lifecycle                            |
| `event-service`        | 8082                  | Append-only event log                                     |
| `rule-engine`          | 8083                  | Evaluates signals → incident decisions                    |
| `user-service`         | 8084                  | Auth + role management                                    |
| `notification-service` | 8085                  | Notifies users on incident events                         |
| `webhook-service`      | 8086                  | Receives CI/CD webhook events                             |
| `genai-service`        | 8087                  | AI summaries, triage, postmortem drafts                   |
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

- PRs and merge queue: lint, lockfile check, container build validation, semantic PR title check (images built but not pushed)
- Publishing a GitHub Release runs `release.yml`, the single release orchestrator: it builds and pushes all images to GHCR (tagged with the release version and `latest`), then deploys to Kubernetes and the Azure VM. The deploy jobs use `needs: build`, so they start only after every image is pushed and a failed build aborts both deploys (no GHCR polling).
- `deploy-k8s.yml` and `deploy-azure-vm.yml` are reusable (`workflow_call`) workflows that `release.yml` calls, and they also expose `workflow_dispatch` for ad-hoc deploys. The image tag and (for Azure) the action come from workflow inputs.
- Manual Kubernetes deploy: `deploy-k8s.yml` (workflow_dispatch with `tag` input; gates on `kubernetes`)
- Manual Azure VM deploy: `deploy-azure-vm.yml` (workflow_dispatch with `action` and `tag` inputs; gates on `azure`; one approval covers Terraform and Ansible)

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
- Always use existing Pixi tasks for build, test, lint, and development workflows. Do not invoke language-specific tooling directly when a Pixi task is available.
