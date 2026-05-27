variable "subscription_id" {
  type        = string
  description = "Azure subscription ID"
  sensitive   = true
}

variable "resource_group_name" {
  type        = string
  default     = "ims-rg"
  description = "Name of the Azure resource group"
}

variable "location" {
  type        = string
  default     = "Germany West Central"
  description = "Azure region for resources"
}

variable "vm_name" {
  type        = string
  default     = "ims-vm"
  description = "Name of the virtual machine"
}

variable "admin_username" {
  type        = string
  default     = "azureuser"
  description = "Admin username for the VM"
}

variable "vm_size" {
  type        = string
  default     = "Standard_B2s"
  description = "Azure VM size (2 CPU, 4GB RAM recommended)"
}

variable "public_key_path" {
  type        = string
  default     = "~/.ssh/id_ed25519.pub"
  description = "Path to SSH public key file"
}