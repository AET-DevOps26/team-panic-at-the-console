variable "prefix" {
  description = "Prefix for all Azure resource names"
  type        = string
  default     = "team-panic"
}

variable "resource_group_name" {
  description = "Name of the Azure resource group"
  type        = string
  default     = "team-panic-rg"
}

variable "location" {
  description = "Azure region"
  type        = string
  default     = "germanywestcentral"
}

variable "environment" {
  description = "Environment tag applied to all resources"
  type        = string
  default     = "production"
}

variable "vm_size" {
  description = "Azure VM SKU. Standard_B4as_v2 = 2 vCPU / 8 GB AMD, burstable."
  type        = string
  default     = "Standard_B2as_v2"
}

variable "admin_username" {
  description = "Linux admin username on the VM"
  type        = string
  default     = "azureuser"
}

variable "ssh_public_key" {
  description = "SSH public key content (full key string, not a file path)"
  type        = string
}

variable "ssh_source_cidr" {
  description = "Source IP/CIDR allowed to SSH. Use '*' to allow all, or set to your IP (e.g. '1.2.3.4/32') for tighter control."
  type        = string
  default     = "*"
}

variable "os_disk_size_gb" {
  description = "OS disk size in GB. 25 GB covers OS, Docker images, Postgres data, and the Ollama qwen2.5:3b model."
  type        = number
  default     = 25
}
