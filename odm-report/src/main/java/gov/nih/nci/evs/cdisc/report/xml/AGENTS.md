# ODM XML Binding Agent Context

## Generated JAXB Binding Scope

This package contains generated JAXB types for ODM and related XML signature
schemas. The classes map schema elements and attributes used by ODM conversion.

## Schema Source

Schema resources live under `odm-report/src/main/resources/schema`, including
controlled terminology, controlled terminology M11, foundation ODM, and core XML
signature schemas.

## Manual Edit Constraints

Treat these files as generated code. Avoid style cleanup, broad refactors, or
manual field/accessor changes. If a targeted fix is unavoidable, keep it small
and explain why regeneration was not used.

## Regeneration Notes

Regeneration should preserve the package
`gov.nih.nci.evs.cdisc.report.xml`, JAXB annotations, namespace behavior, and
the expectations of `OdmConvertorV2`.

## Core Generated Types

- `ODM.java`
- `ObjectFactory.java`
- `package-info.java`
- `ODMContext.java`
- `ODMcomplexTypeDefinitionStudy.java`
- `ODMcomplexTypeDefinitionMetaDataVersion.java`
- `ODMcomplexTypeDefinitionCodeList.java`
- `ODMcomplexTypeDefinitionEnumeratedItem.java`
- `FileType.java`
- `Granularity.java`
