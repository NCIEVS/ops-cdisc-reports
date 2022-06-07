resource "aws_efs_file_system" "cdisc_report_fs" {
}

resource "aws_efs_access_point" "cdisc_report_fs_ap" {
  file_system_id = aws_efs_file_system.cdisc_report_fs.id
  posix_user {
    gid            = 65534
    uid            = 65534
    secondary_gids = [65534]
  }
  root_directory {
    path = "/"
    creation_info {
      owner_gid   = 65534
      owner_uid   = 65534
      permissions = 755
    }
  }
}

resource "aws_efs_mount_target" "cdisc_report_fs_target" {
  file_system_id = aws_efs_file_system.cdisc_report_fs.id
  subnet_id      = data.aws_subnet.private_subnets[0].id
}

resource "aws_datasync_location_efs" "efs_ds_location" {
  efs_file_system_arn = aws_efs_mount_target.cdisc_report_fs_target.file_system_arn

  ec2_config {
    security_group_arns = [data.aws_security_group.default_security_group.arn]
    subnet_arn          = data.aws_subnet.private_subnets[0].arn
  }
}

data "aws_s3_bucket" "thesaurus_bucket" {
  bucket = var.thesaurus_bucket
}

data "aws_iam_policy_document" "data_sync_assume_role_policy" {
  version = "2012-10-17"
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["datasync.amazonaws.com"]
    }
  }
}
resource "aws_iam_role" "data_sync_role" {
  name               = "cdisc-report-data-sync-role"
  assume_role_policy = data.aws_iam_policy_document.data_sync_assume_role_policy.json
}

resource "aws_iam_role_policy" "cdisc_report_data_sync_policy" {
  name = "cdisc-report-data-sync-policy"
  role = aws_iam_role.data_sync_role.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow",
        Action = [
          "datasync:CreateLocationEfs",
          "datasync:CreateTask"
        ],
        Resource = [aws_efs_file_system.cdisc_report_fs.arn,data.aws_s3_bucket.thesaurus_bucket.arn]
      },
      {
        "Action": [
          "s3:GetBucketLocation",
          "s3:ListBucket",
          "s3:ListBucketMultipartUploads"
        ],
        "Effect": "Allow",
        "Resource": data.aws_s3_bucket.thesaurus_bucket.arn
      },
      {
        "Action": [
          "s3:AbortMultipartUpload",
          "s3:DeleteObject",
          "s3:GetObject",
          "s3:ListMultipartUploadParts",
          "s3:GetObjectTagging",
          "s3:PutObjectTagging",
          "s3:PutObject"
        ],
        "Effect": "Allow",
        "Resource": "${data.aws_s3_bucket.thesaurus_bucket.arn}/*"
      },
      {
        Effect = "Allow"
        Action = [
          "elasticfilesystem:ClientMount",
          "elasticfilesystem:ClientWrite"
        ],
        Resource = [
          aws_efs_file_system.cdisc_report_fs.arn
        ]
      }
    ]
  })
}

resource "aws_datasync_location_s3" "s3_ds_location" {
  s3_bucket_arn = data.aws_s3_bucket.thesaurus_bucket.arn
  subdirectory  = "/NCI/Thesaurus"

  s3_config {
    bucket_access_role_arn = aws_iam_role.data_sync_role.arn
  }
}

resource "aws_datasync_task" "s3_to_efs_ds_task" {
  source_location_arn      = aws_datasync_location_s3.s3_ds_location.arn
  destination_location_arn = aws_datasync_location_efs.efs_ds_location.arn
  name                     = "s3-to-efs-ds-task"

  options {
    bytes_per_second = -1
  }
}
