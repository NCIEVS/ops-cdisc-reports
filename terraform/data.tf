data "aws_caller_identity" "current" {}

locals {
  account_id = data.aws_caller_identity.current.account_id
}

data "aws_subnet" "private_subnets" {
  count = length(var.availability_zones)
  filter {
    name   = "tag:Name"
    values = ["${var.network_resources["private_subnet_name"]}-${element(var.availability_zones, count.index)}"]
  }
}

data "aws_vpc" "nci_vpc" {
  filter {
    name   = "tag:Name"
    values = [var.network_resources["vpc_name"]]
  }
}

data "aws_security_group" "default_security_group" {
  id = var.security_group_id
}

data "aws_secretsmanager_secret" "google_secret" {
  name = "/nci/cdisc/gdrive"
}