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
          "arn:aws:secretsmanager:*:340229005005:secret:*",
          "arn:aws:lambda:us-west-2:340229005005:function:*"
        ]
      },
      {
        Effect = "Allow",
        Action = [
          "ec2:CreateNetworkInterface",
          "elasticfilesystem:*",
          "iam:PassRole",
          "ec2:DescribeNetworkInterfaces",
          "s3:*",
          "ec2:DeleteNetworkInterface",
          "datasync:*"
        ],
        Resource = "*"
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
        Resource = "arn:aws:lambda:us-west-2:340229005005:function:cdisc*"
      }
    ]
  })
}

