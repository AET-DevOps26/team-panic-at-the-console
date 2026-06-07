{{- define "devops-platform.postgresPassword" -}}
{{- .Values.secrets.postgresPassword | default .Values.postgres.password -}}
{{- end -}}

{{- define "devops-platform.postgresSecretName" -}}
postgres-credentials
{{- end -}}

{{/*
HTTP liveness/readiness for Spring Boot and genai-service (process up only; no downstream checks).
*/}}
{{- define "devops-platform.httpProbes" -}}
{{- $path := required "healthPath is required when probes are enabled" .healthPath -}}
{{- $port := required "port is required when probes are enabled" .port -}}
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
