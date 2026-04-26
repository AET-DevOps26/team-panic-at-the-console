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
cp .env.example .env
docker compose up --build
```

App runs at `http://localhost:3000`, API at `http://localhost:8080`.

## Installation

Prerequisites:

- Docker + Docker Compose
- Git
- Pixi (recommended for local tooling)

Install Pixi on macOS:

```bash
brew install pixi
```

Install project tooling and Git hooks:

```bash
pixi install
pixi run pre-commit-install
```

Run the same checks as CI locally:

```bash
pixi run lint
```

## Project Structure

```
.
├── api/                    # OpenAPI spec (single source of truth)
├── services/
│   ├── gateway/            # Spring Cloud Gateway
│   ├── user-service/       # Spring Boot — user management
│   ├── content-service/    # Spring Boot — core business logic
│   └── genai-service/      # Python — LangChain AI service
├── client/                 # Frontend (React/Vue/Angular)
├── infra/
│   ├── helm/               # Helm charts for Kubernetes
│   ├── k8s/                # Raw Kubernetes manifests
│   └── monitoring/         # Prometheus + Grafana config
├── docs/                   # Architecture diagrams, ADRs, API docs
└── docker-compose.yml      # Local orchestration
```

## Architecture

See [docs/architecture/](docs/architecture/) for UML diagrams.

API docs: `http://localhost:8080/swagger-ui.html` (when running)

## CI/CD

- **CI**: Runs on every PR — lint, build, test all services
- **CD**: Deploys to Kubernetes on merge to `main`

See [.github/workflows/](.github/workflows/).

## Monitoring

Grafana: `http://localhost:3001` (admin/admin)
Prometheus: `http://localhost:9090`

Dashboards exported to [infra/monitoring/grafana/dashboards/](infra/monitoring/grafana/dashboards/).

## Testing

```bash
# All services
docker compose run --rm user-service ./mvnw test
docker compose run --rm content-service ./mvnw test
docker compose run --rm genai-service pytest
```

## Student Responsibilities

- **Client**: TBD
- **Server**: TBD
- **GenAI**: TBD
