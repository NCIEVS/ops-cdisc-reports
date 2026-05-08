# Thesaurus Parser Agent Context

## OWL Reader Scope

This package owns the V2 Thesaurus OWL reader used by newer text/excel and
pairing report logic. It builds an in-memory `Map<String, Concept>` from an OWL
file, keyed by concept code.

## Streaming XML Parser Pattern

`ThesaurusOwlReader` uses StAX events to stream through the OWL file. It tracks
top-level `owl:Class` depth to avoid ending a concept when nested class elements
appear inside subclass or equivalent-class structures.

## Concept/Axiom Model

The model package stores parsed concept state:

- `Concept`: code, label, hierarchy, subset, synonym, definition, and retired
  concept state.
- `Axiom`: intermediate parsed axiom data.
- `AlternativeDefinition`: non-primary definition data.

Synonyms reuse `common` model classes so downstream report modules receive the
same synonym shape.

## Handler Pattern

Handlers isolate XML element-specific parsing:

- `ConceptHandler`: coordinates concept element handling.
- `ClassElementHandler` implementations: parse subclass and equivalent-class
  relationships.
- `CharacterHandler` implementations: collect character data for recognized
  fields.
- `AxiomHandler`: converts completed axiom data into synonyms or alternative
  definitions.

When adding parser support, prefer a new handler or enum entry over broad
conditionals in `ThesaurusOwlReader`.

## Core Logic Files

- `owl/ThesaurusOwlReader.java`
- `owl/ConceptHandler.java`
- `owl/AxiomHandler.java`
- `owl/ClassElementEnum.java`
- `owl/CharacterHandlerFactory.java`
- `owl/SubClassOfHandler.java`
- `owl/EquivalentClassHandler.java`
- `model/Concept.java`
- `model/Axiom.java`
- `model/AlternativeDefinition.java`
- `utils/ThesaurusUtils.java`
