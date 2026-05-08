# owl-reader Agent Context

## Standalone OWL Reader Scope

This subproject contains an older standalone OWL reader utility. It is separate
from the V2 parser in `text-excel-reports` and is not part of the primary report
pipeline.

## Default-package Caveat

The Java files in this module are in the default package. Avoid introducing
cross-module dependencies on these classes. If this utility needs production use,
move it behind a named package with tests instead of expanding default-package
usage.

## Parsing Utility Files

- `Concept.java`: standalone concept data object.
- `ConceptMarker.java`: marker/helper data object.
- `OwlReader.java`: standalone parser and `main` entry point.

## Test Status

`owl-reader/src/test` currently has no committed test files. Add tests before
changing parsing behavior intended for reuse.
