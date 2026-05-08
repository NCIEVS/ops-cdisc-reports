# pdf-report Agent Context

## PDF Report Scope

This module converts PDF-targeted HTML into final PDF reports. The Lambda
handler consumes `ReportEnum.PDF_HTML` and adds `ReportEnum.MAIN_PDF`.

## HTML-to-PDF Flow

`HtmlToPdfConverter.convert` opens the HTML file, writes to the target PDF path,
sets A3 page size, and uses the source HTML directory as the base URI so local
assets resolve correctly.

## iText Usage

The converter uses iText `html2pdf` and a `PdfDocument` event handler. When
changing rendering behavior, verify pagination, base URI handling, and footer
output.

## Footer/Page Events

`FooterEventHandler` handles page footer rendering and writes total page counts
after conversion. Keep footer changes covered by generated PDF tests where
practical.

## Lambda Entry Point

`report/aws/LambdaHandler.java` validates summary fields, converts every
existing `PDF_HTML` file, and writes the result by replacing `-pdf.html` with
`.pdf`.

## Core Logic Files

- `report/aws/LambdaHandler.java`
- `report/HtmlToPdfConverter.java`
- `report/FooterEventHandler.java`
- `src/test/java/gov/nih/nci/evs/cdisc/report/HtmlToPdfConverterTest.java`
