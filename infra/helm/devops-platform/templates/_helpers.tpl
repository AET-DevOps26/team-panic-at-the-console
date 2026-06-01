{{- define "devops-platform.postgresPassword" -}}
{{- .Values.secrets.postgresPassword | default .Values.postgres.password -}}
{{- end -}}

{{- define "devops-platform.postgresSecretName" -}}
postgres-credentials
{{- end -}}
