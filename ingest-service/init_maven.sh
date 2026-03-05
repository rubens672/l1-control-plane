#!/usr/bin/env bash
set -euo pipefail #è una configurazione di sicurezza per gli script Bash

BASE="src/main/java/ai/berticloud/ingest"

mkdir -p src/{main,test}/{java,resources}
mkdir -p "$BASE"/{config,auth,db,api}

touch \
pom.xml \
Dockerfile \
src/main/resources/application.yml \
"$BASE/IngestApplication.java" \
"$BASE/config/CacheConfig.java" \
"$BASE/auth/DeviceAuthContext.java" \
"$BASE/auth/AuthzService.java" \
"$BASE/db/AuthzRepository.java" \
"$BASE/api/IngestController.java"

echo "Project structure created."