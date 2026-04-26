# DevOps Project 2026

Repository for team Panic! At the Console

> TUM DevOps Project — Spring 2026

## Team

| Name | GitHub | TUMonline | Matriculation | Subsystem |
| ---- | ------ | --------- | ------------- | --------- |
| TBD  | TBD    | TBD       | TBD           | Client    |
| TBD  | TBD    | TBD       | TBD           | Server    |
| TBD  | TBD    | TBD       | TBD           | GenAI     |

## Quick Start

```bash
# install tools and hooks once
pixi install
pixi run pre-commit-install

# run the same checks as CI
pixi run lint
```

Run local service scaffold with Docker Compose:

```bash
cp .env.example .env
docker compose up --build
```

Equivalent Pixi tasks:

```bash
pixi run compose-up
pixi run compose-down
pixi run compose-validate
```

This repository currently provides project scaffolding, CI, and linting automation.

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
├── api/                    # API specs and related scripts
│   ├── scripts/
│   └── specs/
├── services/
│   ├── content-service/
│   ├── gateway/
│   ├── genai-service/
│   └── user-service/
├── infra/
│   ├── compose/
│   ├── helm/
│   ├── k8s/
│   └── monitoring/
├── docs/
│   ├── adr/
│   ├── architecture/
│   └── submissions/
├── tests/
└── scripts/
```

## Architecture

See [docs/architecture/](docs/architecture/) for UML diagrams.

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
- Required secrets for deploy workflow:
  - `KUBECONFIG_B64` (base64 encoded kubeconfig)
  - `SOPS_AGE_KEY` (AGE private key content)

## Testing

```bash
# Current scaffold test target (same command as CI)
pixi run lint
```

## Local Runtime

- Local compose file: `docker-compose.yml`
- Production-oriented compose with Traefik + TLS: `docker-compose.prod.yml`
- Images default to GHCR tag `main` and can be overridden with `IMAGE_TAG`.

## Student Responsibilities

- **Client**: TBD
- **Server**: TBD
- **GenAI**: TBD
