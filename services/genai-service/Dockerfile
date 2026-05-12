# Pin aligns with pixi-build preview support; bump when upgrading Pixi lock formats.
FROM ghcr.io/prefix-dev/pixi:0.41.4 AS build

WORKDIR /app

COPY pixi.toml pixi.lock pyproject.toml ./
COPY src/ ./src/

RUN pixi install --frozen -e prod

FROM python:3.12-slim AS runtime

WORKDIR /app

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PATH="/app/.pixi/envs/prod/bin:$PATH"

COPY --from=build /app/.pixi/envs/prod /app/.pixi/envs/prod
COPY --from=build /app/src /app/src

EXPOSE 8087

CMD ["uvicorn", "genai_service.main:app", "--host", "0.0.0.0", "--port", "8087"]
