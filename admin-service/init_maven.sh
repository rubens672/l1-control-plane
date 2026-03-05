#!/usr/bin/env bash
set -euo pipefail #è una configurazione di sicurezza per gli script Bash

BASE="src/main/java/ai/berticloud/admin"

mkdir -p src/{main,test}/{java,resources}
mkdir -p "$BASE"/{api,db,security,util}
mkdir -p "$BASE"/api/dto

touch src/main/resources/application.yml

touch \
pom.xml \
Dockerfile \
src/main/resources/application.yml \
"$BASE/AdminApplication.java" \
"$BASE/api/AdminController.java" \
"$BASE/api/dto/*.java" \
"$BASE/db/AdminRepository.java" \
"$BASE/security/BootstrapTokenIssuer.java" \
"$BASE/util/CryptoUtil.java"

echo "Project structure created."