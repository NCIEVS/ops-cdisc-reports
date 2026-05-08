# common Agent Context

## Shared Module Scope

`common` owns cross-module contracts, scanner utilities, report path utilities,
and cloud clients used by report-producing subprojects. Add behavior here only
when it is genuinely shared across modules.

## Shared Contracts

The pipeline data contract is centered on:

- `ReportEnum`: report artifact keys and archive selection.
- `ReportSummary`: Step Function and Lambda summary object.
- `ReportDetail`: per-concept generated artifact map.
- `ThesaurusRequest`: initial OWL/concept/publication request.

Report modules should extend these contracts carefully because downstream
handlers depend on existing field names and `ReportEnum` keys.

## AWS/GCP Clients

- `aws/SecretsClient.java`: reads AWS Secrets Manager values.
- `gcp/GoogleDriveClient.java`: wraps Google Drive folder, file upload,
  permission, and cleanup operations.

Keep cloud clients here so report modules do not duplicate credential or Drive
API behavior.

## Scanner/Utility Patterns

`report/utils` contains shared validation, output path, ZIP, and OWL scanning
code. `OWLScanner.java` and `CDISCScanner.java` are large legacy scanners used
by several legacy generators; prefer targeted changes and focused regression
tests when touching them.

`ReportUtils` owns the `/mnt/cdisc/work` path convention. Do not hard-code new
base output paths in report modules when an existing helper can resolve them.

## Core Logic Files

- `report/ReportEnum.java`
- `report/model/ReportSummary.java`
- `report/model/ReportDetail.java`
- `report/model/ThesaurusRequest.java`
- `report/model/Synonym.java`
- `report/utils/AssertUtils.java`
- `report/utils/ReportUtils.java`
- `report/utils/CDISCScanner.java`
- `report/utils/OWLScanner.java`
- `report/utils/ZipUtils.java`
- `aws/SecretsClient.java`
- `gcp/GoogleDriveClient.java`

## Test Context Link

Shared fixtures and reusable test helpers are documented in
`common/src/test/AGENTS.md`.
