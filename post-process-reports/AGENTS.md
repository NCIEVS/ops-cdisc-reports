# post-process-reports Agent Context

## Step Function Post-processing Scope

This module consolidates nested Step Function branch responses and archives
generated files after all report branches complete.

## Nested Summary Merge

`PostProcessService.getReportSummary` accepts the raw nested `List` shape
returned by Step Functions, recursively extracts map nodes, converts them to
`ReportSummary`, groups `ReportDetail` values by concept code, and merges their
report maps.

## Archive Rules

`archiveFiles` copies report files listed in `ReportEnum.ARCHIVE_REPORTS` into
an `Archive` folder next to each report and appends the publication date to the
file name. Do not duplicate archive eligibility outside `ReportEnum`.

## Lambda Entry Point

`report/aws/LambdaHandler.java` accepts raw `List` input, delegates merge logic
to `PostProcessService`, archives files, and returns the flattened
`ReportSummary`.

## Core Logic Files

- `report/aws/LambdaHandler.java`
- `report/PostProcessService.java`
- `src/test/java/gov/nih/nci/evs/cdisc/report/PostProcessServiceTest.java`
- `src/test/resources/fixtures/lambda-request/step-function-request.json`
