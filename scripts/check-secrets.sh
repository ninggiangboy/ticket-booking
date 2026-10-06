#!/usr/bin/env bash
# Fails when a Stripe key or webhook secret is committed (DOC-63 §4.1).
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
if git grep -nE "sk_(test|live)_[A-Za-z0-9]{10,}|whsec_[A-Za-z0-9]{10,}" -- . ':!*.md' ':!**/*.example' ':!**/*IT.java' ':!**/*Test.java'; then
  echo "secret-looking value committed"
  exit 1
fi
