# AccessZero IAM Platform - Terraform Configuration
variable "kubeconfig_path" {
  type        = string
  description = "Path to the kubeconfig file for local or remote cluster deployment"
  default     = "~/.kube/config"
}

variable "namespace" {
  type        = string
  description = "Kubernetes namespace for AccessZero"
  default     = "accesszero"
}

variable "keycloak_admin_user" {
  type        = string
  description = "Keycloak admin master username"
  default     = "admin"
}

variable "keycloak_admin_password" {
  type        = string
  description = "Keycloak admin master password"
  sensitive   = true
  default     = "adminpassword"
}

variable "ldap_admin_password" {
  type        = string
  description = "OpenLDAP root bind password"
  sensitive   = true
  default     = "adminpassword"
}

variable "api_replicas" {
  type        = number
  description = "Number of AccessZero API replicas"
  default     = 2
}
