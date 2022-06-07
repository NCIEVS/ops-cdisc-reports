resource "aws_iam_policy" "cdisc_report_policy" {
  name = "cdisc-report-policy"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue",
          "secretsmanager:DescribeSecret",
          "lambda:InvokeFunction"
        ],
        Resource = [
          data.aws_secretsmanager_secret.google_secret.arn,
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "elasticfilesystem:ClientMount",
          "elasticfilesystem:ClientWrite"
        ],
        Resource = [
          aws_efs_file_system.cdisc_report_fs.arn,
        ]
      }
    ]
  })
}

resource "aws_iam_role" "step_function_role" {
  name = "cdisc-report-sf-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = ["sts:AssumeRole"]
        Principal = {
          Service = ["states.amazonaws.com"]
        }
      }
    ]
  })
}

resource "aws_iam_role_policy" "step_function_policy" {
  name = "cdisc-report-sf-policy"
  role = aws_iam_role.step_function_role.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow",
        Action   = "lambda:InvokeFunction",
        Resource = "arn:aws:lambda:${var.aws_region}:${local.account_id}:function:cdisc*"
      }
    ]
  })
}

