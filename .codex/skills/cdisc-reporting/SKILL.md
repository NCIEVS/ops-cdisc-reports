---
name: cdisc-reporting
description: "Use when working in `ops-cdisc-reports` to guide a user through generating, rerunning, troubleshooting, or documenting CDISC or ICH report runs locally or in AWS. Optimized for non-technical users: first verify prerequisites on the current machine, then walk through the AWS process one step at a time instead of dumping the full runbook."
---

# CDISC Reporting

Use this skill for tasks related to producing reports from this repository, especially when the user wants to:

- run or regenerate CDISC reports
- run or regenerate ICH reports
- prepare or adjust Step Function input JSON
- skip specific report steps for reruns
- follow or refresh the AWS operational runbook
- troubleshoot the build, Terraform deployment, DataSync, EFS, or Step Function flow
- be guided through the process as a non-technical operator

## First steps

1. Read `/Users/squareroot/wci/ncievs-CDISC/ops-cdisc-reports/README.md` for the repo-level architecture, build commands, and the base Step Function request shape.
2. Read [references/aws-runbook.md](references/aws-runbook.md) when the task involves AWS execution, operational steps, or manual reruns.
3. If the user needs the latest operational instructions, run [scripts/fetch_wiki_runbook.sh](scripts/fetch_wiki_runbook.sh) to pull the current Confluence page.

## Default interaction mode

When the user asks to create reports, do not respond with the whole workflow at once.

Default to a guided operator flow:

1. Check prerequisites on the current machine.
2. Summarize what is ready and what is missing in plain language.
3. Execute or explain exactly one next step.
4. After that step completes, report the result and present the next step only.

Treat this as a step-by-step walkthrough for a non-technical user unless the user explicitly asks for a full summary or all steps upfront.

## Prerequisite checks

Before starting the AWS workflow, check as many of these as are relevant:

- repo state:
  - `git status --short --branch`
- tenv:
  - `tenv --version`
- Terraform selection:
  - `tenv terraform list`
- Java:
  - `java -version`
- Terraform:
  - `terraform version`
- AWS CLI:
  - `aws --version`
- AWS config presence:
  - `ls -la ~/.aws ~/.gradle 2>/dev/null`
- Terraform inputs:
  - confirm `terraform/dev.tfvars` exists
- AWS access:
  - `aws sts get-caller-identity`

Prefer the documented repo versions when checking readiness:

- treat Java 11 as the expected Java version for this repo because `README.md` says "Written in Java 11"
- treat Terraform 1.11.2 as the expected Terraform version for CDISC report runs
- when `tenv` is installed, prefer `tenv terraform install 1.11.2` and `tenv terraform use 1.11.2` before any Terraform command
- after selecting the version with `tenv`, confirm with `terraform version`

When network-restricted commands fail in the sandbox, rerun them with escalation rather than stopping at the failure.

Do not force exact version matches if the installed versions are plausibly compatible. Report them as:

- ready
- present but different from the documented version
- missing
- blocked

If something is missing, stop and tell the user exactly what needs to be fixed before moving on.
If `tenv` is missing and Terraform work is required, stop and tell the user to install `tenv` before continuing.

## Guided execution order

Once prerequisites are acceptable, drive the workflow in this order:

1. Verify repo and environment readiness.
2. Refresh the code before any Gradle build:
   - confirm the repo is on `main`
   - if it is not on `main`, stop and ask the user before switching branches
   - run `git pull` on `main` before building
3. Ask whether to run the Gradle build with tests.
4. Present the build choices in this order:
   - default and first option: build without tests using `./gradlew clean buildZip -x test`
   - second option: build with tests using `./gradlew clean buildZip`
5. Build Lambda artifacts with Gradle using the user's choice.
6. Verify `tenv` is available and select Terraform 1.11.2 with it.
7. Verify Terraform inputs and current AWS identity.
8. Ask whether to destroy existing Terraform resources before the run.
9. Present the Terraform choices in this order:
   - default and first option: do not destroy first; continue directly to `terraform apply -var-file dev.tfvars`
   - second option: destroy first with `terraform apply -destroy -var-file dev.tfvars`, then run `terraform apply -var-file dev.tfvars`
10. Apply or refresh Terraform resources using the user's choice.
11. List `s3://wci-us-west-2/NCI/Thesaurus/`, choose the most recent `.owl` file, and ask the user to confirm it before proceeding.
12. Ask whether to use the default previous-run folder `s3://wci-us-west-2/NCI/Thesaurus/work/Previous`.
13. If the user provides local files instead of S3 paths, upload those files to the correct S3 locations.
14. Start the DataSync task and report progress until it completes or fails.
15. If DataSync succeeds, treat the file transfer to EFS as complete and proceed without a separate EFS verification step unless the user explicitly asks for one or the run shows evidence of a file-access problem.
16. Split the standard CDISC concept list into two Step Function runs to avoid Lambda timeouts. Keep the same pairing report concept subset in both runs unless the user asks otherwise.
17. Generate the exact Step Function input JSON for both runs.
18. Guide the user to start each Step Function run.
19. Explain what to check in Google Drive after completion.
20. If requested, guide cleanup.

Do not jump ahead. Finish the current step first.

## Step presentation style

Use language suited for a non-technical user:

- say what you are checking
- say what the result means
- say what happens next

For each step:

- keep the explanation short
- include the exact command only when useful
- avoid unexplained jargon
- explicitly say whether you already completed the step or the user must do something in the AWS UI

When a step requires a manual AWS Console action, give only the instructions for that one screen or task. Do not bundle later tasks into the same message.

For file-upload steps:

- list `s3://wci-us-west-2/NCI/Thesaurus/`, choose the newest `.owl` object, and ask the user to confirm it before proceeding
- ask whether to use the default previous-run folder `s3://wci-us-west-2/NCI/Thesaurus/work/Previous`
- only ask for local upload paths when the required files are not already staged in S3 or the user wants to replace them
- verify the path exists before attempting upload
- state the target S3 location before uploading

For DataSync:

- prefer starting the task directly with AWS CLI when possible
- after starting it, monitor status and keep the user updated
- report at least:
  - that the task started
  - the current execution status while waiting
  - whether it completed successfully or failed
- if DataSync completes successfully, assume the files were transferred and move on to Step Function input preparation unless the user explicitly asks for EFS verification or there is evidence of a mount or file-path problem
- if the AWS CLI route is blocked or unavailable, explain the exact DataSync task name and the single manual action the user must take

Before any potentially slow or potentially destructive step, ask explicitly:

- before the Gradle build, ask whether the user wants to run tests
- before Terraform destroy, ask whether the user wants to destroy existing resources first

Use these defaults and present them first:

- build without tests is the default build option
- do not destroy Terraform resources first is the default Terraform option

Do not assume opposite defaults if the user has not already said.

## Working rules

- Prefer repo-local facts from `README.md`, `terraform/`, and module code when answering implementation questions.
- Prefer the Confluence runbook for AWS operational steps such as Terraform apply/destroy, DataSync, EFS setup, Step Function execution, and cleanup.
- The verified Confluence host for the runbook is `https://wci-wiki.atlassian.net`, even though `WCI_ATLASSIAN_URL` may point somewhere else. Use the script in this skill rather than assuming the env URL is the correct host.
- Do not print or persist Atlassian secrets. The available env vars are expected to include `WCI_ATLASSIAN_EMAIL` and `WCI_ATLASSIAN_TOKEN`.
- Before building, refresh the code on `main`: check the branch, stop for user confirmation if the repo is not on `main`, and run `git pull` on `main`.
- Before building, ask whether to run tests and present `./gradlew clean buildZip -x test` as the default and first option.
- Before Terraform work, verify `tenv` is installed and switch to Terraform 1.11.2.
- Before Terraform apply, ask whether to destroy existing resources first and present skipping destroy as the default and first option.
- For the OWL file, list `s3://wci-us-west-2/NCI/Thesaurus/`, choose the most recent `.owl`, and ask the user to confirm it before DataSync or Step Function input preparation.
- For previous-run files, ask whether to use `s3://wci-us-west-2/NCI/Thesaurus/work/Previous` before proceeding.
- Before S3 upload, ask for local paths only when the files are not already available in S3 or the user wants to replace them.
- For S3 upload, prefer doing the upload directly with AWS CLI once the user provides local paths instead of only describing a manual console flow.
- For DataSync, prefer starting `s3-to-efs-ds-task` directly and then polling execution status to report progress back to the user.
- If `s3-to-efs-ds-task` finishes with `SUCCESS`, treat that as sufficient confirmation that the uploaded files reached EFS for normal operator flow.
- For standard CDISC runs, split the concept list into two Step Function executions because running the full list can cause Lambda timeouts.
- Keep the pairing report concept subset the same for both split CDISC runs unless the user asks for a different pairing strategy.
- If a Step Function execution fails or reports errors, inspect the execution history first, then check the relevant Lambda CloudWatch logs. If the cause is not obvious from logs, inspect the corresponding module code and summarize the likely problem and next action.
- When the user asks to generate a Step Function payload, start from the examples in the reference and then tailor:
  - `thesaurusOwlFile`
  - `publicationDate`
  - `conceptCodes`
  - `deliveryEmailAddresses`
  - `pairingReportRequest`
  - any `skip<ReportName>` flags
- When needed, ask only for the minimum missing input for the current step. Do not ask for inputs needed by later steps yet.
- When skipping `text-excel-reports`, include `reportDetails` in the root input because downstream jobs depend on that object.
- For manual file replacement workflows, remember the downstream dependency chain noted in the runbook:
  - `.txt` changes affect changes reports
  - `.xls` changes affect paired Excel outputs and downstream ODM/PDF/HTML generation

## Key repo paths

- `/Users/squareroot/wci/ncievs-CDISC/ops-cdisc-reports/README.md`
- `/Users/squareroot/wci/ncievs-CDISC/ops-cdisc-reports/terraform`
- `/Users/squareroot/wci/ncievs-CDISC/ops-cdisc-reports/docs/architecture.png`
- `/Users/squareroot/wci/ncievs-CDISC/ops-cdisc-reports/docs/stepfunctions_graph.png`
- `/Users/squareroot/wci/ncievs-CDISC/ops-cdisc-reports/post-process-reports`
- `/Users/squareroot/wci/ncievs-CDISC/ops-cdisc-reports/text-excel-reports`
- `/Users/squareroot/wci/ncievs-CDISC/ops-cdisc-reports/excel-formatting`
- `/Users/squareroot/wci/ncievs-CDISC/ops-cdisc-reports/odm-report`
- `/Users/squareroot/wci/ncievs-CDISC/ops-cdisc-reports/upload-reports`

## Output expectations

- For operational requests, behave like a guided checklist runner, not a static document.
- Start by reporting prerequisite status before recommending the build or AWS steps.
- Present one concrete next step at a time.
- For operational requests, give exact commands, paths, JSON, and any assumptions that still need confirmation.
- For rerun requests, be explicit about which steps are skipped and what prerequisite files must already exist in `/mnt/cdisc/work/current/...`.
- For current-process questions, state whether the answer came from the bundled reference or was refreshed from Confluence during the turn.
