#!/usr/bin/env bash
set -euo pipefail #è una configurazione di sicurezza per gli script Bash

BASE="src/main/java/ai/berticloud/shared"

mkdir -p src/{main,test}/{java,resources}
mkdir -p "$BASE"/{security,identity}

touch src/main/resources/application.yml

touch \
  pom.xml \
  "$BASE/security/MtlsHeaders.java" \
  "$BASE/identity/DeviceIdentity.java" \
  "$BASE/identity/SanUriParser.java" \
  "$BASE/identity/SanUriSelector.java"

echo "Project structure created."