#!/usr/bin/env bash
set -Eeuo pipefail
IFS=$'\n\t'

# Automates the Confluence runbook "Running CDISC Reports in AWS":
# https://wci-wiki.atlassian.net/wiki/spaces/MSC/pages/1642004534/Running+CDISC+Reports+in+AWS

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
START_DIR="$(pwd)"

TFVARS_FILE="${REPO_ROOT}/terraform/dev.tfvars"
OWL_FILE=""
PREVIOUS_TEXT_PATH=""
PUBLICATION_DATE=""
REPORT_TYPE="cdisc"
DELIVERY_EMAILS="akuppusamy@westcoastinformatics.com,jliu@westcoastinformatics.com,dshapiro@westcoastinformatics.com"
CONCEPT_CODES=""
PAIRING_CONCEPT_CODES="C81222,C66830,C77526"
AWS_PROFILE=""
AWS_REGION=""
THESAURUS_BUCKET=""
S3_PREFIX="NCI/Thesaurus"
DATASYNC_TASK_NAME="s3-to-efs-ds-task"
STATE_MACHINE_NAME="cdisc-report-state-machine"
DESTROY_FIRST=1
VERIFY_EFS=1
YES=0
POLL_SECONDS=15
PAYLOAD_FILE=""
GENERATED_PAYLOAD_FILE=""

usage() {
  cat <<'USAGE'
Usage:
  bin/run-cdisc-reports-aws.sh \
    --owl-file /path/to/Thesaurus.owl \
    --previous-text /path/to/previous-text-files \
    --publication-date YYYY-MM-DD

Required unless --payload-file supplies the Step Function input:
  --publication-date YYYY-MM-DD       Publication date used in generated Step Function JSON.

Always required:
  --owl-file PATH                     Local Thesaurus OWL file to upload to S3.
  --previous-text PATH                Local prior-quarter .txt file, or directory of .txt files.

Options:
  --report-type cdisc|ich             Generated payload type. Default: cdisc.
  --payload-file PATH                 Use an existing Step Function JSON input instead of generating one.
  --delivery-emails CSV               Override generated payload delivery email list.
  --concept-codes CSV                 Override generated payload concept code list.
  --pairing-concept-codes CSV         Override generated CDISC pairing report concept code list.
  --tfvars PATH                       Terraform var file. Default: terraform/dev.tfvars.
  --aws-profile PROFILE               AWS profile. Default: aws_profile from tfvars.
  --aws-region REGION                 AWS region. Default: aws_region from tfvars, then us-west-2.
  --skip-destroy                      Do not run terraform apply -destroy before terraform apply.
  --skip-efs-verify                   Do not invoke list-efs-3 after DataSync.
  --poll-seconds SECONDS              Poll interval for DataSync and Step Functions. Default: 15.
  -y, --yes                           Do not prompt; pass -auto-approve to Terraform.
  -h, --help                          Show this help.

The default follows the wiki and destroys existing Terraform-managed AWS resources
before recreating them. Use --skip-destroy to keep existing resources.
USAGE
}

log() {
  printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"
}

warn() {
  printf '[%s] WARNING: %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*" >&2
}

die() {
  printf '[%s] ERROR: %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*" >&2
  exit 1
}

cleanup() {
  if [[ -n "${GENERATED_PAYLOAD_FILE}" && -f "${GENERATED_PAYLOAD_FILE}" ]]; then
    rm -f "${GENERATED_PAYLOAD_FILE}"
  fi
}
trap cleanup EXIT

need_value() {
  if [[ $# -lt 2 || "${2:-}" == --* || "${2:-}" == "" ]]; then
    die "$1 requires a value"
  fi
}

abs_path() {
  case "$1" in
    /*) printf '%s\n' "$1" ;;
    *) printf '%s/%s\n' "$START_DIR" "$1" ;;
  esac
}

trim() {
  local value="$1"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  printf '%s' "$value"
}

json_escape() {
  local value="$1"
  value="${value//\\/\\\\}"
  value="${value//\"/\\\"}"
  value="${value//$'\n'/\\n}"
  value="${value//$'\r'/\\r}"
  value="${value//$'\t'/\\t}"
  printf '%s' "$value"
}

json_array_from_csv() {
  local csv="$1"
  local old_ifs="$IFS"
  local first=1
  local item
  local items
  IFS=','
  read -r -a items <<< "$csv"
  IFS="$old_ifs"

  printf '['
  for item in "${items[@]}"; do
    item="$(trim "$item")"
    [[ -z "$item" ]] && continue
    if [[ "$first" -eq 0 ]]; then
      printf ', '
    fi
    printf '"%s"' "$(json_escape "$item")"
    first=0
  done
  printf ']'
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || die "Required command '$1' is not installed or is not on PATH"
}

run() {
  log "+ $*"
  "$@"
}

confirm() {
  local prompt="$1"
  local reply

  if [[ "$YES" -eq 1 ]]; then
    return 0
  fi

  printf '%s [y/N] ' "$prompt" >&2
  if ! read -r reply; then
    die "No confirmation was provided. Re-run with --yes for non-interactive execution."
  fi

  case "$reply" in
    y|Y|yes|YES) return 0 ;;
    *) die "Cancelled" ;;
  esac
}

read_tfvar_string() {
  local key="$1"
  awk -v key="$key" '
    $0 ~ "^[[:space:]]*" key "[[:space:]]*=" {
      sub(/^[^=]*=[[:space:]]*/, "", $0)
      sub(/[[:space:]]*#.*/, "", $0)
      gsub(/^[[:space:]]*"/, "", $0)
      gsub(/"[[:space:]]*,?[[:space:]]*$/, "", $0)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", $0)
      print $0
      exit
    }
  ' "$TFVARS_FILE"
}

aws_cli() {
  aws "${AWS_ARGS[@]}" "$@"
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --owl-file)
        need_value "$@"
        OWL_FILE="$(abs_path "$2")"
        shift 2
        ;;
      --previous-text|--previous-text-path)
        need_value "$@"
        PREVIOUS_TEXT_PATH="$(abs_path "$2")"
        shift 2
        ;;
      --publication-date)
        need_value "$@"
        PUBLICATION_DATE="$2"
        shift 2
        ;;
      --report-type)
        need_value "$@"
        REPORT_TYPE="$(printf '%s' "$2" | tr '[:upper:]' '[:lower:]')"
        shift 2
        ;;
      --payload-file)
        need_value "$@"
        PAYLOAD_FILE="$(abs_path "$2")"
        shift 2
        ;;
      --delivery-emails)
        need_value "$@"
        DELIVERY_EMAILS="$2"
        shift 2
        ;;
      --concept-codes)
        need_value "$@"
        CONCEPT_CODES="$2"
        shift 2
        ;;
      --pairing-concept-codes)
        need_value "$@"
        PAIRING_CONCEPT_CODES="$2"
        shift 2
        ;;
      --tfvars)
        need_value "$@"
        TFVARS_FILE="$(abs_path "$2")"
        shift 2
        ;;
      --aws-profile)
        need_value "$@"
        AWS_PROFILE="$2"
        shift 2
        ;;
      --aws-region)
        need_value "$@"
        AWS_REGION="$2"
        shift 2
        ;;
      --poll-seconds)
        need_value "$@"
        POLL_SECONDS="$2"
        shift 2
        ;;
      --skip-destroy)
        DESTROY_FIRST=0
        shift
        ;;
      --skip-efs-verify)
        VERIFY_EFS=0
        shift
        ;;
      -y|--yes)
        YES=1
        shift
        ;;
      -h|--help)
        usage
        exit 0
        ;;
      *)
        die "Unknown option: $1"
        ;;
    esac
  done
}

validate_inputs() {
  [[ -n "$OWL_FILE" ]] || die "--owl-file is required"
  [[ -f "$OWL_FILE" ]] || die "OWL file does not exist: $OWL_FILE"

  [[ -n "$PREVIOUS_TEXT_PATH" ]] || die "--previous-text is required"
  [[ -e "$PREVIOUS_TEXT_PATH" ]] || die "Previous text path does not exist: $PREVIOUS_TEXT_PATH"
  if [[ -f "$PREVIOUS_TEXT_PATH" && "$PREVIOUS_TEXT_PATH" != *.txt ]]; then
    die "--previous-text file must be a .txt file. Pass a directory for multiple prior-quarter text files."
  fi

  [[ -f "$TFVARS_FILE" ]] || die "Terraform var file does not exist: $TFVARS_FILE"

  case "$REPORT_TYPE" in
    cdisc|ich) ;;
    *) die "--report-type must be cdisc or ich" ;;
  esac

  if [[ -n "$PAYLOAD_FILE" ]]; then
    [[ -f "$PAYLOAD_FILE" ]] || die "Payload file does not exist: $PAYLOAD_FILE"
  else
    [[ -n "$PUBLICATION_DATE" ]] || die "--publication-date is required unless --payload-file is provided"
    [[ "$PUBLICATION_DATE" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]] || die "--publication-date must be YYYY-MM-DD"
  fi

  [[ "$POLL_SECONDS" =~ ^[0-9]+$ ]] || die "--poll-seconds must be a positive integer"
  [[ "$POLL_SECONDS" -gt 0 ]] || die "--poll-seconds must be greater than zero"
}

load_tfvars_defaults() {
  AWS_PROFILE="${AWS_PROFILE:-$(read_tfvar_string aws_profile)}"
  AWS_REGION="${AWS_REGION:-$(read_tfvar_string aws_region)}"
  AWS_REGION="${AWS_REGION:-us-west-2}"
  THESAURUS_BUCKET="$(read_tfvar_string thesaurus_bucket)"

  [[ -n "$THESAURUS_BUCKET" ]] || die "Could not read thesaurus_bucket from $TFVARS_FILE"
}

check_prerequisites() {
  local java_version java_major terraform_version terraform_minor wrapper_version

  log "Checking prerequisites from the wiki runbook"
  require_command git
  require_command java
  require_command terraform
  require_command gradle
  require_command aws
  require_command awk
  require_command sed

  [[ -x "${REPO_ROOT}/gradlew" ]] || die "Gradle wrapper is missing or not executable: ${REPO_ROOT}/gradlew"
  wrapper_version="$(sed -n 's|.*gradle-\([0-9][^-]*\)-bin.zip.*|\1|p' "${REPO_ROOT}/gradle/wrapper/gradle-wrapper.properties")"
  if [[ "$wrapper_version" != "7.4.2" ]]; then
    warn "Gradle wrapper is $wrapper_version; wiki tested with Gradle 7.4.2"
  else
    log "Gradle wrapper is pinned to 7.4.2"
  fi

  java_version="$(java -version 2>&1 | awk -F '"' '/version/ {print $2; exit}')"
  java_major="${java_version%%.*}"
  if [[ "$java_major" == "1" ]]; then
    java_major="$(printf '%s' "$java_version" | awk -F '.' '{print $2}')"
  fi
  [[ "$java_major" == "11" ]] || die "Java 11 is required by the wiki; found version ${java_version:-unknown}"
  log "Java version is $java_version"

  terraform_version="$(terraform version | awk '/^Terraform v/ {gsub(/^v/, "", $2); print $2; exit}')"
  terraform_minor="$(printf '%s' "$terraform_version" | awk -F '.' '{print $1 "." $2}')"
  if [[ "$terraform_minor" != "1.11" ]]; then
    warn "Terraform is $terraform_version; wiki tested with Terraform 1.11.2"
  else
    log "Terraform version is $terraform_version"
  fi

  if [[ "$(gradle --version | awk '/^Gradle / {print $2; exit}')" != "7.4.2" ]]; then
    warn "System Gradle is $(gradle --version | awk '/^Gradle / {print $2; exit}'); wiki tested with Gradle 7.4.2. The build will use the repo wrapper."
  else
    log "System Gradle version is 7.4.2"
  fi

  AWS_ARGS=(--region "$AWS_REGION")
  if [[ -n "$AWS_PROFILE" ]]; then
    AWS_ARGS+=(--profile "$AWS_PROFILE")
  fi

  log "Checking AWS access in region $AWS_REGION"
  aws_cli sts get-caller-identity --query Arn --output text >/dev/null || die "AWS credentials are not valid for profile '${AWS_PROFILE:-default}'"
  aws_cli s3api head-bucket --bucket "$THESAURUS_BUCKET" >/dev/null || die "Cannot access S3 bucket: $THESAURUS_BUCKET"
}

update_repo() {
  log "Checking out main and pulling latest code"
  cd "$REPO_ROOT"
  run git fetch origin main
  run git checkout main
  run git pull --ff-only origin main
}

build_lambdas() {
  log "Building Lambda zip artifacts without tests"
  cd "$REPO_ROOT"
  run ./gradlew clean buildZip -x test
}

terraform_apply() {
  local auto_approve=()

  if [[ "$YES" -eq 1 ]]; then
    auto_approve=(-auto-approve)
  fi

  log "Running Terraform"
  cd "${REPO_ROOT}/terraform"
  run terraform init

  if [[ "$DESTROY_FIRST" -eq 1 ]]; then
    confirm "Destroy Terraform-managed AWS resources before recreating them?"
    run terraform apply -destroy -var-file "$TFVARS_FILE" "${auto_approve[@]}"
  fi

  run terraform apply -var-file "$TFVARS_FILE" "${auto_approve[@]}"
}

cleanup_s3_inputs() {
  local s3_root="s3://${THESAURUS_BUCKET}/${S3_PREFIX}"
  local previous_uri="${s3_root}/work/Previous"
  local key

  confirm "Delete prior OWL files from ${s3_root}/ and prior text files from ${previous_uri}/?"

  log "Removing top-level OWL files from ${s3_root}/"
  aws_cli s3api list-objects-v2 \
    --bucket "$THESAURUS_BUCKET" \
    --prefix "${S3_PREFIX}/" \
    --delimiter "/" \
    --query 'Contents[].Key' \
    --output text |
    tr '\t' '\n' |
    while IFS= read -r key; do
      case "$key" in
        *.owl) run aws "${AWS_ARGS[@]}" s3 rm "s3://${THESAURUS_BUCKET}/${key}" ;;
      esac
    done

  log "Removing previous-quarter text files from ${previous_uri}/"
  run aws "${AWS_ARGS[@]}" s3 rm "${previous_uri}/" --recursive
}

upload_s3_inputs() {
  local s3_root="s3://${THESAURUS_BUCKET}/${S3_PREFIX}"
  local previous_uri="${s3_root}/work/Previous"

  log "Uploading OWL file to ${s3_root}/"
  run aws "${AWS_ARGS[@]}" s3 cp "$OWL_FILE" "${s3_root}/"

  if [[ -d "$PREVIOUS_TEXT_PATH" ]]; then
    log "Uploading prior-quarter .txt files from ${PREVIOUS_TEXT_PATH} to ${previous_uri}/"
    run aws "${AWS_ARGS[@]}" s3 cp "$PREVIOUS_TEXT_PATH" "${previous_uri}/" --recursive --exclude "*" --include "*.txt"
  else
    log "Uploading prior-quarter text file to ${previous_uri}/"
    run aws "${AWS_ARGS[@]}" s3 cp "$PREVIOUS_TEXT_PATH" "${previous_uri}/"
  fi
}

find_datasync_task_arn() {
  aws_cli datasync list-tasks \
    --query "Tasks[?Name=='${DATASYNC_TASK_NAME}'].TaskArn | [0]" \
    --output text
}

run_datasync() {
  local task_arn execution_arn status

  log "Starting DataSync task ${DATASYNC_TASK_NAME}"
  task_arn="$(find_datasync_task_arn)"
  [[ -n "$task_arn" && "$task_arn" != "None" ]] || die "Could not find DataSync task named ${DATASYNC_TASK_NAME}"

  execution_arn="$(aws_cli datasync start-task-execution \
    --task-arn "$task_arn" \
    --query TaskExecutionArn \
    --output text)"
  [[ -n "$execution_arn" && "$execution_arn" != "None" ]] || die "DataSync did not return an execution ARN"
  log "DataSync execution started: $execution_arn"

  while true; do
    status="$(aws_cli datasync describe-task-execution \
      --task-execution-arn "$execution_arn" \
      --query Status \
      --output text)"
    log "DataSync status: $status"
    case "$status" in
      SUCCESS) break ;;
      ERROR) die "DataSync failed: $execution_arn" ;;
      *) sleep "$POLL_SECONDS" ;;
    esac
  done
}

verify_efs_contents() {
  local output_file

  [[ "$VERIFY_EFS" -eq 1 ]] || return 0

  log "Invoking list-efs-3 to verify EFS contents, if that helper Lambda exists"
  if ! aws_cli lambda get-function --function-name list-efs-3 >/dev/null 2>&1; then
    warn "Lambda list-efs-3 was not found; skipping EFS listing verification"
    return 0
  fi

  output_file="$(mktemp "${TMPDIR:-/tmp}/cdisc-list-efs-3.XXXXXX")"
  aws_cli lambda invoke --function-name list-efs-3 "$output_file" >/dev/null
  log "list-efs-3 output:"
  sed -n '1,120p' "$output_file"
  rm -f "$output_file"
}

generate_payload() {
  local owl_basename efs_owl concepts emails pairing_concepts
  owl_basename="$(basename "$OWL_FILE")"
  efs_owl="/mnt/cdisc/${owl_basename}"
  emails="$(json_array_from_csv "$DELIVERY_EMAILS")"

  GENERATED_PAYLOAD_FILE="$(mktemp "${TMPDIR:-/tmp}/cdisc-step-input.XXXXXX.json")"

  if [[ "$REPORT_TYPE" == "ich" ]]; then
    concepts="$(json_array_from_csv "${CONCEPT_CODES:-C217023}")"
    cat > "$GENERATED_PAYLOAD_FILE" <<EOF
{
  "thesaurusOwlFile": "$(json_escape "$efs_owl")",
  "publicationDate": "$(json_escape "$PUBLICATION_DATE")",
  "conceptCodes": $concepts,
  "deliveryEmailAddresses": $emails,
  "skipPairingReport": true
}
EOF
  else
    concepts="$(json_array_from_csv "${CONCEPT_CODES:-C81222,C77527,C67497,C132298,C120166,C66830,C77526,C165634,C188693,C203912,C207069}")"
    pairing_concepts="$(json_array_from_csv "$PAIRING_CONCEPT_CODES")"
    cat > "$GENERATED_PAYLOAD_FILE" <<EOF
{
  "thesaurusOwlFile": "$(json_escape "$efs_owl")",
  "publicationDate": "$(json_escape "$PUBLICATION_DATE")",
  "conceptCodes": $concepts,
  "deliveryEmailAddresses": $emails,
  "pairingReportRequest": {
    "thesaurusOwlFile": "$(json_escape "$efs_owl")",
    "publicationDate": "$(json_escape "$PUBLICATION_DATE")",
    "conceptCodes": $pairing_concepts
  }
}
EOF
  fi

  PAYLOAD_FILE="$GENERATED_PAYLOAD_FILE"
}

find_state_machine_arn() {
  aws_cli stepfunctions list-state-machines \
    --query "stateMachines[?name=='${STATE_MACHINE_NAME}'].stateMachineArn | [0]" \
    --output text
}

run_step_function() {
  local state_machine_arn execution_arn status

  if [[ -z "$PAYLOAD_FILE" ]]; then
    generate_payload
  fi

  log "Starting Step Function ${STATE_MACHINE_NAME} with input:"
  sed 's/^/  /' "$PAYLOAD_FILE"

  state_machine_arn="$(find_state_machine_arn)"
  [[ -n "$state_machine_arn" && "$state_machine_arn" != "None" ]] || die "Could not find Step Function named ${STATE_MACHINE_NAME}"

  execution_arn="$(aws_cli stepfunctions start-execution \
    --state-machine-arn "$state_machine_arn" \
    --input "file://${PAYLOAD_FILE}" \
    --query executionArn \
    --output text)"
  [[ -n "$execution_arn" && "$execution_arn" != "None" ]] || die "Step Functions did not return an execution ARN"
  log "Step Function execution started: $execution_arn"

  while true; do
    status="$(aws_cli stepfunctions describe-execution \
      --execution-arn "$execution_arn" \
      --query status \
      --output text)"
    log "Step Function status: $status"
    case "$status" in
      SUCCEEDED) break ;;
      FAILED|TIMED_OUT|ABORTED) die "Step Function ended with status $status: $execution_arn" ;;
      *) sleep "$POLL_SECONDS" ;;
    esac
  done
}

finish_message() {
  log "AWS report execution completed"
  cat <<'NEXT'

Manual post-run checks from the wiki:
- Review the shared Google Drive folder from the notification email.
- Delete stale paired files.
- Check each PDF for the correct release date and null values.
- Note any sources that were not updated this cycle before sending the distribution email.

When the AWS resources are no longer needed, destroy them from terraform/:
  terraform apply -destroy -var-file dev.tfvars
NEXT
}

main() {
  parse_args "$@"
  validate_inputs
  load_tfvars_defaults
  check_prerequisites
  update_repo
  build_lambdas
  terraform_apply
  cleanup_s3_inputs
  upload_s3_inputs
  run_datasync
  verify_efs_contents
  run_step_function
  finish_message
}

main "$@"
