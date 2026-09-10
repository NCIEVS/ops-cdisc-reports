#!/usr/bin/env python3
"""Create and search NCI-META Jira tickets using WCI_ATLASSIAN* env vars."""

from __future__ import annotations

import argparse
import base64
import json
import os
import re
import ssl
import sys
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from typing import Any


PROJECT_DEFAULT = "NM"
BOARD_DEFAULT = "NM"
ISSUE_TYPE_DEFAULT = "Story"
ENV_PREFIX = "WCI_ATLASSIAN"

STOPWORDS = {
    "about",
    "after",
    "again",
    "all",
    "also",
    "and",
    "any",
    "are",
    "because",
    "before",
    "can",
    "for",
    "from",
    "has",
    "have",
    "into",
    "its",
    "more",
    "need",
    "needs",
    "not",
    "our",
    "should",
    "that",
    "the",
    "their",
    "this",
    "ticket",
    "with",
    "work",
    "will",
    "would",
}


class JiraError(RuntimeError):
    def __init__(self, message: str, status: int | None = None):
        super().__init__(message)
        self.status = status


@dataclass(frozen=True)
class Config:
    base_url: str | None
    email: str | None
    token: str | None
    project_key: str
    board_name: str
    issue_type: str
    story_points_field: str | None
    epic_field: str | None
    board_field: str | None
    board_value: Any

    @property
    def has_auth(self) -> bool:
        return bool(self.base_url and self.token)

    @property
    def missing(self) -> list[str]:
        missing = []
        if not self.base_url:
            missing.append(
                "WCI_ATLASSIAN_BASE_URL, WCI_ATLASSIAN_SITE_URL, "
                "WCI_ATLASSIAN_URL, or WCI_ATLASSIAN_HOST"
            )
        if not self.token:
            missing.append(
                "WCI_ATLASSIAN_API_TOKEN, WCI_ATLASSIAN_TOKEN, or "
                "WCI_ATLASSIAN_PAT"
            )
        return missing

    def summary(self) -> dict[str, Any]:
        auth_mode = (
            "basic"
            if self.email and self.token
            else "bearer"
            if self.token
            else "missing"
        )
        return {
            "base_url": self.base_url,
            "auth_mode": auth_mode,
            "email_present": bool(self.email),
            "token_present": bool(self.token),
            "project_key": self.project_key,
            "board_name": self.board_name,
            "issue_type": self.issue_type,
            "story_points_field": self.story_points_field or "auto",
            "epic_field": self.epic_field or "parent",
            "board_field": self.board_field or "filter-based/not set",
            "missing": self.missing,
        }


def env_first(*suffixes: str) -> str | None:
    for suffix in suffixes:
        value = os.environ.get(f"{ENV_PREFIX}_{suffix}")
        if value:
            return value.strip()
    return None


def read_config() -> Config:
    base_url = env_first("BASE_URL", "SITE_URL", "URL", "HOST")
    if base_url:
        base_url = base_url.rstrip("/")
        if not re.match(r"^https?://", base_url):
            base_url = f"https://{base_url}"

    board_name = env_first("BOARD_NAME") or BOARD_DEFAULT
    board_value: Any = env_first("BOARD_VALUE") or board_name
    board_value_json = env_first("BOARD_VALUE_JSON")
    if board_value_json:
        try:
            board_value = json.loads(board_value_json)
        except json.JSONDecodeError as exc:
            raise JiraError(f"WCI_ATLASSIAN_BOARD_VALUE_JSON is invalid JSON: {exc}") from exc

    return Config(
        base_url=base_url,
        email=env_first("EMAIL", "USER_EMAIL", "USERNAME", "USER"),
        token=env_first("API_TOKEN", "TOKEN", "PAT"),
        project_key=env_first("PROJECT_KEY", "PROJECT") or PROJECT_DEFAULT,
        board_name=board_name,
        issue_type=env_first("ISSUE_TYPE") or ISSUE_TYPE_DEFAULT,
        story_points_field=env_first("STORY_POINTS_FIELD"),
        epic_field=env_first("EPIC_FIELD"),
        board_field=env_first("BOARD_FIELD"),
        board_value=board_value,
    )


def is_ssl_verification_error(exc: urllib.error.URLError) -> bool:
    reason = getattr(exc, "reason", None)
    return isinstance(reason, ssl.SSLCertVerificationError) or "CERTIFICATE_VERIFY_FAILED" in str(reason)


def certifi_ssl_context() -> ssl.SSLContext | None:
    try:
        import certifi  # type: ignore[import-not-found]
    except ImportError:
        return None
    return ssl.create_default_context(cafile=certifi.where())


class JiraClient:
    def __init__(self, config: Config):
        if not config.has_auth:
            raise JiraError("Missing Jira configuration: " + "; ".join(config.missing))
        self.config = config
        assert config.base_url is not None
        self.base_url = config.base_url

    def request(
        self,
        method: str,
        path: str,
        *,
        query: dict[str, Any] | None = None,
        payload: dict[str, Any] | None = None,
        ok: tuple[int, ...] = (200,),
    ) -> Any:
        url = self.base_url + path
        if query:
            clean_query = {k: v for k, v in query.items() if v is not None}
            url = f"{url}?{urllib.parse.urlencode(clean_query, doseq=True)}"

        data = None
        headers = {
            "Accept": "application/json",
            "User-Agent": "codex-jira-create-ticket/1.0",
        }
        if payload is not None:
            data = json.dumps(payload).encode("utf-8")
            headers["Content-Type"] = "application/json"

        token = self.config.token or ""
        if self.config.email:
            raw = f"{self.config.email}:{token}".encode("utf-8")
            headers["Authorization"] = "Basic " + base64.b64encode(raw).decode("ascii")
        else:
            headers["Authorization"] = f"Bearer {token}"

        request = urllib.request.Request(url, data=data, headers=headers, method=method)
        try:
            with self._open(request) as response:
                body = response.read().decode("utf-8")
                if response.status not in ok:
                    raise JiraError(f"{method} {path} returned HTTP {response.status}", response.status)
                if not body:
                    return {}
                return json.loads(body)
        except urllib.error.HTTPError as exc:
            body = exc.read().decode("utf-8", errors="replace")
            raise JiraError(
                f"{method} {path} failed with HTTP {exc.code}: {summarize_error_body(body)}",
                exc.code,
            ) from exc
        except urllib.error.URLError as exc:
            raise JiraError(f"{method} {path} failed: {exc.reason}") from exc

    def _open(self, request: urllib.request.Request):
        try:
            return urllib.request.urlopen(request, timeout=30)
        except urllib.error.URLError as exc:
            if not is_ssl_verification_error(exc):
                raise
            context = certifi_ssl_context()
            if context is None:
                raise
            return urllib.request.urlopen(request, timeout=30, context=context)


def summarize_error_body(body: str) -> str:
    if not body:
        return "<empty response>"
    try:
        parsed = json.loads(body)
    except json.JSONDecodeError:
        return body[:800]
    parts: list[str] = []
    if parsed.get("errorMessages"):
        parts.extend(str(item) for item in parsed["errorMessages"])
    if parsed.get("errors"):
        parts.extend(f"{key}: {value}" for key, value in parsed["errors"].items())
    return "; ".join(parts) if parts else json.dumps(parsed)[:800]


def require_config(config: Config) -> None:
    if config.missing:
        raise JiraError("Missing Jira configuration: " + "; ".join(config.missing))


def jql_string(value: str) -> str:
    return '"' + value.replace("\\", "\\\\").replace('"', '\\"') + '"'


def get_issue(client: JiraClient, key: str, fields: list[str] | None = None) -> dict[str, Any]:
    issue_key = urllib.parse.quote(key.strip(), safe="")
    return client.request(
        "GET",
        f"/rest/api/3/issue/{issue_key}",
        query={"fields": ",".join(fields or ["summary", "issuetype", "status"])},
    )


def search_issues(client: JiraClient, jql: str, max_results: int, fields: list[str]) -> dict[str, Any]:
    query = {
        "jql": jql,
        "maxResults": max_results,
        "fields": ",".join(fields),
    }
    try:
        return client.request("GET", "/rest/api/3/search/jql", query=query)
    except JiraError as exc:
        if exc.status not in (400, 404):
            raise
        return client.request(
            "POST",
            "/rest/api/3/search",
            payload={"jql": jql, "maxResults": max_results, "fields": fields},
        )


def words_for_search(text: str, limit: int = 6) -> list[str]:
    found = []
    seen = set()
    for word in re.findall(r"[A-Za-z0-9]{3,}", text.lower()):
        if word in STOPWORDS or word in seen:
            continue
        seen.add(word)
        found.append(word)
        if len(found) >= limit:
            break
    return found


def adf_to_text(value: Any) -> str:
    parts: list[str] = []

    def walk(node: Any) -> None:
        if isinstance(node, dict):
            text = node.get("text")
            if isinstance(text, str):
                parts.append(text)
            for child in node.get("content", []):
                walk(child)
        elif isinstance(node, list):
            for child in node:
                walk(child)

    walk(value)
    return " ".join(parts)


def issue_row(issue: dict[str, Any], terms: list[str] | None = None) -> dict[str, Any]:
    fields = issue.get("fields", {})
    summary = fields.get("summary") or ""
    status = (fields.get("status") or {}).get("name") or ""
    issue_type = (fields.get("issuetype") or {}).get("name") or ""
    description = adf_to_text(fields.get("description"))
    score = 0
    for term in terms or []:
        score += 3 * summary.lower().count(term)
        score += description.lower().count(term)
    return {
        "key": issue.get("key"),
        "summary": summary,
        "status": status,
        "issue_type": issue_type,
        "score": score,
    }


def find_epic_by_key(client: JiraClient, key: str) -> list[dict[str, Any]]:
    issue = get_issue(client, key, ["summary", "issuetype", "status"])
    row = issue_row(issue)
    if row["issue_type"].lower() != "epic":
        raise JiraError(f"{key} is a {row['issue_type']}, not an Epic.")
    return [row]


def find_epics_by_query(
    client: JiraClient,
    project_key: str,
    query_text: str,
    limit: int,
) -> list[dict[str, Any]]:
    terms = words_for_search(query_text)
    base = f"project = {jql_string(project_key)} AND issuetype = Epic"
    if terms:
        clauses = []
        for term in terms:
            clauses.append(f"summary ~ {jql_string(term + '*')}")
            clauses.append(f"description ~ {jql_string(term + '*')}")
        jql = f"{base} AND ({' OR '.join(clauses)}) ORDER BY updated DESC"
    else:
        jql = f"{base} ORDER BY updated DESC"

    try:
        result = search_issues(
            client,
            jql,
            max(limit * 3, 10),
            ["summary", "description", "issuetype", "status"],
        )
    except JiraError as exc:
        if exc.status != 400 or not terms:
            raise
        fallback_jql = f"{base} ORDER BY updated DESC"
        result = search_issues(
            client,
            fallback_jql,
            max(limit * 3, 10),
            ["summary", "description", "issuetype", "status"],
        )
    rows = [issue_row(issue, terms) for issue in result.get("issues", [])]
    rows.sort(key=lambda row: (row["score"], row["key"] or ""), reverse=True)
    return rows[:limit]


def get_fields(client: JiraClient) -> list[dict[str, Any]]:
    return client.request("GET", "/rest/api/3/field")


def resolve_field_id(client: JiraClient, requested: str, *, purpose: str) -> str:
    requested = requested.strip()
    if requested in {"parent", "labels", "components"} or requested.startswith("customfield_"):
        return requested

    requested_folded = requested.casefold()
    for field in get_fields(client):
        if (field.get("name") or "").casefold() == requested_folded:
            field_id = field.get("id")
            if field_id:
                return field_id
    raise JiraError(f"Could not find Jira field named {requested!r} for {purpose}.")


def resolve_story_points_field(client: JiraClient, config: Config) -> str:
    if config.story_points_field:
        return resolve_field_id(client, config.story_points_field, purpose="story points")

    preferred = {
        "story point estimate": 0,
        "story points": 1,
        "story point estimates": 2,
    }
    candidates: list[tuple[int, str, str]] = []
    for field in get_fields(client):
        name = field.get("name") or ""
        name_folded = name.casefold()
        if name_folded in preferred or "story point" in name_folded:
            field_id = field.get("id")
            if field_id:
                candidates.append((preferred.get(name_folded, 99), name, field_id))

    if not candidates:
        raise JiraError(
            "Could not auto-detect the story points field. Set "
            "WCI_ATLASSIAN_STORY_POINTS_FIELD to the Jira field id or exact field name."
        )
    candidates.sort(key=lambda item: item[0])
    return candidates[0][2]


def add_board_field(client: JiraClient, fields: dict[str, Any], config: Config) -> None:
    if not config.board_field:
        return
    field_id = resolve_field_id(client, config.board_field, purpose="board routing")
    if field_id == "labels":
        label = re.sub(r"[^A-Za-z0-9_.-]+", "-", str(config.board_value)).strip("-")
        if label:
            fields.setdefault("labels", [])
            if label not in fields["labels"]:
                fields["labels"].append(label)
    elif field_id == "components":
        fields["components"] = [{"name": str(config.board_value)}]
    else:
        fields[field_id] = config.board_value


def validate_story_points(value: Any) -> int:
    try:
        points = int(value)
    except (TypeError, ValueError) as exc:
        raise JiraError("story_points must be 1, 2, or 3.") from exc
    if points not in {1, 2, 3}:
        raise JiraError("story_points must be 1, 2, or 3.")
    return points


def project_reference(project_key_or_id: str) -> dict[str, str]:
    if project_key_or_id.isdigit():
        return {"id": project_key_or_id}
    return {"key": project_key_or_id}


def infer_summary(description: str) -> str:
    first_line = next((line.strip() for line in description.splitlines() if line.strip()), "")
    if not first_line:
        return "New Jira ticket"
    first_sentence = re.split(r"(?<=[.!?])\s+", first_line, maxsplit=1)[0]
    return first_sentence[:120].rstrip(" .")


def text_node(text: str, *, strong: bool = False) -> dict[str, Any]:
    node: dict[str, Any] = {"type": "text", "text": text}
    if strong:
        node["marks"] = [{"type": "strong"}]
    return node


def paragraph(text: str = "") -> dict[str, Any]:
    if not text:
        return {"type": "paragraph"}
    return {"type": "paragraph", "content": [text_node(text)]}


def rich_paragraph(content: list[dict[str, Any]]) -> dict[str, Any]:
    if not content:
        return paragraph()
    return {"type": "paragraph", "content": content}


def labeled_paragraph(label: str, value: str) -> dict[str, Any]:
    content = [text_node(f"{label}: ", strong=True)]
    if value:
        content.append(text_node(value))
    return rich_paragraph(content)


def heading(text: str, level: int = 2) -> dict[str, Any]:
    return {
        "type": "heading",
        "attrs": {"level": level},
        "content": [text_node(text)],
    }


def text_list(value: Any) -> list[str]:
    if value is None:
        return []
    if isinstance(value, str):
        values = [value]
    elif isinstance(value, list | tuple):
        values = value
    else:
        values = [value]
    return [str(item).strip() for item in values if str(item).strip()]


def bullet_list(items: list[str]) -> dict[str, Any]:
    return {
        "type": "bulletList",
        "content": [
            {"type": "listItem", "content": [paragraph(item)]}
            for item in items
            if item.strip()
        ],
    }


def ordered_list(items: list[str]) -> dict[str, Any]:
    return {
        "type": "orderedList",
        "attrs": {"order": 1},
        "content": [
            {"type": "listItem", "content": [paragraph(item)]}
            for item in items
            if item.strip()
        ],
    }


def section_blocks(section: Any) -> list[dict[str, Any]]:
    if isinstance(section, str):
        return [paragraph(section.strip())] if section.strip() else []
    if not isinstance(section, dict):
        text = str(section).strip()
        return [paragraph(text)] if text else []

    blocks: list[dict[str, Any]] = []
    title = str(section.get("heading") or section.get("title") or "").strip()
    body = str(section.get("body") or section.get("description") or "").strip()
    items = text_list(section.get("items"))

    if title:
        blocks.append(heading(title))
    for block in re.split(r"\n\s*\n", body):
        if block.strip():
            blocks.append(paragraph(" ".join(line.strip() for line in block.splitlines())))
    if items:
        blocks.append(ordered_list(items) if section.get("ordered") else bullet_list(items))
    return blocks


def test_case_list_item(test_case: Any) -> dict[str, Any] | None:
    if isinstance(test_case, str):
        text = test_case.strip()
        if not text:
            return None
        return {"type": "listItem", "content": [paragraph(text)]}
    if not isinstance(test_case, dict):
        text = str(test_case).strip()
        if not text:
            return None
        return {"type": "listItem", "content": [paragraph(text)]}

    name = str(test_case.get("name") or "Test case").strip()
    steps = text_list(test_case.get("steps"))
    expected = str(test_case.get("expected") or "").strip()

    content: list[dict[str, Any]] = [rich_paragraph([text_node(name, strong=True)])]
    if steps:
        content.append(rich_paragraph([text_node("Steps", strong=True)]))
        content.append(ordered_list(steps))
    if expected:
        content.append(labeled_paragraph("Expected", expected))
    return {"type": "listItem", "content": content}


def test_cases_list(test_cases: Any) -> dict[str, Any] | None:
    raw_cases = test_cases if isinstance(test_cases, list | tuple) else [test_cases]
    items = [item for item in (test_case_list_item(test_case) for test_case in raw_cases) if item]
    if not items:
        return None
    return {"type": "bulletList", "content": items}


def build_description_adf(ticket: dict[str, Any]) -> dict[str, Any]:
    content: list[dict[str, Any]] = []

    description = str(ticket.get("description") or "").strip()
    content.append(heading("Description"))
    for block in re.split(r"\n\s*\n", description):
        if block.strip():
            content.append(paragraph(" ".join(line.strip() for line in block.splitlines())))

    sections = ticket.get("sections") or []
    if not isinstance(sections, list | tuple):
        sections = [sections]
    for section in sections:
        content.extend(section_blocks(section))

    acceptance = text_list(ticket.get("acceptance_criteria"))
    if acceptance:
        content.append(heading("Acceptance Criteria"))
        content.append(bullet_list(acceptance))

    test_cases = test_cases_list(ticket.get("test_cases") or [])
    if test_cases:
        content.append(heading("Test Cases"))
        content.append(test_cases)

    return {"type": "doc", "version": 1, "content": content}


def load_ticket_payload(path: str) -> dict[str, Any]:
    if path == "-":
        raw = sys.stdin.read()
    else:
        with open(path, "r", encoding="utf-8") as handle:
            raw = handle.read()
    try:
        payload = json.loads(raw)
    except json.JSONDecodeError as exc:
        raise JiraError(f"Ticket input is invalid JSON: {exc}") from exc
    if not isinstance(payload, dict):
        raise JiraError("Ticket input must be a JSON object.")
    return payload


def verify_board(client: JiraClient, config: Config, *, strict: bool) -> list[str]:
    warnings: list[str] = []
    try:
        result = client.request(
            "GET",
            "/rest/agile/1.0/board",
            query={
                "projectKeyOrId": config.project_key,
                "name": config.board_name,
                "maxResults": 10,
            },
        )
    except JiraError as exc:
        message = (
            f"Could not verify board {config.board_name!r}: {exc}. "
            "Jira board membership is filter-based."
        )
        if strict:
            raise JiraError(message) from exc
        warnings.append(message)
        return warnings

    boards = result.get("values", [])
    if not boards:
        message = (
            f"No Jira board named {config.board_name!r} was found for project "
            f"{config.project_key!r}."
        )
        if strict:
            raise JiraError(message)
        warnings.append(message)
    return warnings


def build_create_payload(
    client: JiraClient,
    config: Config,
    ticket: dict[str, Any],
    *,
    skip_epic_validation: bool,
) -> dict[str, Any]:
    description = str(ticket.get("description") or "").strip()
    if not description:
        raise JiraError("Ticket description is required.")

    summary = str(ticket.get("summary") or "").strip() or infer_summary(description)
    story_points = validate_story_points(ticket.get("story_points"))
    story_points_field = resolve_story_points_field(client, config)

    fields: dict[str, Any] = {
        "project": project_reference(config.project_key),
        "issuetype": {"name": config.issue_type},
        "summary": summary,
        "description": build_description_adf(ticket),
        story_points_field: story_points,
    }

    epic_key = str(ticket.get("epic_key") or "").strip()
    if epic_key:
        if not skip_epic_validation:
            find_epic_by_key(client, epic_key)
        epic_field = config.epic_field or "parent"
        field_id = resolve_field_id(client, epic_field, purpose="Epic linkage")
        if field_id == "parent":
            fields["parent"] = {"key": epic_key}
        else:
            fields[field_id] = epic_key

    add_board_field(client, fields, config)
    return {"fields": fields}


def create_issue(
    client: JiraClient,
    config: Config,
    ticket: dict[str, Any],
    *,
    dry_run: bool,
    check_board: bool,
    strict_board: bool,
    skip_epic_validation: bool,
) -> dict[str, Any]:
    warnings = verify_board(client, config, strict=strict_board) if check_board else []
    payload = build_create_payload(
        client,
        config,
        ticket,
        skip_epic_validation=skip_epic_validation,
    )

    if dry_run:
        return {"dry_run": True, "warnings": warnings, "payload": payload}

    result = client.request("POST", "/rest/api/3/issue", payload=payload, ok=(201,))
    key = result.get("key")
    output = {
        "key": key,
        "id": result.get("id"),
        "self": result.get("self"),
        "url": f"{config.base_url}/browse/{key}" if key else None,
        "warnings": warnings,
    }
    return output


def print_rows(rows: list[dict[str, Any]]) -> None:
    if not rows:
        print("No matching Epics found.")
        return
    for row in rows:
        print(f"{row['key']}\t{row['status']}\t{row['summary']}")


def command_inspect_config(args: argparse.Namespace) -> int:
    config = read_config()
    if args.json:
        print(json.dumps(config.summary(), indent=2))
        if config.missing:
            return 1
    else:
        summary = config.summary()
        for key, value in summary.items():
            if key != "missing":
                print(f"{key}: {value}")
        if config.missing:
            print("missing:")
            for item in config.missing:
                print(f"- {item}")
            return 1
    return 0


def command_find_epics(args: argparse.Namespace) -> int:
    config = read_config()
    require_config(config)
    client = JiraClient(config)
    if args.key:
        rows = find_epic_by_key(client, args.key)
    else:
        rows = find_epics_by_query(client, config.project_key, args.query or "", args.limit)

    if args.json:
        print(json.dumps(rows, indent=2))
    else:
        print_rows(rows)
    return 0


def command_create(args: argparse.Namespace) -> int:
    config = read_config()
    require_config(config)
    client = JiraClient(config)
    ticket = load_ticket_payload(args.input)
    output = create_issue(
        client,
        config,
        ticket,
        dry_run=args.dry_run,
        check_board=args.check_board,
        strict_board=args.strict_board,
        skip_epic_validation=args.skip_epic_validation,
    )
    print(json.dumps(output, indent=2))
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)

    inspect_parser = subparsers.add_parser("inspect-config", help="Show sanitized config.")
    inspect_parser.add_argument("--json", action="store_true", help="Emit JSON.")
    inspect_parser.set_defaults(func=command_inspect_config)

    find_parser = subparsers.add_parser("find-epics", help="Find or validate Epics.")
    group = find_parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--key", help="Exact Epic key to validate.")
    group.add_argument("--query", help="Epic description to search for.")
    find_parser.add_argument("--limit", type=int, default=5, help="Maximum rows to return.")
    find_parser.add_argument("--json", action="store_true", help="Emit JSON.")
    find_parser.set_defaults(func=command_find_epics)

    create_parser = subparsers.add_parser("create", help="Create a Jira issue from JSON.")
    create_parser.add_argument("--input", required=True, help="Ticket JSON file, or '-' for stdin.")
    create_parser.add_argument("--dry-run", action="store_true", help="Build payload without creating.")
    create_parser.add_argument("--check-board", action="store_true", help="Try to verify the Jira board.")
    create_parser.add_argument(
        "--strict-board",
        action="store_true",
        help="Fail if board verification fails or finds no matching board.",
    )
    create_parser.add_argument(
        "--skip-epic-validation",
        action="store_true",
        help="Do not fetch the Epic before creating the ticket.",
    )
    create_parser.set_defaults(func=command_create)

    return parser


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()
    try:
        return args.func(args)
    except JiraError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
