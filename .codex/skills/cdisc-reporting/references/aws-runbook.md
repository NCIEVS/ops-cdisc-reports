# AWS Runbook

This reference distills the Confluence page `Running CDISC Reports in AWS` (page id `1642004534`) together with repo facts from `/Users/squareroot/wci/ncievs-CDISC/ops-cdisc-reports/README.md`.

If the user needs the latest version, run [../scripts/fetch_wiki_runbook.sh](../scripts/fetch_wiki_runbook.sh).

## Prerequisites

- `openjdk` 11.0.12
- `terraform` 1.11.2
- `gradle` 7.4.2
- Optional but useful:
  - `aws` CLI 2.5.6
  - `git`

The wiki notes that minor version drift is usually fine, but major upgrades or downgrades may not be.

## Build And Deploy

From repo root:

```bash
git status
git pull
./gradlew clean buildZip
```

If tests are too slow or blocked:

```bash
./gradlew clean buildZip -x test
```

From `/Users/squareroot/wci/ncievs-CDISC/ops-cdisc-reports/terraform`:

```bash
terraform init
terraform apply -destroy -var-file dev.tfvars
terraform apply -var-file dev.tfvars
```

The runbook expects AWS region `us-west-2`.

Minimal AWS config:

```ini
[default]
region = us-west-2
```

Minimal AWS credentials file shape:

```ini
[default]
aws_access_key_id =
aws_secret_access_key =
```

## High-Level AWS Flow

1. Build Lambda zip artifacts with Gradle.
2. Apply Terraform from `terraform/`.
3. Upload the new OWL file to S3.
4. Upload prior quarter text reports to S3 under the `work/Previous` location.
5. Run the `s3-to-efs-ds-task` DataSync job to move files onto EFS at `/mnt/cdisc`.
6. If DataSync succeeds, proceed to the Step Function with a tailored JSON input.
7. Optionally verify EFS contents only when troubleshooting file-access or mount issues.
8. Review outputs in Google Drive and communicate the distribution.
9. Destroy AWS resources when finished if they are not needed.

## S3 And EFS Details

Upload to:

- `s3://wci-us-west-2/NCI/Thesaurus/`
- previous-quarter text reports under `s3://wci-us-west-2/NCI/Thesaurus/work/Previous`

The wiki explicitly says to remove stale files in those folders before uploading the new set.

In guided mode, ask the user for:

- the local path to the OWL file to upload
- the local path to the previous-quarter text files, or the directory containing them

Validate those paths before uploading.

If using AWS CLI, the practical upload targets are:

- OWL file to `s3://wci-us-west-2/NCI/Thesaurus/`
- previous-quarter text files to `s3://wci-us-west-2/NCI/Thesaurus/work/Previous/`

For directories of prior-quarter text files, a recursive copy may be needed.

## DataSync Execution

The runbook names the task `s3-to-efs-ds-task`.

In guided mode:

1. Start the DataSync task.
2. Capture the task execution id or ARN.
3. Poll execution status and report progress to the user until it reaches a terminal state.
4. If the execution fails, report that immediately and stop.
5. If the execution succeeds, treat that as sufficient confirmation for normal operator flow and continue to Step Function input preparation.

Optional troubleshooting-only EFS verification from the Lambda helper:

```bash
# in list-efs-3, make sure hello.sh includes:
echo $(ls -ltr /mnt/cdisc)
```

Then invoke:

```bash
aws lambda invoke --function-name list-efs-3 /dev/stdout
```

## Standard CDISC Step Function Input

The README shows the base shape and the wiki extends the current concept list:

```json
{
  "thesaurusOwlFile": "/mnt/cdisc/Thesaurus-*editthiswithweeklyversion*",
  "publicationDate": "YYYY-MM-DD",
  "conceptCodes": [
    "C81222",
    "C77527",
    "C67497",
    "C120166",
    "C66830",
    "C77526",
    "C165634",
    "C188693",
    "C203912",
    "C207069"
  ],
  "deliveryEmailAddresses": [
    "akuppusamy@westcoastinformatics.com",
    "jliu@westcoastinformatics.com",
    "dshapiro@westcoastinformatics.com"
  ],
  "pairingReportRequest": {
    "thesaurusOwlFile": "/mnt/cdisc/Thesaurus-*editthiswithweeklyversion*",
    "publicationDate": "YYYY-MM-DD",
    "conceptCodes": [
      "C81222",
      "C66830",
      "C77526"
    ]
  }
}
```

The wiki also notes:

- If this is week 2 or week 3, remove concept codes for subsources that are not updated in that cycle.
- `C132298` / Protocol Terminology is retired and should not be included in standard CDISC runs.
- A typical Step Function run takes about 15 to 20 minutes.
- The listed email addresses receive Google Drive sharing notifications after completion.

## ICH Step Function Input

```json
{
  "thesaurusOwlFile": "/mnt/cdisc/Thesaurus-*editthiswithweeklyversion*",
  "publicationDate": "YYYY-MM-DD",
  "conceptCodes": [
    "C217023"
  ],
  "deliveryEmailAddresses": [
    "akuppusamy@westcoastinformatics.com",
    "jliu@westcoastinformatics.com",
    "dshapiro@westcoastinformatics.com"
  ],
  "skipPairingReport": true
}
```

## Skip Flags And Reruns

The README says report steps can be skipped with root-level `skip<REPORT_NAME>` flags, for example `skipTextExcelReport`.

The runbook adds these constraints:

- Jobs that produce no data, such as post-process and upload, should not be skipped.
- If `text-excel-reports` is skipped, the input must already include `reportDetails`.
- Manual edits to generated `.txt` or `.xls` files have downstream consequences:
  - `.txt` feeds the changes report
  - `.xls` feeds paired Excel, ODM, PDF, and HTML outputs

Example rerun payload when text and pairing generation are skipped:

```json
{
  "publicationDate": "2024-03-29",
  "deliveryEmailAddresses": [
    "akuppusamy@westcoastinformatics.com",
    "jliu@westcoastinformatics.com",
    "dshapiro@westcoastinformatics.com"
  ],
  "reportDetails": [
    {
      "code": "C81222",
      "label": "CDISC ADaM Terminology",
      "reports": {
        "MAIN_TEXT": "/mnt/cdisc/work/current/ADaM/ADaM Terminology.txt",
        "MAIN_EXCEL": "/mnt/cdisc/work/current/ADaM/ADaM Terminology.xls"
      }
    },
    {
      "code": "C77527",
      "label": "CDISC CDASH Terminology",
      "reports": {
        "MAIN_TEXT": "/mnt/cdisc/work/current/CDASH/CDASH Terminology.txt",
        "MAIN_EXCEL": "/mnt/cdisc/work/current/CDASH/CDASH Terminology.xls"
      }
    }
  ],
  "skipTextExcelReport": true,
  "skipPairingReport": true,
  "skipExcelFormatter": true
}
```

In practice, expand `reportDetails` to cover every concept you intend downstream steps to use.

## Replacing Files

When reports already exist and a subset must be replaced:

1. Upload replacement files to `s3://wci-us-west-2/NCI/Thesaurus/`.
2. Run DataSync `s3-to-efs-ds-task` to move them to `/mnt/cdisc/`.
3. Edit the `list-efs-2` Lambda logic if needed to move files into the appropriate `/mnt/cdisc/work/current/...` directory.
4. Re-run the Step Function with the appropriate skip flags.

## Post-Run Review

The wiki instructs operators to:

- review the shared Google Drive folder
- delete stale paired files
- check each PDF for correct release dates and null values
- note which sources were not updated that cycle
- send a distribution email with the Drive link

## Cleanup

Delete old Drive folders by invoking `upload-reports` with:

```json
{"deleteOldReportsThresholdDays": 90}
```

Destroy AWS resources when done:

```bash
terraform apply -destroy -var-file dev.tfvars
```
