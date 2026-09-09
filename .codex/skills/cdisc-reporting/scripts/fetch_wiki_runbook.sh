#!/usr/bin/env bash
set -euo pipefail

page_id="${1:-1642004534}"
base_url="https://wci-wiki.atlassian.net"

: "${WCI_ATLASSIAN_EMAIL:?WCI_ATLASSIAN_EMAIL must be set}"
: "${WCI_ATLASSIAN_TOKEN:?WCI_ATLASSIAN_TOKEN must be set}"

curl -sS -u "${WCI_ATLASSIAN_EMAIL}:${WCI_ATLASSIAN_TOKEN}" \
  "${base_url}/wiki/rest/api/content/${page_id}?expand=body.storage,title" |
  jq -r '
    .title,
    "",
    .body.storage.value
  ' |
  perl -0pe '
    s{<ac:structured-macro[^>]*ac:name="code"[^>]*>.*?<ac:plain-text-body><!\[CDATA\[(.*?)\]\]></ac:plain-text-body>.*?</ac:structured-macro>}{\n```\n$1\n```\n}gms;
    s{<br\s*/?>}{\n}gms;
    s{</(p|h1|h2|h3|li|ul|blockquote)>}{\n}gms;
    s{<[^>]+>}{}gms;
    s/&nbsp;/ /g;
    s/&amp;/&/g;
    s/&lt;/</g;
    s/&gt;/>/g;
    s/&quot;/"/g;
    s/&#39;/'\''/g;
    s/&rsquo;/'\''/g;
    s/&ldquo;/"/g;
    s/&rdquo;/"/g;
    s/\n{3,}/\n\n/g;
  '
