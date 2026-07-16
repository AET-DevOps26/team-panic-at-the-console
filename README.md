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
│   ├── helm/                   # Helm chart + SOPS-encrypted deploy values
│   └── compose/                # Local docker-compose stack
├── docs/
│   ├── schema.md               # Persistence schema snapshot
│   └── submissions/
├── tests/
└── scripts/
```

## CI/CD

See [.github/workflows/](.github/workflows/).

| Workflow                          | Trigger                                               | Uses SOPS?                               |
| --------------------------------- | ----------------------------------------------------- | ---------------------------------------- |
| `ci.yml` / `compose-validate.yml` | PR, merge queue                                       | No (lint/compose only)                   |
| `container-ci.yml`                | PR, merge queue                                       | No (builds only, no push)                |
| `release.yml`                     | GitHub Release                                        | Orchestrates build + push + both deploys |
| `deploy-k8s.yml`                  | Called by `release.yml`, manual (`workflow_dispatch`) | Yes: deploy to Kubernetes                |
| `deploy-azure-vm.yml`             | Called by `release.yml`, manual (`workflow_dispatch`) | No (uses OIDC + Ansible)                 |

**PR CI does not decrypt SOPS or deploy to the cluster** (no cluster credentials on PRs). Deploy workflows gate on the `kubernetes` and `azure` GitHub Environments; credentials live in repository Actions secrets (below).

### Release tags

Publish a GitHub Release to trigger `release.yml`, the full release pipeline: it builds and pushes all images to GHCR (tagged with the release version and `latest`), then runs `deploy-k8s.yml` and `deploy-azure-vm.yml`. Both deploys use `needs: build`, so they start only once every image is pushed; a failed build aborts the deploys. Each deploy workflow gates on its own environment (`kubernetes` for Helm, `azure` for Terraform + Ansible).

### Helm + SOPS

Production deploy secrets live in `infra/helm/secrets/values.prod.enc.yaml` (encrypted, committed). Plaintext defaults for local chart dev stay in `infra/helm/devops-platform/values.yaml`.

**Pixi tasks** (deploy environment; install with `pixi install`):

| Task                                | Purpose                                           |
| ----------------------------------- | ------------------------------------------------- |
| `pixi run -e deploy helm-deploy`    | Decrypt SOPS values and install/upgrade the chart |
| `pixi run -e deploy helm-uninstall` | Remove the Helm release                           |
| `pixi run -e deploy k9s`            | Open k9s (uses your active kubeconfig)            |

**Flow (local or CI):**

1. `pixi run -e deploy helm-deploy` decrypts `values.prod.enc.yaml` with `SOPS_AGE_KEY`.
2. Helm installs/upgrades `infra/helm/devops-platform` with `--values` (decrypted file) and `--set global.image.tag=$TAG`.
3. The chart creates `postgres-credentials` from `secrets.postgresPassword` in the decrypted file (falls back to `postgres.password` in `values.yaml` if unset).

**Key files:**

| File                                              | Purpose                                                           |
| ------------------------------------------------- | ----------------------------------------------------------------- |
| `.sops.yaml`                                      | Which AGE public keys may encrypt `infra/helm/secrets/*.enc.yaml` |
| `infra/helm/secrets/values.prod.enc.yaml`         | Encrypted prod overrides (commit this)                            |
| `infra/helm/secrets/values.prod.dec.example.yaml` | Template for plaintext before first encrypt                       |
| `infra/helm/devops-platform/files/init-dbs.sh`    | Postgres DB init (compose + Helm)                                 |

Never commit `*.dec.yaml` or `~/.config/sops/age/keys.txt` (gitignored / local only).

#### GitHub Actions secrets (Kubernetes deploy)

Configure under **Settings → Secrets and variables → Actions** (repository scope):

| Name               | Type     | Used for                                       |
| ------------------ | -------- | ---------------------------------------------- |
| `KUBECONFIG_B64`   | Secret   | Cluster access (base64 kubeconfig)             |
| `SOPS_AGE_KEY`     | Secret   | Full AGE private key file (same as `keys.txt`) |
| `DEPLOY_NAMESPACE` | Variable | e.g. `team-panic-at-the-console-devops26`      |

`deploy-k8s.yml` runs `pixi run -e deploy helm-deploy` with these values. Azure deploy secrets are documented in `deploy-azure-vm.yml`.

#### New team member access

You need the **team AGE private key** that matches `.sops.yaml` (recipient `age16sgwfcnyz...`). Without it, `sops --decrypt` fails.

**Option A (usual):** A teammate shares the team `keys.txt` out of band (1Password, in person, etc.). Store it as:

```bash
mkdir -p ~/.config/sops/age
chmod 700 ~/.config/sops/age
# paste the team key file
chmod 600 ~/.config/sops/age/keys.txt
export SOPS_AGE_KEY_FILE=~/.config/sops/age/keys.txt
```

**Option B (new key):** Generate a key with `pixi run -e deploy age-keygen -o ~/.config/sops/age/keys.txt`, send the `# public key: age1...` line to someone who can already decrypt. They add your public key to `.sops.yaml` and re-encrypt:

```bash
sops updatekeys infra/helm/secrets/values.prod.enc.yaml
```

Then commit the updated `.sops.yaml` and `values.prod.enc.yaml`.

**Verify access:**

```bash
export SOPS_AGE_KEY_FILE=~/.config/sops/age/keys.txt
pixi run -e deploy sops --decrypt infra/helm/secrets/values.prod.enc.yaml
```

#### Edit production secrets

Use vim (avoid Cursor/VS Code as `$EDITOR` if it opens empty):

```bash
export SOPS_AGE_KEY_FILE=~/.config/sops/age/keys.txt
EDITOR=vim pixi run -e deploy sops infra/helm/secrets/values.prod.enc.yaml
```

Or set one field:

```bash
pixi run -e deploy sops set infra/helm/secrets/values.prod.enc.yaml \
  '["secrets"]["postgresPassword"]' '"your-password"'
```

Commit only the updated `values.prod.enc.yaml`.

Expected decrypted shape:

```yaml
global:
  image:
    tag: latest
secrets:
  postgresPassword: "<cluster-postgres-password>"
```

#### Deploy / uninstall locally

Required environment variables for `helm-deploy`: `KUBECONFIG_B64`, `SOPS_AGE_KEY`, `DEPLOY_NAMESPACE`, `TAG`.  
`helm-uninstall` needs `KUBECONFIG_B64` and `DEPLOY_NAMESPACE` only.

```bash
export SOPS_AGE_KEY_FILE=~/.config/sops/age/keys.txt

KUBECONFIG_B64=$(base64 < ~/.kube/config | tr -d '\n') \
SOPS_AGE_KEY="$(cat ~/.config/sops/age/keys.txt)" \
DEPLOY_NAMESPACE=team-panic-at-the-console-devops26 \
TAG=v0.0.3 \
pixi run -e deploy helm-deploy

KUBECONFIG_B64=$(base64 < ~/.kube/config | tr -d '\n') \
DEPLOY_NAMESPACE=team-panic-at-the-console-devops26 \
pixi run -e deploy helm-uninstall
```

Optional: `VALUES_FILE=path/to/other.enc.yaml` when running `helm-deploy`.

#### URLs and routes

**Stud cluster** (`https://team-panic-at-the-console-devops26.stud.k8s.aet.cit.tum.de/`):

| Path | Service |
| ---- | ------- |
| `/` | Frontend |
| `/api` | Gateway (`/api/v1/...`) |
| `/swagger` | Swagger UI (OpenAPI) |

Ingress uses cert-manager (`letsencrypt-prod`) and TLS secret `devops-platform-tls`. Metrics are not self-hosted on the cluster: the release ships a PodMonitor, PrometheusRule, and a Grafana dashboard ConfigMap that the shared cluster prometheus-operator and Grafana consume.

**Local compose** (via `docker compose up` or `pixi run compose-up`):

| URL | Service |
| --- | ------- |
| `http://localhost:8080/api/v1/` | Gateway (via `edge`) |
| `http://localhost:8080/swagger` | Swagger UI (via `edge`) |
| `http://localhost:8080/grafana` | Grafana (via `edge`; `admin` / `admin`) |
| `http://localhost:8080/prometheus` | Prometheus UI (via `edge`) |
| `http://localhost:3000/` | Frontend (direct) |
| `http://localhost:3030/` | Grafana (direct) |
| `http://localhost:9090/prometheus` | Prometheus (direct) |
| `http://localhost:8087/metrics` | genai-service Prometheus scrape |

Grafana talks to Prometheus at `http://prometheus:9090/prometheus` inside Docker; use `localhost` from your browser.

Populate Grafana panels after boot: `pixi run compose-smoke-genai-metrics`.

**On Kubernetes**, the chart does not self-host Grafana or Prometheus. It ships namespaced CRs (PodMonitor, PrometheusRule) and a Grafana dashboard ConfigMap that the shared cluster prometheus-operator scrapes and the shared Grafana renders. View metrics and the GenAI dashboard in the cluster's Grafana.

#### Debug the cluster

```bash
pixi run -e deploy k9s
```

Use a kubeconfig/context that points at the stud cluster (`kubectl config current-context`).

## Testing

```bash
# Lint (all services, same as CI)
pixi run lint

# Java service unit tests
pixi run --manifest-path services/incident-service/pixi.toml test
pixi run --manifest-path services/user-service/pixi.toml test

# genai-service (Python)
pixi run test-genai
```

## Local Runtime

A single command from the repository root, no extra tooling required:

```bash
docker compose up
```

Starts all services plus shared infrastructure (Postgres, NATS). Service env vars (`DATABASE_URL`, `NATS_URL`) are pre-wired, and every variable has a baked-in default, so no `.env` is required for a default boot.

The root `compose.yaml` is the entry point Docker discovers automatically (it points at `infra/compose/docker-compose.yml`, where the stack is defined).

For local development, the Pixi wrapper builds from source and loads `.env.example`:

```bash
pixi run compose-up
```

#### URLs and routes

| URL | Service |
| --- | ------- |
| `http://localhost:8080/api/v1/` | Gateway (via `edge`; e.g. `/health`) |
| `http://localhost:8080/swagger` | Swagger UI |
| `http://localhost:8080/grafana` | Grafana (via `edge`; `admin` / `admin`) |
| `http://localhost:8080/prometheus` | Prometheus (via `edge`) |
| `http://localhost:3000/` | Frontend (direct) |
| `http://localhost:3030/` | Grafana (direct) |
| `http://localhost:9090/prometheus` | Prometheus (direct) |
| `http://localhost:8087/metrics` | genai-service metrics |

Same path layout as the stud-cluster ingress (`/api`, `/swagger`, `/grafana`, `/prometheus`); the direct host ports stay published for local debugging. After boot: `pixi run compose-smoke-genai-metrics` to fill the genai dashboard.

Shared non-secret defaults (for example `NATS_URL`) are defined once in `.env.example` and referenced from service-specific environment sections.

- Override the image tag: `IMAGE_TAG=v0.0.1 pixi run compose-up`

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
