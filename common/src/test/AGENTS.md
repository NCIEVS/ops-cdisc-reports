# common Test Agent Context

## Shared Test Utilities

This tree provides reusable test fixtures and assertions for other Gradle
subprojects through `testImplementation project(":common").sourceSets.test.output`.
Keep helpers stable and broadly useful.

## Fixture Layout

`src/test/resources/fixtures` contains abridged OWL inputs and expected report
artifacts. Report modules use these fixtures to avoid committing full Thesaurus
OWL files.

The `report-files` fixture mirrors generated output structure by terminology
area, including archive folders and static OWL files. Preserve this shape when
adding new shared fixtures.

## Assertion Helpers

`AssertExcelFiles` compares workbook structure, sheets, rows, and primitive cell
values. Use it for generated Excel files instead of ad hoc workbook comparison.

`TestUtils` resolves resource paths and streams while handling URL decoding for
fixture names containing spaces.

## Core Test Files

- `gov/nih/nci/evs/test/Fixtures.java`: common request and summary builders.
- `gov/nih/nci/evs/test/utils/TestUtils.java`: fixture path and stream helpers.
- `gov/nih/nci/evs/test/utils/AssertExcelFiles.java`: workbook comparison
  helper.
- `gov/nih/nci/evs/cdisc/report/utils/ReportUtilsTest.java`: shared path and
  label utility tests.
