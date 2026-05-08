# owl-report Agent Context

## OWL/RDF Report Scope

This module converts text reports into OWL/RDF output and packages generated
OWL files with required static schema files. The Lambda handler consumes
`ReportEnum.MAIN_TEXT` and adds `MAIN_OWL` and `OWL_ZIP`.

## Text-to-RDF Flow

The active handler branches by file name: ICH files use `IchRDFGenerator`; all
other files use `RDFGenerator`. The generated OWL file is written next to the
source text report.

`RDFGeneratorV2` is the newer JAXB-based path that converts text rows into
typed `TextReport` objects and marshals generated RDF binding types.

## Static Schema Packaging

`OwlZipFileGenerator` copies `ct-schema.owl` and `meta-model-schema.owl` from
`ReportUtils.getStaticFilesPath()` into the concept output folder before zipping
them with the generated OWL file.

## ICH vs CDISC Branching

Keep ICH-specific RDF behavior in `IchRDFGenerator` and handler selection.
Shared packaging should remain in `OwlZipFileGenerator`.

## Lambda Entry Point

`report/aws/LambdaHandler.java` validates summary fields, checks that each
`MAIN_TEXT` file exists, generates OWL, creates the zip, and updates report
paths.

## Core Logic Files

- `report/aws/LambdaHandler.java`
- `report/RDFGenerator.java`
- `report/IchRDFGenerator.java`
- `report/RDFGeneratorV2.java`
- `report/OwlZipFileGenerator.java`
- `report/model/TextReport.java`
- `src/test/java/gov/nih/nci/evs/cdisc/report/RDFGeneratorTest.java`
- `src/test/java/gov/nih/nci/evs/cdisc/report/OwlZipFileGeneratorTest.java`

## XML Binding Context Link

Generated RDF/OWL binding rules are documented in
`src/main/java/gov/nih/nci/evs/cdisc/report/xml/AGENTS.md`.
