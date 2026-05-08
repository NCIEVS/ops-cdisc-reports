# OWL/RDF XML Binding Agent Context

## Generated RDF JAXB Binding Scope

This package contains generated JAXB types used by `RDFGeneratorV2` to marshal
RDF/OWL terminology output.

## Schema Source

Schema resources live under `owl-report/src/main/resources/schema`, including
`rdf.xsd`, `ontology.xsd`, `permissible_value.xsd`, `cdisc.xsd`, and
`misc.xsd`.

## Manual Edit Constraints

Treat these files as generated code. Avoid manual formatting churn and broad
refactors. If a targeted fix is required, keep it minimal and document the
reason.

## Namespace Handling Notes

`RDFGeneratorV2` uses a templated namespace value and replaces `{terminology}`
after marshalling. Preserve that behavior when changing package annotations or
generated namespace mappings.

## Core Generated Types

- `RDF.java`
- `ObjectFactory.java`
- `package-info.java`
- `Ontology.java`
- `PermissibleValue.java`
- `CdiscDefinition.java`
- `CdiscSubmissionValue.java`
- `CdiscSynonyms.java`
- `CodelistName.java`
- `NciCode.java`
- `NciPreferredTerm.java`
