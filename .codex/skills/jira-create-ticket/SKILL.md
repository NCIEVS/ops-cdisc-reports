---
name: jira-create-ticket
description: Create Jira tickets for the NCI-META Jira project/space using project key NM and Atlassian credentials from WCI_ATLASSIAN* environment variables. Use when a user asks Codex to create, draft, or submit a Jira issue/ticket/story for this project, including choosing or searching for an Epic, generating acceptance criteria and test cases, collecting 1/2/3 story points, and creating the issue through Jira.
---

# Jira Create Ticket

## Overview

Use this skill to create NCI-META Jira tickets through Atlassian environment credentials. The workflow is intentionally interactive: collect the Epic choice, ticket description, generated acceptance criteria, test cases, and story points before creating the live issue.

## Defaults

- Project/space display name: `NCI-META`
- Jira project key for issue creation: `NM`
- Board: `NM`
- Issue type: `Story`
- Credentials: read only environment variables whose names start with `WCI_ATLASSIAN`
- Helper script: `scripts/jira_ticket.py`

Jira boards are usually filter-based, not a direct create-issue field. Create the issue with project key `NM`; Jira shows this project/space as `NCI-META`. Use `NM` as the board constraint and let the helper verify the board when possible. If this Jira instance requires a board-routing field, set `WCI_ATLASSIAN_BOARD_FIELD` and optionally `WCI_ATLASSIAN_BOARD_VALUE` in the environment.

## Required Workflow

1. Confirm configuration without exposing secrets:

   ```bash
   python3 .codex/skills/jira-create-ticket/scripts/jira_ticket.py inspect-config
   ```

   Do not ask the user to paste Atlassian credentials. If required credentials are missing, report the missing environment variable families. If `project_key` reports `NCI-META`, that value is the display name and issue creation can fail with "valid project is required"; use `WCI_ATLASSIAN_PROJECT_KEY=NM` or update the environment.

2. Ask for the Epic before creating the ticket:

   Ask: "Which Epic should this ticket belong to? You can enter an Epic key, describe the Epic so I can search for it, or say none."

   - If the user gives an Epic key, verify it:

     ```bash
     python3 .codex/skills/jira-create-ticket/scripts/jira_ticket.py find-epics --key EPIC-123
     ```

   - If the user describes an Epic, search and suggest the best match:

     ```bash
     python3 .codex/skills/jira-create-ticket/scripts/jira_ticket.py find-epics --query "user's epic description"
     ```

     Present the best candidate with key, summary, and status. Ask the user to confirm it, provide a different Epic key, or continue with no Epic.

   - If the user chooses no Epic, continue without one.

3. Ask for the ticket description.

   From the description, draft:

   - A concise Jira summary/title
   - A short plain-language description paragraph
   - Named sections for grouped context such as affected files, source links, dependencies, or constraints
   - Acceptance criteria written as observable outcomes
   - Test cases with test name, steps, and expected result

   Keep the ticket concrete and implementation-neutral unless the user gave specific implementation requirements. Do not pack labeled lists such as "Known local assets to update" into the free-text description. Put grouped items in the payload's `sections` array so Jira renders them as separate headings with real bullet or numbered lists.

4. Ask for story points.

   Accept only `1`, `2`, or `3`.

   - `1`: straightforward, low uncertainty
   - `2`: moderate effort or moderate uncertainty
   - `3`: larger, harder, or higher uncertainty

5. Show a short final preview and ask for explicit confirmation before creating the live Jira ticket unless the user has already explicitly said to create it.

6. Create a temporary JSON payload and call the helper:

   ```bash
   python3 .codex/skills/jira-create-ticket/scripts/jira_ticket.py create --input /tmp/jira-ticket.json --check-board
   ```

   Example payload shape:

   ```json
   {
     "summary": "Add validation for report run inputs",
     "description": "Users need clearer validation before a report run starts.",
     "sections": [
       {
         "heading": "Affected Inputs",
         "items": [
           "Report type",
           "Source terminology file",
           "Output directory"
         ]
       },
       {
         "heading": "Reference Workflow",
         "ordered": true,
         "items": [
           "Open the report run form.",
           "Leave one required input blank.",
           "Submit the form."
         ]
       }
     ],
     "acceptance_criteria": [
       "Invalid required inputs are caught before the run starts.",
       "The error message identifies the field that needs correction."
     ],
     "test_cases": [
       {
         "name": "Missing required input",
         "steps": [
           "Start a report run without the required input.",
           "Submit the form."
         ],
         "expected": "The run does not start and the missing field is identified."
       }
     ],
     "story_points": 2,
     "epic_key": "EPIC-123"
   }
   ```

   Formatting guidance:

   - `description`: 1-3 short paragraphs only; no pseudo-Markdown lists.
   - `sections`: use for source URLs, affected files, known assets, scope notes, or ordered workflows.
   - `acceptance_criteria`: use concise bullet-worthy outcome statements.
   - `test_cases`: keep `steps` as an array; the helper renders steps as a numbered list under each test case.

7. Report the created Jira key and URL back to the user.

## Environment Variables

The helper only reads names beginning with `WCI_ATLASSIAN`.

Required:

- URL: `WCI_ATLASSIAN_BASE_URL`, `WCI_ATLASSIAN_SITE_URL`, `WCI_ATLASSIAN_URL`, or `WCI_ATLASSIAN_HOST`
- Token: `WCI_ATLASSIAN_API_TOKEN`, `WCI_ATLASSIAN_TOKEN`, or `WCI_ATLASSIAN_PAT`
- User email for Jira Cloud basic auth: `WCI_ATLASSIAN_EMAIL`, `WCI_ATLASSIAN_USER_EMAIL`, or `WCI_ATLASSIAN_USERNAME`

If no email is present, the helper treats the token as a bearer token.

Optional:

- `WCI_ATLASSIAN_PROJECT_KEY`: overrides `NM`
- `WCI_ATLASSIAN_BOARD_NAME`: overrides `NM`
- `WCI_ATLASSIAN_ISSUE_TYPE`: overrides `Story`
- `WCI_ATLASSIAN_STORY_POINTS_FIELD`: field id or exact field name for story points
- `WCI_ATLASSIAN_EPIC_FIELD`: `parent`, a field id, or an exact field name for Epic linkage
- `WCI_ATLASSIAN_BOARD_FIELD`: `labels`, `components`, a field id, or an exact field name for board routing
- `WCI_ATLASSIAN_BOARD_VALUE`: value to use with `WCI_ATLASSIAN_BOARD_FIELD`; defaults to `NM`
- `WCI_ATLASSIAN_BOARD_VALUE_JSON`: JSON value to use for complex custom fields

## Failure Handling

- If the first Jira call fails with DNS or host lookup errors from the sandbox, retry the same helper command with escalated network access.
- If Python fails TLS verification with `CERTIFICATE_VERIFY_FAILED`, the helper retries with the local `certifi` CA bundle when `certifi` is installed. If it still fails, retry the command with `SSL_CERT_FILE` set to certifi's bundle path:

   ```bash
   SSL_CERT_FILE=$(python3 -c "import certifi; print(certifi.where())") python3 .codex/skills/jira-create-ticket/scripts/jira_ticket.py create --input /tmp/jira-ticket.json --check-board
   ```

- If Jira rejects the issue with `project: valid project is required`, do not use the display name `NCI-META` as the project key. Retry with `WCI_ATLASSIAN_PROJECT_KEY=NM` and keep the board name as `NM`.
- If story points cannot be mapped to a Jira field, stop and ask the user or environment owner to set `WCI_ATLASSIAN_STORY_POINTS_FIELD`.
- If Epic linkage fails, retry only after identifying the correct Epic field; commonly this is `parent` or an `Epic Link` custom field.
- If board verification fails because the Agile API is unavailable, explain that Jira board membership is controlled by the board filter and continue only if the project/field routing is otherwise configured.
- Never store credentials, tokens, or API responses containing secrets in the skill files or ticket payload.
