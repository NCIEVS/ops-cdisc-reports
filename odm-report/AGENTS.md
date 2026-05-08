# odm-report Agent Context

## ODM Conversion Scope

This module converts formatted Excel terminology reports into ODM XML files.
The Lambda handler consumes `ReportEnum.MAIN_EXCEL` and adds `ReportEnum.ODM_XML`.

## Excel-to-ODM Flow

The active handler branches by file name: ICH files use `IchExcel2ODM`; all
other files use `TerminologyExcel2ODM`. Both write the ODM XML file next to the
source Excel file using the `.odm.xml` suffix.

## ICH vs CDISC Branching

Keep ICH-specific behavior isolated to ICH converter classes and schema/XSLT
compatibility paths. Do not spread filename checks beyond the module boundary
unless a shared pipeline decision is required.

## JAXB Usage

`OdmConvertorV2` and `TerminologyExcelReaderV2` are the newer typed conversion
path. They read workbook data into V2 models and marshal generated JAXB binding
types from `report/xml`.

Generated binding rules are documented in
`src/main/java/gov/nih/nci/evs/cdisc/report/xml/AGENTS.md`.

## Core Logic Files

- `report/aws/LambdaHandler.java`
- `report/TerminologyExcel2ODM.java`
- `report/IchExcel2ODM.java`
- `report/OdmConvertorV2.java`
- `report/TerminologyExcelReader.java`
- `report/TerminologyExcelReaderV2.java`
- `report/CustomCharacterEscapeHandler.java`
- `report/model/XMLData.java`
- `report/model/XmlDataV2.java`
- `report/model/Terminology.java`
- `report/utils/ExcelReader.java`
- `report/utils/XmlValidator.java`
- `src/test/java/gov/nih/nci/evs/cdisc/report/TerminologyExcel2ODMTest.java`
