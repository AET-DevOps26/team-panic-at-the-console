# Ansible deployment for Azure VM

This folder contains the Ansible automation to provision an Azure VM and deploy the project stack via Docker Compose.

## What this playbook does

- installs base packages (`git`, `curl`, apt helpers)
- installs Docker Engine and the Docker Compose plugin
- clones the repository into `/opt/team-panic-at-the-console`
- copies `.env.example` to `.env` if needed
- runs `docker compose up -d` using `infra/compose/docker-compose.yml`

## How to use it

1. Create Azure resources with Terraform:

```bash
cd infra/terraform
terraform init
terraform apply -auto-approve -var-file=terraform.tfvars
```

2. Get the public IP from Terraform:

```bash
cd infra/terraform
terraform output -raw public_ip
```

3. Update `infra/ansible/inventory/hosts.ini` with the actual VM IP and your SSH key:

```ini
[azure]
52.123.45.67 ansible_user=azureuser ansible_ssh_private_key_file=~/.ssh/id_ed25519 ansible_python_interpreter=/usr/bin/python3
```

4. Run the Ansible playbook from the repository root:

```bash
cd /home/florianp/uni/devops/team-panic-at-the-console
ansible-playbook -i infra/ansible/inventory/hosts.ini infra/ansible/site.yml
```

## Notes

- The VM is created by Terraform in `infra/terraform`.
- The playbook assumes the VM user is `azureuser` and that SSH key auth is already configured in Terraform.
- If the repository is already cloned on the VM, the playbook will update it.
- If published images are available on GHCR, the playbook will attempt `docker compose pull`; otherwise it falls back to `docker compose build`.
