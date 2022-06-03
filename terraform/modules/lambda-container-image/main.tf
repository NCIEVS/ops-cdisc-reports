terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "2.16.0"
    }
    aws = {
      source  = "hashicorp/aws"
      version = "4.16.0"
    }
  }
}

provider "docker" {
  registry_auth {
    address  = split("/", data.aws_ecr_repository.ecr_repo.repository_url)[0]
    username = data.aws_ecr_authorization_token.token.user_name
    password = data.aws_ecr_authorization_token.token.password
  }
}

data "aws_region" "current" {

}
data "aws_caller_identity" "this" {

}

data "aws_ecr_repository" "ecr_repo" {
  name = "cdisc-report-generators"
}

data "aws_ecr_authorization_token" "token" {

}

locals {
  image_tag = format("%s-%s", var.function_name, var.image_version)
  policies  = concat(var.policies, ["arn:aws:iam::aws:policy/service-role/AWSLambdaVPCAccessExecutionRole"])
}

module "lambda_container_image" {
  source                       = "terraform-aws-modules/lambda/aws"
  version                      = "3.2.1"
  architectures                = [var.architecture]
  function_name                = var.function_name
  description                  = var.description
  role_name                    = "${var.function_name}-lambda-role"
  create_package               = false
  build_in_docker              = true
  image_uri                    = docker_registry_image.this.name
  package_type                 = "Image"
  timeout                      = var.timeout_in_seconds
  memory_size                  = var.memory_size_in_mb
  vpc_subnet_ids               = var.subnet_ids
  vpc_security_group_ids       = var.security_group_ids
  file_system_arn              = var.file_system_access_point_arn
  file_system_local_mount_path = var.file_system_local_mount_path
  attach_policies              = true
  number_of_policies           = length(local.policies)
  policies                     = local.policies
}

resource "docker_registry_image" "this" {
  name = format("%s:%s", data.aws_ecr_repository.ecr_repo.repository_url, local.image_tag)
  build {
    context    = var.source_path
    dockerfile = var.docker_file_path
  }
  keep_remotely = true
}
