terraform {
  required_version = ">= 1.5.0"
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.26"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.12"
    }
  }
}

provider "kubernetes" {
  config_path = fileexists(pathexpand(var.kubeconfig_path)) ? pathexpand(var.kubeconfig_path) : null
}

provider "helm" {
  kubernetes {
    config_path = fileexists(pathexpand(var.kubeconfig_path)) ? pathexpand(var.kubeconfig_path) : null
  }
}
