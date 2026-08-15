# AccessZero IAM Platform - Terraform Configuration
resource "kubernetes_config_map" "accesszero_config" {
  metadata {
    name      = "accesszero-config"
    namespace = kubernetes_namespace.accesszero.metadata[0].name
  }

  data = {
    SPRING_DATASOURCE_URL = "jdbc:postgresql://postgres.${kubernetes_namespace.accesszero.metadata[0].name}.svc.cluster.local:5432/accesszerodb"
    KEYCLOAK_SERVER_URL   = "http://keycloak.${kubernetes_namespace.accesszero.metadata[0].name}.svc.cluster.local:8081"
    KEYCLOAK_REALM        = "accesszero-realm"
    LDAP_URL              = "ldap://openldap.${kubernetes_namespace.accesszero.metadata[0].name}.svc.cluster.local:389"
    LDAP_BASE_DN          = "dc=company,dc=com"
  }
}
