# Agent Instructions

This repository starts as a scaffold for the DevOps 2026 project. Keep changes small, explicit, and reproducible.

## Core Principles

- Prefer minimal, incremental changes over broad rewrites.
- Keep docs and workflows aligned with the actual repository state.
- Pin CI tooling versions where possible for deterministic runs.
- Use `pixi` tasks for local and CI automation.
- Preserve stable CI check names unless there is a deliberate migration plan.

## Local Development

Install tooling and hooks:

```bash
pixi install
pixi run pre-commit-install
```

Run checks:

```bash
pixi run lint
```

## CI Expectations

- GitHub workflows should avoid mutable or floating dependencies.
- CI should fail if the lockfile is outdated (`pixi install --frozen`).
- Add new jobs only when they validate real repository behavior.
- Keep required check contexts stable: `Lint`, `lint-openapi`, `semantic-pr`.
- If any required-check job is renamed, update the branch ruleset required status checks in the same PR.
- Keep merge-critical workflows compatible with merge queues (`pull_request` + `merge_group`).
- Use `main` as default branch target in workflows and automation.
- If CI job names change, update required-status-check rules in GitHub rulesets in the same change.
- Keep `pull_request` and `merge_group` support for merge-queue compatibility.

## Repository Hygiene

- Keep `README.md` realistic; avoid documenting services that do not exist yet.
- Keep `pixi.toml` focused on currently used tools and tasks.
- Extend Dependabot only after manifests are added in the referenced paths.

- Avoid root-level language/runtime toolchains until related service manifests and tasks exist.
## Toolchain Policy

- Keep root Pixi minimal and split concerns by feature (`lint`, `deploy`, etc.).
- Include deploy tooling (`helm`, `kubectl`, `sops`, `age`) in Pixi when used by project workflows.
- Do not add root-level Java/JDK, Maven, npm, or framework-specific toolchains until corresponding service manifests/tasks are committed.
