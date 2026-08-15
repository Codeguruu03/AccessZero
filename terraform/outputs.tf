output "namespace" {
  value       = var.namespace
  description = "Target Kubernetes namespace"
}

output "keycloak_service_url" {
  value       = "http://keycloak.${var.namespace}.svc.cluster.local:8081"
  description = "Internal Keycloak URL"
}

output "ldap_service_url" {
  value       = "ldap://openldap.${var.namespace}.svc.cluster.local:389"
  description = "Internal OpenLDAP service endpoint"
}

output "accesszero_api_url" {
  value       = "http://accesszero-api.${var.namespace}.svc.cluster.local:8080"
  description = "AccessZero API control plane internal cluster URL"
}
