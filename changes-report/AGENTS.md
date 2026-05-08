# changes-report Agent Context

## Changes Report Scope

This module compares current text reports against previous text reports and
adds a changes text report to each `ReportDetail`.

The Lambda handler consumes an existing `ReportSummary`, reads `MAIN_TEXT`, and
adds `CHANGES_TEXT` when a previous report exists.

## Current-vs-Previous Diff Flow

Current reports come from `ReportDetail.reports[MAIN_TEXT]`. Previous reports
are resolved from `/mnt/cdisc/work/previous` by matching the current report file
name. Missing previous reports are skipped by the Lambda handler.

## Legacy vs V2 Diff Logic

`ChangesReport` is the active legacy implementation used by the Lambda handler.
It parses tab-delimited text into codelist and element maps, calculates
add/remove/change operations, and prints the output.

`ChangesReportV2` is a newer implementation using typed V2 models and Guava
`MapDifference`. Keep V2 changes covered by focused tests before wiring it into
the runtime handler.

## Core Logic Files

- `report/aws/LambdaHandler.java`
- `report/ChangesReport.java`
- `report/ChangesReportV2.java`
- `report/model/Change.java`
- `report/model/ChangeV2.java`
- `report/model/Codelist.java`
- `report/model/CodeListV2.java`
- `report/model/Element.java`
- `report/model/ElementV2.java`
- `src/test/java/gov/nih/nci/evs/cdisc/report/ChangesReportTest.java`
- `src/test/java/gov/nih/nci/evs/cdisc/report/aws/LambdaHandlerTest.java`
