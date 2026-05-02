#!/usr/bin/env sh
set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

chmod +x .githooks/pre-commit .githooks/pre-push
git config core.hooksPath .githooks

printf '%s\n' "Git hooks installed: core.hooksPath=.githooks"
