locals {
  text_excel_report_generator_configuration = var.lambda_configuration["cdisc-text-excel-report-generator"]
  pairing_report_generator_configuration    = var.lambda_configuration["cdisc-pairing-report-generator"]
  excel_report_formatter_configuration      = var.lambda_configuration["cdisc-excel-report-formatter"]
  changes_report_generator_configuration    = var.lambda_configuration["cdisc-changes-report-generator"]
  odm_xml_report_generator_configuration    = var.lambda_configuration["cdisc-odm-xml-report-generator"]
  html_report_generator_configuration       = var.lambda_configuration["cdisc-html-report-generator"]
  pdf_report_generator_configuration        = var.lambda_configuration["cdisc-pdf-report-generator"]
  owl_report_generator_configuration        = var.lambda_configuration["cdisc-owl-report-generator"]
  post_process_report_configuration         = var.lambda_configuration["cdisc-post-process-report"]
  upload_report_configuration               = var.lambda_configuration["cdisc-upload-report"]
}

data "external" "versions" {
  program = ["bash", "../scripts/get-gradle-version.sh"]
  query = {
    "cdisc-text-excel-report-generator" = "../text-excel-reports"
    "cdisc-pairing-report-generator"    = "../pairing-report"
    "cdisc-excel-report-formatter"      = "../excel-formatting"
    "cdisc-changes-report-generator"    = "../changes-report"
    "cdisc-odm-xml-report-generator"    = "../odm-report"
    "cdisc-html-report-generator"       = "../html-report"
    "cdisc-pdf-report-generator"        = "../pdf-report"
    "cdisc-owl-report-generator"        = "../owl-report"
    "cdisc-post-process-report"         = "../post-process-reports"
    "cdisc-upload-report"               = "../upload-reports"
  }
}

module "text_excel_report_generator_lambda_zip" {
  source                       = "terraform-aws-modules/lambda/aws"
  version                      = "3.2.1"
  architectures                = [var.architecture]
  function_name                = "cdisc-text-excel-report-generator"
  description                  = "Lambda that creates text and excel files from Thesaurus owl file for a specific concept"
  handler                      = "gov.nih.nci.evs.cdisc.report.aws.LambdaHandler"
  runtime                      = "java11"
  role_name                    = "cdisc-text-excel-report-generator-lambda-role"
  create_package               = false
  timeout                      = local.text_excel_report_generator_configuration.timeout_in_mins * 60
  memory_size                  = local.text_excel_report_generator_configuration.memory_in_mb
  vpc_subnet_ids               = [data.aws_subnet.private_subnets[0].id]
  vpc_security_group_ids       = [data.aws_security_group.default_security_group.id]
  file_system_arn              = aws_efs_access_point.cdisc_report_fs_ap.arn
  file_system_local_mount_path = "/mnt/cdisc"
  attach_policies              = true
  number_of_policies           = 1
  policies                     = [aws_iam_policy.cdisc_report_policy.arn]
  local_existing_package       = "../text-excel-reports/build/distributions/text-excel-reports-${replace(data.external.versions.result.cdisc-text-excel-report-generator,"\r","")}.zip"
}

module "pairing_report_generator_lambda_zip" {
  source                       = "terraform-aws-modules/lambda/aws"
  version                      = "3.2.1"
  architectures                = [var.architecture]
  function_name                = "cdisc-pairing-report-generator"
  description                  = "Lambda that creates pairing reports"
  handler                      = "gov.nih.nci.evs.cdisc.report.aws.LambdaHandler"
  runtime                      = "java11"
  role_name                    = "cdisc-pairing-report-generator-lambda-role"
  create_package               = false
  timeout                      = local.pairing_report_generator_configuration.timeout_in_mins * 60
  memory_size                  = local.pairing_report_generator_configuration.memory_in_mb
  vpc_subnet_ids               = [data.aws_subnet.private_subnets[0].id]
  vpc_security_group_ids       = [data.aws_security_group.default_security_group.id]
  file_system_arn              = aws_efs_access_point.cdisc_report_fs_ap.arn
  file_system_local_mount_path = "/mnt/cdisc"
  attach_policies              = true
  number_of_policies           = 1
  policies                     = [aws_iam_policy.cdisc_report_policy.arn]
  local_existing_package       = "../pairing-report/build/distributions/pairing-report-${replace(data.external.versions.result.cdisc-pairing-report-generator,"\r","")}.zip"
}

module "excel_report_formatter_lambda_zip" {
  source                       = "terraform-aws-modules/lambda/aws"
  version                      = "3.2.1"
  architectures                = [var.architecture]
  function_name                = "cdisc-excel-report-formatter"
  description                  = "Lambda that formats excel reports created by cdisc-text-excel-report-generator"
  handler                      = "gov.nih.nci.evs.cdisc.report.aws.LambdaHandler"
  runtime                      = "java11"
  role_name                    = "cdisc-excel-report-formatter-lambda-role"
  create_package               = false
  timeout                      = local.excel_report_formatter_configuration.timeout_in_mins * 60
  memory_size                  = local.excel_report_formatter_configuration.memory_in_mb
  vpc_subnet_ids               = [data.aws_subnet.private_subnets[0].id]
  vpc_security_group_ids       = [data.aws_security_group.default_security_group.id]
  file_system_arn              = aws_efs_access_point.cdisc_report_fs_ap.arn
  file_system_local_mount_path = "/mnt/cdisc"
  attach_policies              = true
  number_of_policies           = 1
  policies                     = [aws_iam_policy.cdisc_report_policy.arn]
  local_existing_package       = "../excel-formatting/build/distributions/excel-formatting-${replace(data.external.versions.result.cdisc-excel-report-formatter,"\r","")}.zip"
}

module "changes_report_generator_lambda_zip" {
  source                       = "terraform-aws-modules/lambda/aws"
  version                      = "3.2.1"
  architectures                = [var.architecture]
  function_name                = "cdisc-changes-report-generator"
  description                  = "Lambda that creates a report detailing the differences between the current and previous text report"
  handler                      = "gov.nih.nci.evs.cdisc.report.aws.LambdaHandler"
  runtime                      = "java11"
  role_name                    = "cdisc-changes-report-generator-lambda-role"
  create_package               = false
  timeout                      = local.changes_report_generator_configuration.timeout_in_mins * 60
  memory_size                  = local.changes_report_generator_configuration.memory_in_mb
  vpc_subnet_ids               = [data.aws_subnet.private_subnets[0].id]
  vpc_security_group_ids       = [data.aws_security_group.default_security_group.id]
  file_system_arn              = aws_efs_access_point.cdisc_report_fs_ap.arn
  file_system_local_mount_path = "/mnt/cdisc"
  attach_policies              = true
  number_of_policies           = 1
  policies                     = [aws_iam_policy.cdisc_report_policy.arn]
  local_existing_package       = "../changes-report/build/distributions/changes-report-${replace(data.external.versions.result.cdisc-changes-report-generator,"\r","")}.zip"
}

module "odm_xml_report_generator_lambda_zip" {
  source                       = "terraform-aws-modules/lambda/aws"
  version                      = "3.2.1"
  architectures                = [var.architecture]
  function_name                = "cdisc-odm-xml-report-generator"
  description                  = "Lambda that creates odm xml"
  handler                      = "gov.nih.nci.evs.cdisc.report.aws.LambdaHandler"
  runtime                      = "java11"
  role_name                    = "cdisc-odm-xml-report-generator-lambda-role"
  create_package               = false
  timeout                      = local.odm_xml_report_generator_configuration.timeout_in_mins * 60
  memory_size                  = local.odm_xml_report_generator_configuration.memory_in_mb
  vpc_subnet_ids               = [data.aws_subnet.private_subnets[0].id]
  vpc_security_group_ids       = [data.aws_security_group.default_security_group.id]
  file_system_arn              = aws_efs_access_point.cdisc_report_fs_ap.arn
  file_system_local_mount_path = "/mnt/cdisc"
  attach_policies              = true
  number_of_policies           = 1
  policies                     = [aws_iam_policy.cdisc_report_policy.arn]
  local_existing_package       = "../odm-report/build/distributions/odm-report-${replace(data.external.versions.result.cdisc-odm-xml-report-generator,"\r","")}.zip"
}

module "html_report_generator_lambda_zip" {
  source                       = "terraform-aws-modules/lambda/aws"
  version                      = "3.2.1"
  architectures                = [var.architecture]
  function_name                = "cdisc-html-report-generator"
  description                  = "Lambda that creates two HTML reports from the XML file created in cdisc-odm-xml-report-generator. One of the HTML files is the HTML report. The other file is a HTML file from which a PDF file will be generated in a subsequent step"
  handler                      = "gov.nih.nci.evs.cdisc.report.aws.LambdaHandler"
  runtime                      = "java11"
  role_name                    = "cdisc-html-report-generator-lambda-role"
  create_package               = false
  timeout                      = local.html_report_generator_configuration.timeout_in_mins * 60
  memory_size                  = local.html_report_generator_configuration.memory_in_mb
  vpc_subnet_ids               = [data.aws_subnet.private_subnets[0].id]
  vpc_security_group_ids       = [data.aws_security_group.default_security_group.id]
  file_system_arn              = aws_efs_access_point.cdisc_report_fs_ap.arn
  file_system_local_mount_path = "/mnt/cdisc"
  attach_policies              = true
  number_of_policies           = 1
  policies                     = [aws_iam_policy.cdisc_report_policy.arn]
  local_existing_package       = "../html-report/build/distributions/html-report-${replace(data.external.versions.result.cdisc-html-report-generator,"\r","")}.zip"
}

module "pdf_report_generator_lambda_zip" {
  source                       = "terraform-aws-modules/lambda/aws"
  version                      = "3.2.1"
  architectures                = [var.architecture]
  function_name                = "cdisc-pdf-report-generator"
  description                  = "Lambda to create a PDF report from a HMTL file created by cdisc-html-report-generator"
  handler                      = "gov.nih.nci.evs.cdisc.report.aws.LambdaHandler"
  runtime                      = "java11"
  role_name                    = "cdisc-pdf-report-generator-lambda-role"
  create_package               = false
  timeout                      = local.pdf_report_generator_configuration.timeout_in_mins * 60
  memory_size                  = local.pdf_report_generator_configuration.memory_in_mb
  vpc_subnet_ids               = [data.aws_subnet.private_subnets[0].id]
  vpc_security_group_ids       = [data.aws_security_group.default_security_group.id]
  file_system_arn              = aws_efs_access_point.cdisc_report_fs_ap.arn
  file_system_local_mount_path = "/mnt/cdisc"
  attach_policies              = true
  number_of_policies           = 1
  policies                     = [aws_iam_policy.cdisc_report_policy.arn]
  local_existing_package       = "../pdf-report/build/distributions/pdf-report-${replace(data.external.versions.result.cdisc-pdf-report-generator,"\r","")}.zip"
}

module "owl_report_generator_lambda_zip" {
  source                       = "terraform-aws-modules/lambda/aws"
  version                      = "3.2.1"
  architectures                = [var.architecture]
  function_name                = "cdisc-owl-report-generator"
  description                  = "Lambda to create a OWL file from the text file created by cdisc-text-excel-report-generator"
  handler                      = "gov.nih.nci.evs.cdisc.report.aws.LambdaHandler"
  runtime                      = "java11"
  role_name                    = "cdisc-owl-report-generator-lambda-role"
  create_package               = false
  timeout                      = local.owl_report_generator_configuration.timeout_in_mins * 60
  memory_size                  = local.owl_report_generator_configuration.memory_in_mb
  vpc_subnet_ids               = [data.aws_subnet.private_subnets[0].id]
  vpc_security_group_ids       = [data.aws_security_group.default_security_group.id]
  file_system_arn              = aws_efs_access_point.cdisc_report_fs_ap.arn
  file_system_local_mount_path = "/mnt/cdisc"
  attach_policies              = true
  number_of_policies           = 1
  policies                     = [aws_iam_policy.cdisc_report_policy.arn]
  local_existing_package       = "../owl-report/build/distributions/owl-report-${replace(data.external.versions.result.cdisc-owl-report-generator,"\r","")}.zip"
}

module "post_process_lambda_zip" {
  source                       = "terraform-aws-modules/lambda/aws"
  version                      = "3.2.1"
  architectures                = [var.architecture]
  function_name                = "cdisc-post-process-reports"
  description                  = "Lambda to consolidate reports outputs and package reports"
  handler                      = "gov.nih.nci.evs.cdisc.report.aws.LambdaHandler"
  runtime                      = "java11"
  role_name                    = "cdisc-post-process-reports-lambda-role"
  create_package               = false
  timeout                      = local.post_process_report_configuration.timeout_in_mins * 60
  memory_size                  = local.post_process_report_configuration.memory_in_mb
  vpc_subnet_ids               = [data.aws_subnet.private_subnets[0].id]
  vpc_security_group_ids       = [data.aws_security_group.default_security_group.id]
  file_system_arn              = aws_efs_access_point.cdisc_report_fs_ap.arn
  file_system_local_mount_path = "/mnt/cdisc"
  attach_policies              = true
  number_of_policies           = 1
  policies                     = [aws_iam_policy.cdisc_report_policy.arn]
  local_existing_package       = "../post-process-reports/build/distributions/post-process-reports-${replace(data.external.versions.result.cdisc-post-process-report,"\r","")}.zip"
}

module "upload_report_lambda_zip" {
  source                       = "terraform-aws-modules/lambda/aws"
  version                      = "3.2.1"
  architectures                = [var.architecture]
  function_name                = "cdisc-upload-report"
  description                  = "Lambda that uploads all generated reports to GDrive"
  handler                      = "gov.nih.nci.evs.cdisc.report.aws.LambdaHandler"
  runtime                      = "java11"
  role_name                    = "cdisc-upload-report-lambda-role"
  create_package               = false
  timeout                      = local.owl_report_generator_configuration.timeout_in_mins * 60
  memory_size                  = local.owl_report_generator_configuration.memory_in_mb
  vpc_subnet_ids               = [data.aws_subnet.private_subnets[0].id]
  vpc_security_group_ids       = [data.aws_security_group.default_security_group.id]
  file_system_arn              = aws_efs_access_point.cdisc_report_fs_ap.arn
  file_system_local_mount_path = "/mnt/cdisc"
  attach_policies              = true
  number_of_policies           = 1
  policies                     = [aws_iam_policy.cdisc_report_policy.arn]
  local_existing_package       = "../upload-reports/build/distributions/upload-reports-${replace(data.external.versions.result.cdisc-upload-report,"\r","")}.zip"
}

data "template_file" "step_function_definition" {
  template = file("${path.module}/state-definition.tfl")
  vars = {
    "text_excel_report_generator_arn" = module.text_excel_report_generator_lambda_zip.lambda_function_arn
    "pairing_report_generator_arn"    = module.pairing_report_generator_lambda_zip.lambda_function_arn
    "excel_report_formatter_arn"      = module.excel_report_formatter_lambda_zip.lambda_function_arn
    "changes_report_generator_arn"    = module.changes_report_generator_lambda_zip.lambda_function_arn
    "odm_xml_report_generator_arn"    = module.odm_xml_report_generator_lambda_zip.lambda_function_arn
    "html_report_generator_arn"       = module.html_report_generator_lambda_zip.lambda_function_arn
    "pdf_report_generator_arn"        = module.pdf_report_generator_lambda_zip.lambda_function_arn
    "owl_report_generator_arn"        = module.owl_report_generator_lambda_zip.lambda_function_arn
    "upload_report_arn"               = module.upload_report_lambda_zip.lambda_function_arn
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