# ops-cdisc-reports Agent Context

## Global Architecture Overview

This repository is a Gradle multi-project Java 11 build. Treat each Gradle
subproject as a module boundary; the current tree does not define JPMS
`module-info.java` descriptors.

The system generates CDISC and ICH report artifacts from NCI Thesaurus OWL
input. AWS Step Functions orchestrate Lambda-style report steps. Most steps
consume either a `ThesaurusRequest` or a `ReportSummary`, then add generated
file paths to `ReportDetail.reports` using `ReportEnum` keys.

Shared contracts, scanners, cloud clients, and output path utilities live in
`common`. Report-producing subprojects should depend on `common` instead of
duplicating shared request, summary, validation, or path logic.

Runtime report paths are centered on `/mnt/cdisc/work`: current output,
previous reports, archive folders, and static OWL packaging files all derive
from that convention.

## Build/Test Commands

- Full build: `./gradlew clean build assemble`
- Full build without tests: `./gradlew clean build assemble -x test`
- Lambda zip build: `./gradlew clean buildZip`
- One module test run: `./gradlew :<module-name>:test`
- One module build: `./gradlew :<module-name>:clean :<module-name>:build`

Some report tests load OWL fixtures and need more heap. Several modules already
set `test.maxHeapSize = "4g"` in their `build.gradle`.

## Global Coding Standards

- Keep Gradle subproject boundaries clear. Put shared behavior in `common` only
  when more than one report module needs it.
- Preserve the Step Function data contract: handlers should validate required
  input, pass through existing summary fields, and add report paths under the
  correct `ReportEnum`.
- Prefer existing helpers such as `AssertUtils`, `ReportUtils`, `ZipUtils`,
  `CDISCScanner`, and `OWLScanner` before introducing new cross-cutting logic.
- Use Java 11-compatible APIs. Lombok is part of the shared Gradle convention.
- Tests use JUnit 5, AssertJ, Mockito, and shared fixtures from `common`
  where useful.
- Generated JAXB binding packages should be treated as generated code. Make
  targeted edits only when regeneration is not practical and document why.

## Context Map

Read the nearest `AGENTS.md` first, then the child file for deeper scope. Parent
files link to child contexts instead of repeating their rules.

- `buildSrc/AGENTS.md`: shared Gradle convention plugin and build tasks.
- `src/AGENTS.md`: root Google Drive quickstart app outside the report pipeline.
- `common/AGENTS.md`: shared report contracts, scanners, utilities, and cloud
  clients.
- `common/src/test/AGENTS.md`: shared test fixtures and assertion helpers.
- `text-excel-reports/AGENTS.md`: Thesaurus OWL to text and Excel reports.
- `text-excel-reports/src/main/java/gov/nih/nci/evs/cdisc/thesaurus/AGENTS.md`:
  V2 Thesaurus OWL parser and concept model.
- `changes-report/AGENTS.md`: current-vs-previous text diff report.
- `excel-formatting/AGENTS.md`: in-place Excel workbook formatting.
- `pairing-report/AGENTS.md`: pairing report generation.
- `odm-report/AGENTS.md`: Excel to ODM XML conversion.
- `odm-report/src/main/java/gov/nih/nci/evs/cdisc/report/xml/AGENTS.md`:
  generated ODM JAXB bindings.
- `owl-report/AGENTS.md`: text report to OWL/RDF output and OWL zip packaging.
- `owl-report/src/main/java/gov/nih/nci/evs/cdisc/report/xml/AGENTS.md`:
  generated RDF/OWL JAXB bindings.
- `html-report/AGENTS.md`: ODM XML to HTML via XSLT.
- `pdf-report/AGENTS.md`: PDF generation from PDF-targeted HTML.
- `post-process-reports/AGENTS.md`: Step Function response merge and archive.
- `upload-reports/AGENTS.md`: Google Drive upload and old-folder cleanup.
- `owl-reader/AGENTS.md`: standalone legacy OWL reader utility.
