# html-report Agent Context

## HTML Report Scope

This module converts ODM XML reports into human-readable HTML files using XSLT.
The Lambda handler consumes `ReportEnum.ODM_XML` and adds both `MAIN_HTML` and
`PDF_HTML`.

## ODM-to-HTML XSLT Flow

`HtmlReportGenerator` chooses the output file name and XSLT resource based on
the requested `ReportEnum`. `XsltTransformer` loads the XSLT resource from the
module classpath and writes the transformed HTML file.

## MAIN_HTML vs PDF_HTML

`MAIN_HTML` uses the standalone HTML transform and `.html` suffix. `PDF_HTML`
uses the PDF-targeted transform and `-pdf.html` suffix for later conversion by
`pdf-report`.

## ICH XSLT Branching

`IchHtmlReportGenerator` overrides the XSLT resource selection for ICH/M11
output. Keep ICH-specific transform selection in that subclass.

## Lambda Entry Point

`report/aws/LambdaHandler.java` validates summary fields, branches to the ICH
generator when the ODM file name contains `ich`, writes both HTML outputs, and
updates the report map.

## Core Logic Files

- `report/aws/LambdaHandler.java`
- `report/HtmlReportGenerator.java`
- `report/IchHtmlReportGenerator.java`
- `report/XsltTransformer.java`
- `src/main/resources/xslt/*.xsl`
- `src/test/java/gov/nih/nci/evs/cdisc/report/XstlTransformerTest.java`
