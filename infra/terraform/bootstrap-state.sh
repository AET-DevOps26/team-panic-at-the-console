#!/usr/bin/env bash
# One-time setup: creates the Azure Storage Account used for Terraform remote state.
# Run this ONCE before your first "terraform init".
#
# Prerequisites:
#   - az CLI logged in:  az login
#     (or use service principal: export ARM_CLIENT_ID / ARM_CLIENT_SECRET / ARM_TENANT_ID)
set -euo pipefail

RESOURCE_GROUP="tfstate-rg"
LOCATION="germanywestcentral"
CONTAINER="tfstate"

# Storage account names must be 3-24 lowercase alphanumeric characters and globally unique.
STORAGE_ACCOUNT="tfstateteampanic$(openssl rand -hex 4)"

echo "==> Creating resource group '${RESOURCE_GROUP}' in ${LOCATION}"
az group create \
  --name "${RESOURCE_GROUP}" \
  --location "${LOCATION}" \
  --output none

echo "==> Creating storage account '${STORAGE_ACCOUNT}'"
az storage account create \
  --name "${STORAGE_ACCOUNT}" \
  --resource-group "${RESOURCE_GROUP}" \
  --location "${LOCATION}" \
  --sku Standard_LRS \
  --min-tls-version TLS1_2 \
  --output none

echo "==> Creating blob container '${CONTAINER}'"
az storage container create \
  --name "${CONTAINER}" \
  --account-name "${STORAGE_ACCOUNT}" \
  --output none

echo ""
echo "Bootstrap complete."
echo ""
echo "Next steps:"
echo ""
echo "  1. Create infra/terraform/backend.tfvars (gitignored):"
echo "       storage_account_name = \"${STORAGE_ACCOUNT}\""
echo ""
echo "  2. Initialize Terraform:"
echo "       terraform -chdir=infra/terraform init -backend-config=backend.tfvars"
echo ""
echo "  3. Add as a GitHub Actions Variable (for CI/CD):"
echo "       Name:  TF_BACKEND_STORAGE_ACCOUNT"
echo "       Value: ${STORAGE_ACCOUNT}"
