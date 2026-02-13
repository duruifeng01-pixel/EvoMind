#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mvn -s .mvn/settings.xml test
