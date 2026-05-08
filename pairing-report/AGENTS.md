# pairing-report Agent Context

## Pairing Report Scope

This module creates paired-term Excel reports directly from Thesaurus OWL input.
It is a separate generation path from the main text/excel pipeline and returns
its own `ReportSummary`.

## Source/Target Pairing Algorithm

Pairing logic identifies source and target CDISC terms using synonym metadata,
including CDISC preferred terms, source codes, NCI abbreviations, and CDISC
synonyms. Preserve the existing term-source and term-group matching semantics
when changing pairing behavior.

## Legacy vs V2 Pairing

`CDISCPairing` is the active legacy implementation used by the Lambda handler
and shared `CDISCScanner`.

`CDISCPairingV2` uses the V2 Thesaurus parser from `text-excel-reports` and
typed `PairedTerm`/`PairedTermData` models. Keep V2 changes aligned with the
legacy output tests before switching the handler.

## Excel Output Formatting

`XLSXFormatter` owns workbook layout for generated pairing files. Pairing output
uses `.xlsx`, not the legacy `.xls` files created by the main text/excel module.

## Lambda Entry Point

`report/aws/LambdaHandler.java` accepts `ThesaurusRequest`, loops requested
concept codes, and invokes `CDISCPairing.run` with NCIt OWL as the data source.

## Core Logic Files

- `report/aws/LambdaHandler.java`
- `report/CDISCPairing.java`
- `report/CDISCPairingV2.java`
- `report/CDISCPairingReport.java`
- `report/CDISCPairedRow.java`
- `report/model/CDISCRow.java`
- `report/model/PairedTerm.java`
- `report/model/PairedTermData.java`
- `report/utils/PairedTermDataComparator.java`
- `report/utils/XLSXFormatter.java`
- `src/test/java/gov/nih/nci/evs/cdisc/report/CDISCPairingTest.java`
