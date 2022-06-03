variable "architecture" {
  type = string
}

variable "function_name" {
  type = string
}

variable "description" {
  type = string
}

variable "image_version" {
  type = string
}

variable "source_path" {
  type = string
}

variable "docker_file_path" {
  type = string
}

variable "timeout_in_seconds" {
  type    = number
  default = 3
}

variable "memory_size_in_mb" {
  type    = number
  default = 1024
}

variable "subnet_ids" {
  type = list(string)
}

variable "security_group_ids" {
  type = list(string)
}

variable "file_system_access_point_arn" {
  type = string
}

variable "file_system_local_mount_path" {
  type = string
}

variable "policies" {
  type = list(string)
}
