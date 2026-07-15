{{- define "devops-platform.postgresPassword" -}}
{{- .Values.secrets.postgresPassword | default .Values.postgres.password -}}
{{- end -}}

{{- define "devops-platform.postgresSecretName" -}}
postgres-credentials
{{- end -}}

{{- define "devops-platform.grafanaPassword" -}}
{{- .Values.secrets.grafanaPassword | default .Values.monitoring.grafana.adminPassword -}}
{{- end -}}

{{- define "devops-platform.jwtSecret" -}}
{{- .Values.secrets.jwtSecret | default .Values.auth.jwtSecret -}}
{{- end -}}

{{- define "devops-platform.authSecretName" -}}
auth-credentials
{{- end -}}

{{/*
Rollout policy for this chart's singleton (replicas: 1) Deployments.

Never surge: the project ResourceQuota caps namespace limits with no spare
headroom, so the Deployment default (maxSurge 25%, which rounds up to +1 pod at
replicas: 1) cannot schedule the extra pod and the rollout deadlocks. Both
branches stop the old pod before starting the new one; the trade-off is a brief
per-workload gap on every deploy.

Pass `recreate` truthy for pods that must never overlap at all: those on an RWO
volume (two pods cannot mount it) and stateful singletons.
*/}}
{{- define "devops-platform.rolloutStrategy" -}}
# Keep few old ReplicaSets; repeated deploys otherwise pile up ~10 stale RS each.
revisionHistoryLimit: 2
{{- if .recreate }}
# rollingUpdate: null clears the field when switching from RollingUpdate (Helm merge).
strategy:
  type: Recreate
  rollingUpdate: null
{{- else }}
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 0
    maxUnavailable: 1
{{- end }}
{{- end -}}

{{/*
HTTP liveness/readiness for Spring Boot and genai-service (process up only; no downstream checks).
*/}}
{{- define "devops-platform.httpProbes" -}}
{{- $path := required "healthPath is required when probes are enabled" .healthPath -}}
{{- $port := required "port is required when probes are enabled" .port -}}
# Under tight cpu limits JVM startup can take >60s; the startupProbe holds off
# liveness/readiness until the app is up (up to 300s) so a slow boot is not killed.
startupProbe:
  httpGet:
    path: {{ $path }}
    port: {{ $port }}
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 60
livenessProbe:
  httpGet:
    path: {{ $path }}
    port: {{ $port }}
  initialDelaySeconds: {{ .initialDelaySeconds | default 30 }}
  periodSeconds: 10
  timeoutSeconds: 3
  failureThreshold: 3
readinessProbe:
  httpGet:
    path: {{ $path }}
    port: {{ $port }}
  initialDelaySeconds: {{ .readinessInitialDelaySeconds | default 10 }}
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 3
{{- end -}}

{{/*
Ollama: readiness waits until the configured model is present (matches compose healthcheck).
Liveness only checks that the server responds; model pull runs in an init container.
*/}}
{{- define "devops-platform.ollamaProbes" -}}
{{- $model := .model | default "qwen2.5:3b" -}}
{{- $port := required "port is required for ollama probes" .port -}}
readinessProbe:
  exec:
    command:
      - /bin/sh
      - -c
      - ollama list 2>/dev/null | grep -q '{{ $model }}'
  initialDelaySeconds: 10
  periodSeconds: 15
  timeoutSeconds: 5
  failureThreshold: 40
livenessProbe:
  httpGet:
    path: /
    port: {{ $port }}
  initialDelaySeconds: 30
  periodSeconds: 20
  timeoutSeconds: 5
  failureThreshold: 3
{{- end -}}
