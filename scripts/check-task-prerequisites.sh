#!/bin/bash

# check-task-prerequisites.sh - Verify planning artifacts exist before task generation
# Usage: scripts/check-task-prerequisites.sh [--json]

set -e

# Parse arguments
OUTPUT_JSON=false
if [[ "$1" == "--json" ]]; then
    OUTPUT_JSON=true
fi

# Determine current git branch
BRANCH=$(git branch --show-current)

# Setup paths
REPO_ROOT=$(git rev-parse --show-toplevel)
FEATURE_DIR="$REPO_ROOT/specs/$BRANCH"

# Check if feature directory exists
if [[ ! -d "$FEATURE_DIR" ]]; then
    echo "Error: Feature directory not found: $FEATURE_DIR" >&2
    echo "Please run /plan command first" >&2
    exit 1
fi

# Find available documents
AVAILABLE_DOCS=()

if [[ -f "$FEATURE_DIR/plan.md" ]]; then
    AVAILABLE_DOCS+=("plan.md")
fi

if [[ -f "$FEATURE_DIR/spec.md" ]]; then
    AVAILABLE_DOCS+=("spec.md")
fi

if [[ -f "$FEATURE_DIR/research.md" ]]; then
    AVAILABLE_DOCS+=("research.md")
fi

if [[ -f "$FEATURE_DIR/data-model.md" ]]; then
    AVAILABLE_DOCS+=("data-model.md")
fi

if [[ -f "$FEATURE_DIR/quickstart.md" ]]; then
    AVAILABLE_DOCS+=("quickstart.md")
fi

if [[ -d "$FEATURE_DIR/contracts" ]]; then
    CONTRACT_FILES=$(find "$FEATURE_DIR/contracts" -type f -name "*.yaml" -o -name "*.yml" -o -name "*.json" 2>/dev/null | wc -l | tr -d ' ')
    if [[ "$CONTRACT_FILES" -gt 0 ]]; then
        AVAILABLE_DOCS+=("contracts/")
    fi
fi

# Check minimum requirements
if [[ ! -f "$FEATURE_DIR/plan.md" ]]; then
    echo "Error: plan.md not found. Please run /plan command first" >&2
    exit 1
fi

# Output results
if $OUTPUT_JSON; then
    echo "{"
    echo "  \"BRANCH\": \"$BRANCH\","
    echo "  \"FEATURE_DIR\": \"$FEATURE_DIR\","
    echo "  \"AVAILABLE_DOCS\": ["

    DOC_COUNT=${#AVAILABLE_DOCS[@]}
    for i in "${!AVAILABLE_DOCS[@]}"; do
        if [[ $i -eq $((DOC_COUNT - 1)) ]]; then
            echo "    \"${AVAILABLE_DOCS[$i]}\""
        else
            echo "    \"${AVAILABLE_DOCS[$i]}\","
        fi
    done

    echo "  ]"
    echo "}"
else
    echo "Feature directory: $FEATURE_DIR"
    echo "Available documents:"
    for doc in "${AVAILABLE_DOCS[@]}"; do
        echo "  - $doc"
    done
fi
