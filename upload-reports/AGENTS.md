# upload-reports Agent Context

## Upload Scope

This module uploads generated report folders to Google Drive or deletes old
Google Drive report folders. It is the delivery step after report generation
and post-processing.

## Google Drive Client Usage

`UploadReportsService` depends on `common`'s `GoogleDriveClient`. It creates a
timestamped target folder, recursively uploads the local output folder, and
grants write permissions to requested email addresses.

## Secrets Manager Boundary

The Lambda handler obtains Google Drive credentials from AWS Secrets Manager
through `SecretsClient.getSecret("/nci/cdisc/gdrive")`. Keep secret lookup in
the handler boundary and inject `GoogleDriveClient` into service logic for
testability.

## Delete-old-reports Mode

If `ReportSummary.deleteOldReportsThresholdDays` is present, the handler skips
upload validation and deletes old folders instead. Otherwise it requires
`deliveryEmailAddresses` and uploads `ReportUtils.getBaseOutputDirectory()`.

## Lambda Entry Point

`report/aws/LambdaHandler.java` accepts `ReportSummary`, chooses upload or
delete mode, and returns the input summary unchanged.

## Core Logic Files

- `report/aws/LambdaHandler.java`
- `report/UploadReportsService.java`
