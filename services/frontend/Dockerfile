# Build context is services/ — see infra/compose/docker-compose.yml and _build-images.yml.
# OpenAPI types live at services/generated/typescript/ (openapi-typescript output).
FROM node:22-alpine AS builder
WORKDIR /app/frontend

COPY generated/typescript /app/generated/typescript
COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ .
ARG VITE_MOCK=false
ENV VITE_MOCK=$VITE_MOCK
ARG VITE_COMMIT_SHA=dev
ENV VITE_COMMIT_SHA=$VITE_COMMIT_SHA
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=builder /app/frontend/dist /usr/share/nginx/html
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
# The stock entrypoint renders *.template with envsubst into the html root at
# container start (runtime config for the SPA). The empty defaults keep unset
# vars from surviving as literal "${...}" in the rendered file.
COPY frontend/config.js.template /etc/nginx/templates/config.js.template
ENV NGINX_ENVSUBST_OUTPUT_DIR=/usr/share/nginx/html \
    GRAFANA_URL="" \
    PROMETHEUS_URL=""
EXPOSE 3000
CMD ["nginx", "-g", "daemon off;"]
