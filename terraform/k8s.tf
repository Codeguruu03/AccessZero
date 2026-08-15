resource "kubernetes_namespace" "accesszero" {
  metadata {
    name = var.namespace
    labels = {
      "app.kubernetes.io/name"       = "accesszero"
      "app.kubernetes.io/managed-by" = "terraform"
    }
  }
}

resource "kubernetes_secret" "accesszero_secrets" {
  metadata {
    name      = "accesszero-secrets"
    namespace = kubernetes_namespace.accesszero.metadata[0].name
  }

  data = {
    POSTGRES_DB             = "accesszerodb"
    POSTGRES_USER           = "sa"
    POSTGRES_PASSWORD       = "password123"
    KEYCLOAK_ADMIN          = var.keycloak_admin_user
    KEYCLOAK_ADMIN_PASSWORD = var.keycloak_admin_password
    LDAP_ADMIN_PASSWORD     = var.ldap_admin_password
  }
}
