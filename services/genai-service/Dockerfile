# Match a Pixi release that supports workspace `exclude-newer`, lockfile v7, and `pixi-build`
# (see services/genai-service/pixi.toml). Bump when the lockfile format or preview features require it.
FROM ghcr.io/prefix-dev/pixi:0.68.1-jammy AS build

WORKDIR /app

# Build context is services/ — see infra/compose/docker-compose.yml and container-ci.yml.
# Generated OpenAPI client lives at services/generated/python-client/ and is consumed
# as a path dep declared in genai-service/pixi.toml.
COPY generated/python-client ./generated/python-client

COPY genai-service/pixi.toml genai-service/pixi.lock genai-service/pyproject.toml ./genai-service/
COPY genai-service/README.md ./genai-service/
COPY genai-service/src ./genai-service/src

RUN cd genai-service && pixi install --frozen -e prod

FROM python:3.12-slim AS runtime

WORKDIR /app

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PATH="/app/.pixi/envs/prod/bin:$PATH"

COPY --from=build /app/genai-service/.pixi/envs/prod /app/.pixi/envs/prod
COPY --from=build /app/genai-service/src /app/src

EXPOSE 8087

CMD ["uvicorn", "genai_service.main:app", "--host", "0.0.0.0", "--port", "8087"]
