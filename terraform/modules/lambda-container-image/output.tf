output "arn" {
  value = module.lambda_container_image.lambda_function_arn
}

output "invoke_arn" {
  value = module.lambda_container_image.lambda_function_invoke_arn
}

output "function_name" {
  value = module.lambda_container_image.lambda_function_invoke_arn
}