# Root src Agent Context

## Root App Scope

The root `src` tree is separate from the CDISC report Step Function pipeline.
It contains a standalone Google Drive quickstart/client app wired by the root
`build.gradle`, not the shared report module Google Drive client.

## Google Drive Quickstart Context

Keep this code isolated from `common/src/main/java/gov/nih/nci/evs/cdisc/gcp`.
The root app uses `src/main/resources/credentials.json` and should not become a
dependency source for report modules.

## Core Logic Files

- `src/main/java/gov/nih/nci/evs/gdrive/GoogleDriveClient.java`: standalone
  Google Drive client and local `main` entry point.
- `src/main/resources/credentials.json`: local credentials resource used by the
  root app.
