variable "aws_profile" {
  type = string
}

variable "aws_region" {
  type = string
}

variable "aws_tags" {
  type = map(string)
  default = {
    "Application" = "cdisc-reports"
    "Client"      = "NCI"
  }
}

variable "availability_zones" {
  type    = list(string)
  default = ["us-west-2a"]
}

variable "network_resources" {
  type = map(string)
}

variable "security_group_id" {
  type = string
}

variable "thesaurus_bucket" {
  type = string
}

variable "architecture" {
  type = string
}

variable "lambda_configuration" {
  type = map(object({ version = string, memory_in_mb = optional(number), timeout_in_mins = optional(number) }))
}

variable "file_system_local_mount_path" {
  type    = string
  default = "/mnt/cdisc"
}
