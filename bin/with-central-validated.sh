#!/usr/bin/env bash
# Helper: extract Central Portal credentials from the local secrets script,
# export them, and invoke gradle with the validated-deployments init script.
#
# Usage:
#   CENTRAL_DEPLOYMENT_ID=<uuid> examples/bin/with-central-validated.sh build
#
# CENTRAL_DEPLOYMENT_ID must be exported by the caller (the Central Portal
# deployment UUID of the VALIDATED deployment to consume).
#
# All arguments after the script name are forwarded verbatim to gradle.

set -euo pipefail

SECRETS="${MAVEN_CENTRAL_SECRETS:-$HOME/Works/System/bin/mavencentral-secrets}"
if [[ ! -x "$SECRETS" ]]; then
    echo "Cannot find executable secrets script at: $SECRETS" >&2
    echo "Set MAVEN_CENTRAL_SECRETS to override." >&2
    exit 1
fi

SECRETS_OUT="$("$SECRETS")"
extract() {
    # Read the value line immediately following the --- <NAME> --- marker.
    local name="$1"
    awk -v n="$name" '
        $0 == "--- " n " ---" { getline; print; exit }
    ' <<<"$SECRETS_OUT"
}

CENTRAL_USER="$(extract MAVEN_CENTRAL_USERNAME)"
CENTRAL_PASS="$(extract MAVEN_CENTRAL_PASSWORD)"

if [[ -z "$CENTRAL_USER" || -z "$CENTRAL_PASS" ]]; then
    echo "Failed to extract Central Portal credentials from secrets script." >&2
    exit 1
fi

if [[ -z "${CENTRAL_DEPLOYMENT_ID:-}" ]]; then
    echo "CENTRAL_DEPLOYMENT_ID must be exported before running this script." >&2
    exit 1
fi

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
INIT_SCRIPT="$SCRIPT_DIR/central-validated.init.gradle.kts"

export CENTRAL_USER CENTRAL_PASS CENTRAL_DEPLOYMENT_ID
exec gradle --init-script "$INIT_SCRIPT" "$@"
