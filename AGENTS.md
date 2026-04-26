# Agent Instructions

This repository starts as a scaffold for the DevOps 2026 project. Keep changes small, explicit, and reproducible.

## Core Principles

- Prefer minimal, incremental changes over broad rewrites.
- Keep docs and workflows aligned with the actual repository state.
- Pin CI tooling versions where possible for deterministic runs.
- Use `pixi` tasks for local and CI automation.

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

## Repository Hygiene

- Keep `README.md` realistic; avoid documenting services that do not exist yet.
- Keep `pixi.toml` focused on currently used tools and tasks.
- Extend Dependabot only after manifests are added in the referenced paths.
