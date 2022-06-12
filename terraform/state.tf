terraform {
  backend "s3" {
    bucket         = "wci-us-west-2"
    key            = "NCI/cdisc-reports/tf-state"
    dynamodb_table = "terraform-lock-table"
    region         = "us-west-2"
  }
}