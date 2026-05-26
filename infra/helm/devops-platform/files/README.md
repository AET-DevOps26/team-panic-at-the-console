# Chart files

`openapi.yaml` is copied from `api/openapi.yaml` by `infra/helm/scripts/deploy.sh` before each install/upgrade.

For a local `helm template` run:

```bash
cp api/openapi.yaml infra/helm/devops-platform/files/openapi.yaml
```
