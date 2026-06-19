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
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=builder /app/frontend/dist /usr/share/nginx/html
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 3000
CMD ["nginx", "-g", "daemon off;"]
