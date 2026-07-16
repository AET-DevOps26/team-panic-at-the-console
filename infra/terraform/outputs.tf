output "vm_public_ip" {
  description = "Public IP address of the VM"
  value       = azurerm_public_ip.main.ip_address
}

output "vm_name" {
  description = "Azure VM resource name"
  value       = azurerm_linux_virtual_machine.main.name
}

output "resource_group_name" {
  description = "Azure resource group containing all resources"
  value       = azurerm_resource_group.main.name
}

output "ssh_command" {
  description = "SSH command to connect to the VM"
  value       = "ssh ${var.admin_username}@${azurerm_public_ip.main.ip_address}"
}

output "service_urls" {
  description = "Application endpoints once the stack is deployed (all via the edge proxy)"
  value = {
    frontend   = "http://${azurerm_public_ip.main.ip_address}:8080"
    api        = "http://${azurerm_public_ip.main.ip_address}:8080/api/v1"
    swagger_ui = "http://${azurerm_public_ip.main.ip_address}:8080/swagger"
    webhooks   = "http://${azurerm_public_ip.main.ip_address}:8080/webhooks"
    grafana    = "http://${azurerm_public_ip.main.ip_address}:8080/grafana"
    prometheus = "http://${azurerm_public_ip.main.ip_address}:8080/prometheus"
  }
}
