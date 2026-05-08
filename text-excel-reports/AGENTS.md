# text-excel-reports Agent Context

## Text/Excel Report Pipeline

This module is the first report-generation step. It reads a Thesaurus OWL file
and concept codes, then produces the main tab-delimited text report and legacy
Excel report for each concept.

The Lambda handler accepts `ThesaurusRequest` and returns a new `ReportSummary`
containing `MAIN_TEXT` and `MAIN_EXCEL` paths in each `ReportDetail`.

## Lambda Entry Point

`src/main/java/gov/nih/nci/evs/cdisc/report/aws/LambdaHandler.java` validates
`conceptCodes`, `publicationDate`, and `thesaurusOwlFile`, loops through each
concept code, and delegates generator creation to `TextExcelReportGeneratorFactory`.

## Legacy vs V2 Generators

`TextExcelReportGenerator` uses the shared legacy `CDISCScanner`.
`TextExcelReportGeneratorV2` uses the local V2 Thesaurus parser documented in
`src/main/java/gov/nih/nci/evs/cdisc/thesaurus/AGENTS.md`.

The factory currently returns the legacy generator. If switching runtime behavior
to V2, update the factory and run the parameterized generator tests.

## Sorting/Formatting Rules

Text rows are sorted using module-local comparators in `report/util`. Excel
output is created from the text report using the report-writer formatter and
then formatted by later pipeline modules.

Output directories should be resolved through `ReportUtils.getBaseOutputDirectory`
and `ReportUtils.getShortCodeLabel`.

## Core Logic Files

- `report/aws/LambdaHandler.java`
- `report/TextExcelReportGeneratorFactory.java`
- `report/TextExcelReportGenerator.java`
- `report/TextExcelReportGeneratorV2.java`
- `report/util/SortUtils.java`
- `report/util/SortComparator.java`
- `report/util/SortComparatorV2.java`
- `report/util/TextReportLineComparator.java`
- `src/test/java/gov/nih/nci/evs/cdisc/report/TextExcelReportGeneratorTest.java`
- `src/test/java/gov/nih/nci/evs/cdisc/report/aws/LambdaHandlerTest.java`

## Thesaurus Context Link

The V2 OWL parser, concept model, and handler pattern are documented in
`src/main/java/gov/nih/nci/evs/cdisc/thesaurus/AGENTS.md`.
