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
pixi run lint
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

See [.github/workflows/](.github/workflows/).

### Release tags

Create a Git tag like `v0.1.0` (or publish a GitHub Release for that tag) to trigger release image publishing.

## Testing

```bash
# Lint and hook checks (same command as CI)
pixi run lint
```

## Student Responsibilities

- **Client**: TBD
- **Server**: TBD
- **GenAI**: TBD
