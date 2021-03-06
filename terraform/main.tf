locals {
  lambda_configuration = defaults(var.lambda_configuration, { memory_in_mb = 1024
  timeout_in_mins = 5 })
  text_excel_report_generator_configuration = local.lambda_configuration["cdisc-text-excel-report-generator"]
  pairing_report_generator_configuration    = local.lambda_configuration["cdisc-pairing-report-generator"]
  excel_report_formatter_configuration      = local.lambda_configuration["cdisc-excel-report-formatter"]
  changes_report_generator_configuration    = local.lambda_configuration["cdisc-changes-report-generator"]
  odm_xml_report_generator_configuration    = local.lambda_configuration["cdisc-odm-xml-report-generator"]
  html_report_generator_configuration       = local.lambda_configuration["cdisc-html-report-generator"]
  pdf_report_generator_configuration        = local.lambda_configuration["cdisc-pdf-report-generator"]
  owl_report_generator_configuration        = local.lambda_configuration["cdisc-owl-report-generator"]
  post_process_report_configuration         = local.lambda_configuration["cdisc-post-process-report"]
  upload_report_configuration               = local.lambda_configuration["cdisc-upload-report"]
}

data external "versions"{
  program = ["bash", "../scripts/get-gradle-version.sh"]
  query = {
    "cdisc-text-excel-report-generator" = "../text-excel-reports"
    "cdisc-pairing-report-generator" = "../pairing-report"
    "cdisc-excel-report-formatter" = "../excel-formatting"
    "cdisc-changes-report-generator" = "../changes-report"
    "cdisc-odm-xml-report-generator" = "../odm-report"
    "cdisc-html-report-generator" = "../html-report"
    "cdisc-pdf-report-generator" = "../pdf-report"
    "cdisc-owl-report-generator" = "../owl-report"
    "cdisc-post-process-report" = "../post-process-reports"
    "cdisc-upload-report" = "../upload-reports"
  }
}

module "text_excel_report_generator_lambda" {
  source                       = "./modules/lambda-container-image"
  architecture                 = var.architecture
  function_name                = "cdisc-text-excel-report-generator"
  description                  = "Lambda that creates text and excel files from Thesaurus owl file for a specific concept"
  image_version                = data.external.versions.result.cdisc-text-excel-report-generator
  source_path                  = "../text-excel-reports"
  docker_file_path             = "./Dockerfile"
  timeout_in_seconds           = local.text_excel_report_generator_configuration.timeout_in_mins * 60
  memory_size_in_mb            = local.text_excel_report_generator_configuration.memory_in_mb
  subnet_ids                   = [data.aws_subnet.private_subnets[0].id]
  security_group_ids           = [data.aws_security_group.default_security_group.id]
  file_system_access_point_arn = aws_efs_access_point.cdisc_report_fs_ap.arn
  file_system_local_mount_path = "/mnt/cdisc"
  policies                     = [aws_iam_policy.cdisc_report_policy.arn]
}

module "pairing_report_generator_lambda" {
  source                       = "./modules/lambda-container-image"
  architecture                 = var.architecture
  function_name                = "cdisc-pairing-report-generator"
  description                  = "Lambda that creates pairing reports"
  image_version                = data.external.versions.result.cdisc-pairing-report-generator
  source_path                  = "../pairing-report"
  docker_file_path             = "./Dockerfile"
  timeout_in_seconds           = local.pairing_report_generator_configuration.timeout_in_mins * 60
  memory_size_in_mb            = local.pairing_report_generator_configuration.memory_in_mb
  subnet_ids                   = [data.aws_subnet.private_subnets[0].id]
  security_group_ids           = [data.aws_security_group.default_security_group.id]
  file_system_access_point_arn = aws_efs_access_point.cdisc_report_fs_ap.arn
  file_system_local_mount_path = "/mnt/cdisc"
  policies                     = [aws_iam_policy.cdisc_report_policy.arn]
}

module "excel_report_formatter_lambda" {
  source                       = "./modules/lambda-container-image"
  architecture                 = var.architecture
  function_name                = "cdisc-excel-report-formatter"
  description                  = "Lambda that formats excel reports created by cdisc-text-excel-report-generator"
  image_version                = data.external.versions.result.cdisc-excel-report-formatter
  source_path                  = "../excel-formatting"
  docker_file_path             = "./Dockerfile"
  timeout_in_seconds           = local.excel_report_formatter_configuration.timeout_in_mins * 60
  memory_size_in_mb            = local.excel_report_formatter_configuration.memory_in_mb
  subnet_ids                   = [data.aws_subnet.private_subnets[0].id]
  security_group_ids           = [data.aws_security_group.default_security_group.id]
  file_system_access_point_arn = aws_efs_access_point.cdisc_report_fs_ap.arn
  file_system_local_mount_path = "/mnt/cdisc"
  policies                     = [aws_iam_policy.cdisc_report_policy.arn]
}

module "changes_report_generator_lambda" {
  source                       = "./modules/lambda-container-image"
  architecture                 = var.architecture
  function_name                = "cdisc-changes-report-generator"
  description                  = "Lambda that creates a report detailing the differences between the current and previous text report"
  image_version                = data.external.versions.result.cdisc-changes-report-generator
  source_path                  = "../changes-report"
  docker_file_path             = "./Dockerfile"
  timeout_in_seconds           = local.changes_report_generator_configuration.timeout_in_mins * 60
  memory_size_in_mb            = local.changes_report_generator_configuration.memory_in_mb
  subnet_ids                   = [data.aws_subnet.private_subnets[0].id]
  security_group_ids           = [data.aws_security_group.default_security_group.id]
  file_system_access_point_arn = aws_efs_access_point.cdisc_report_fs_ap.arn
  file_system_local_mount_path = "/mnt/cdisc"
  policies                     = [aws_iam_policy.cdisc_report_policy.arn]
}

module "odm_xml_report_generator_lambda" {
  source                       = "./modules/lambda-container-image"
  architecture                 = var.architecture
  function_name                = "cdisc-odm-xml-report-generator"
  description                  = "Lambda that creates odm xml"
  image_version                = data.external.versions.result.cdisc-odm-xml-report-generator
  source_path                  = "../odm-report"
  docker_file_path             = "./Dockerfile"
  timeout_in_seconds           = local.odm_xml_report_generator_configuration.timeout_in_mins * 60
  memory_size_in_mb            = local.odm_xml_report_generator_configuration.memory_in_mb
  subnet_ids                   = [data.aws_subnet.private_subnets[0].id]
  security_group_ids           = [data.aws_security_group.default_security_group.id]
  file_system_access_point_arn = aws_efs_access_point.cdisc_report_fs_ap.arn
  file_system_local_mount_path = "/mnt/cdisc"
  policies                     = [aws_iam_policy.cdisc_report_policy.arn]
}

module "html_report_generator_lambda" {
  source                       = "./modules/lambda-container-image"
  architecture                 = var.architecture
  function_name                = "cdisc-html-report-generator"
  description                  = "Lambda that creates two HTML reports from the XML file created in cdisc-odm-xml-report-generator. One of the HTML files is the HTML report. The other file is a HTML file from which a PDF file will be generated in a subsequent step"
  image_version                = data.external.versions.result.cdisc-html-report-generator
  source_path                  = "../html-report"
  docker_file_path             = "./Dockerfile"
  timeout_in_seconds           = local.html_report_generator_configuration.timeout_in_mins * 60
  memory_size_in_mb            = local.html_report_generator_configuration.memory_in_mb
  subnet_ids                   = [data.aws_subnet.private_subnets[0].id]
  security_group_ids           = [data.aws_security_group.default_security_group.id]
  file_system_access_point_arn = aws_efs_access_point.cdisc_report_fs_ap.arn
  file_system_local_mount_path = "/mnt/cdisc"
  policies                     = [aws_iam_policy.cdisc_report_policy.arn]
}

module "pdf_report_generator_lambda" {
  source                       = "./modules/lambda-container-image"
  architecture                 = var.architecture
  function_name                = "cdisc-pdf-report-generator"
  description                  = "Lambda to create a PDF report from a HMTL file created by cdisc-html-report-generator"
  image_version                = data.external.versions.result.cdisc-pdf-report-generator
  source_path                  = "../pdf-report"
  docker_file_path             = "./Dockerfile"
  timeout_in_seconds           = local.pdf_report_generator_configuration.timeout_in_mins * 60
  memory_size_in_mb            = local.pdf_report_generator_configuration.memory_in_mb
  subnet_ids                   = [data.aws_subnet.private_subnets[0].id]
  security_group_ids           = [data.aws_security_group.default_security_group.id]
  file_system_access_point_arn = aws_efs_access_point.cdisc_report_fs_ap.arn
  file_system_local_mount_path = "/mnt/cdisc"
  policies                     = [aws_iam_policy.cdisc_report_policy.arn]
}

module "owl_report_generator_lambda" {
  source                       = "./modules/lambda-container-image"
  architecture                 = var.architecture
  function_name                = "cdisc-owl-report-generator"
  description                  = "Lambda to create a OWL file from the text file created by cdisc-text-excel-report-generator"
  image_version                = data.external.versions.result.cdisc-owl-report-generator
  source_path                  = "../owl-report"
  docker_file_path             = "./Dockerfile"
  timeout_in_seconds           = local.owl_report_generator_configuration.timeout_in_mins * 60
  memory_size_in_mb            = local.owl_report_generator_configuration.memory_in_mb
  subnet_ids                   = [data.aws_subnet.private_subnets[0].id]
  security_group_ids           = [data.aws_security_group.default_security_group.id]
  file_system_access_point_arn = aws_efs_access_point.cdisc_report_fs_ap.arn
  file_system_local_mount_path = "/mnt/cdisc"
  policies                     = [aws_iam_policy.cdisc_report_policy.arn]
}

module "post_process_lambda" {
  source                       = "./modules/lambda-container-image"
  architecture                 = var.architecture
  function_name                = "cdisc-post-process-reports"
  description                  = "Lambda to consolidate reports outputs and package reports"
  image_version                = data.external.versions.result.cdisc-post-process-report
  source_path                  = "../post-process-reports"
  docker_file_path             = "./Dockerfile"
  timeout_in_seconds           = local.post_process_report_configuration.timeout_in_mins * 60
  memory_size_in_mb            = local.post_process_report_configuration.memory_in_mb
  subnet_ids                   = [data.aws_subnet.private_subnets[0].id]
  security_group_ids           = [data.aws_security_group.default_security_group.id]
  file_system_access_point_arn = aws_efs_access_point.cdisc_report_fs_ap.arn
  file_system_local_mount_path = "/mnt/cdisc"
  policies                     = [aws_iam_policy.cdisc_report_policy.arn]
}

module "upload_report_lambda" {
  source                       = "./modules/lambda-container-image"
  architecture                 = var.architecture
  function_name                = "cdisc-upload-report"
  description                  = "Lambda that uploads all generated reports to GDrive"
  image_version                = data.external.versions.result.cdisc-upload-report
  source_path                  = "../upload-reports"
  docker_file_path             = "./Dockerfile"
  timeout_in_seconds           = local.upload_report_configuration.timeout_in_mins * 60
  memory_size_in_mb            = local.upload_report_configuration.memory_in_mb
  subnet_ids                   = [data.aws_subnet.private_subnets[0].id]
  security_group_ids           = [data.aws_security_group.default_security_group.id]
  file_system_access_point_arn = aws_efs_access_point.cdisc_report_fs_ap.arn
  file_system_local_mount_path = "/mnt/cdisc"
  policies                     = [aws_iam_policy.cdisc_report_policy.arn]
}

data "template_file" "step_function_definition" {
  template = file("${path.module}/state-definition.tfl")
  vars = {
    "text_excel_report_generator_arn" = module.text_excel_report_generator_lambda.arn
    "pairing_report_generator_arn"    = module.pairing_report_generator_lambda.arn
    "excel_report_formatter_arn"      = module.excel_report_formatter_lambda.arn
    "changes_report_generator_arn"    = module.changes_report_generator_lambda.arn
    "odm_xml_report_generator_arn"    = module.odm_xml_report_generator_lambda.arn
    "html_report_generator_arn"       = module.html_report_generator_lambda.arn
    "pdf_report_generator_arn"        = module.pdf_report_generator_lambda.arn
    "owl_report_generator_arn"        = module.owl_report_generator_lambda.arn
    "upload_report_arn"               = module.upload_report_lambda.arn
  }
}

resource "aws_sfn_state_machine" "step_function_state_machine" {
  definition = data.template_file.step_function_definition.rendered
  name       = "cdisc-report-state-machine"
  role_arn   = aws_iam_role.step_function_role.arn
}

data "aws_ecr_authorization_token" "token" {

}

provider "docker" {
  registry_auth {
    address  = split("/", data.aws_ecr_repository.main_ecr_repo.repository_url)[0]
    username = data.aws_ecr_authorization_token.token.user_name
    password = data.aws_ecr_authorization_token.token.password
  }
}

data "aws_ecr_repository" "main_ecr_repo" {
  name = "cdisc-report-generators"
}