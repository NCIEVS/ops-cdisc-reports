# excel-formatting Agent Context

## Excel Formatting Scope

This module formats the legacy Excel files produced by `text-excel-reports`.
It mutates the workbook in place and generally does not add a new report path to
the pipeline summary.

## In-place Workbook Mutation

The Lambda handler reads `ReportEnum.MAIN_EXCEL`, reformats the workbook, sets
metadata, and saves back to the same path. Downstream modules expect the same
file path to remain valid.

## Metadata Rules

Workbook metadata should remain consistent with the current title, author, and
publication date behavior in `CDISCExcelUtils`. Be careful when changing sheet
names, merged cells, header styles, or metadata because downstream ODM
conversion reads these workbooks.

## Lambda Entry Point

`report/aws/LambdaHandler.java` validates `reportDetails` and `publicationDate`,
then applies `CDISCExcelUtils` to every `MAIN_EXCEL` file in the summary.

## Core Logic Files

- `report/aws/LambdaHandler.java`
- `report/CDISCExcelUtils.java`
- `report/CDISCExcelUtilsV2.java`
- `src/test/java/gov/nih/nci/evs/cdisc/report/CDISCExcelUtilsTest.java`
