#!/bin/bash

# setup-plan.sh - Initialize feature planning structure
# Usage: scripts/setup-plan.sh [--json]

set -e

# Parse arguments
OUTPUT_JSON=false
if [[ "$1" == "--json" ]]; then
    OUTPUT_JSON=true
fi

# Determine current git branch
BRANCH=$(git branch --show-current)

# Extract feature number from branch name (format: ###-feature-name)
FEATURE_NUM=$(echo "$BRANCH" | grep -oE '^[0-9]+' || echo "")

if [[ -z "$FEATURE_NUM" ]]; then
    echo "Error: Branch name must start with feature number (e.g., 003-business-module)" >&2
    exit 1
fi

# Setup paths
REPO_ROOT=$(git rev-parse --show-toplevel)
SPECS_DIR="$REPO_ROOT/specs/$BRANCH"
FEATURE_SPEC="$SPECS_DIR/spec.md"
IMPL_PLAN="$SPECS_DIR/plan.md"
RESEARCH_DOC="$SPECS_DIR/research.md"
DATA_MODEL="$SPECS_DIR/data-model.md"
QUICKSTART="$SPECS_DIR/quickstart.md"
CONTRACTS_DIR="$SPECS_DIR/contracts"

# Create directory structure
mkdir -p "$SPECS_DIR"
mkdir -p "$CONTRACTS_DIR"

# Copy plan template if plan.md doesn't exist
if [[ ! -f "$IMPL_PLAN" ]]; then
    cp "$REPO_ROOT/templates/plan-template.md" "$IMPL_PLAN"
fi

# Output results
if $OUTPUT_JSON; then
    cat <<EOF
{
  "BRANCH": "$BRANCH",
  "FEATURE_NUM": "$FEATURE_NUM",
  "SPECS_DIR": "$SPECS_DIR",
  "FEATURE_SPEC": "$FEATURE_SPEC",
  "IMPL_PLAN": "$IMPL_PLAN",
  "RESEARCH_DOC": "$RESEARCH_DOC",
  "DATA_MODEL": "$DATA_MODEL",
  "QUICKSTART": "$QUICKSTART",
  "CONTRACTS_DIR": "$CONTRACTS_DIR"
}
EOF
else
    echo "Feature planning initialized:"
    echo "  Branch: $BRANCH"
    echo "  Feature: $FEATURE_NUM"
    echo "  Specs: $SPECS_DIR"
    echo "  Plan: $IMPL_PLAN"
fi
