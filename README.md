# DevOps Project 2026

Lightweight incident management system - detect, track, and resolve incidents with an immutable event log and AI-assisted analysis.

> TUM DevOps Project - Spring 2026 · Team Panic! At the Console

## Quick Start

```bash
# install tools and hooks once
pixi install
pixi run pre-commit-install

# run the same checks as CI
pixi run lint
```

Equivalent Pixi tasks:

```bash
pixi run compose-up
pixi run compose-down
pixi run compose-validate
```

## Installation

Prerequisites:

- Git
- Pixi

Install Pixi on macOS:

```bash
brew install pixi
```

Use a VS Code Dev Container (optional):

```bash
# in VS Code: Dev Containers: Reopen in Container
```

Install project tooling and Git hooks:

```bash
pixi install
pixi run pre-commit-install
```

## Project Structure

```text
.
├── api/                        # OpenAPI specs
│   └── specs/
├── services/
│   ├── frontend/               # Web dashboard (Client subsystem)
│   ├── gateway/                # API gateway - single entry point
│   ├── incident-service/       # Core incident CRUD + lifecycle
│   ├── event-service/          # Append-only event log / timeline
│   ├── rule-engine/            # Evaluates external signals → incident decisions
│   ├── user-service/           # Auth + role management
│   ├── notification-service/   # Notifies users on incident events
│   ├── webhook-service/        # Receives CI/CD webhook events
│   └── genai-service/          # AI summaries, triage, postmortem drafts
├── infra/
│   ├── helm/                   # Helm chart scaffold
│   ├── compose/                # Compose overrides
│   └── postgres/               # Dev DB init scripts
├── docs/
│   └── submissions/
├── tests/
└── scripts/
```

## CI/CD

- **CI**: Runs on pull requests and merge queues.
- **CD**: Tag/release workflow builds and publishes service images to GHCR.
- **Deploy**: Helm + SOPS deployment scaffold is provided in `.github/workflows/deploy-helm-sops.yml`.

See [.github/workflows/](.github/workflows/).

### Release tags

Create a Git tag like `v0.1.0` (or publish a GitHub Release for that tag) to trigger release image publishing.

### Helm + SOPS setup

- SOPS policy file: `.sops.yaml`
- Helm chart scaffold: `infra/helm/devops-platform`
- Encrypted production values expected at: `infra/helm/secrets/values.prod.enc.yaml`
- Deploy environment also includes `k9s` for cluster exploration/debugging.
- Required secrets for deploy workflow:
  - `KUBECONFIG_B64` (base64 encoded kubeconfig)
  - `SOPS_AGE_KEY` (AGE private key content)

Use deploy tooling from Pixi:

```bash
pixi run deploy-tools-version
pixi run -e deploy k9s
```

## Testing

```bash
# Lint (all services, same as CI)
pixi run lint

# incident-service unit tests
pixi run --manifest-path services/incident-service/pixi.toml test

# genai-service currently does not provide a Pixi manifest/test task
# (services/genai-service contains a Dockerfile only)
```

## Local Runtime

```bash
cp .env.example .env
pixi run compose-up
```

Starts all services plus shared infrastructure (Postgres, NATS). Service env vars (`DATABASE_URL`, `NATS_URL`) are pre-wired.

The compose file lives at `infra/compose/docker-compose.yml`. `pixi run compose-up` passes the correct `--project-directory`, `--env-file`, and `-f` flags automatically.

- Override the image tag: `IMAGE_TAG=v0.1.0 pixi run compose-up`

## Mock API Server

Spin up a local HTTP mock server driven by `api/openapi.yaml` using [Prism](https://stoplight.io/open-source/prism):

```bash
pixi run mock-api
```

Prism reads the spec and serves auto-generated responses on `http://localhost:4010`. The spec declares `servers: /api/v1`, so endpoints are available under `http://localhost:4010/api/v1` (e.g. `http://localhost:4010/api/v1/health`). No services need to be running — useful for frontend development and API exploration before backends exist.

## Student Responsibilities

- **Frontend** (`/services/frontend`): @LeonSpoerl
- **Backend** (`/services/gateway`, `incident-service`, `event-service`, `rule-engine`, `user-service`, `notification-service`, `webhook-service`): @florian-pesco
- **GenAI** (`/services/genai-service`): @manuellerchner
