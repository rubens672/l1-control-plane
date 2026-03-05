#!/usr/bin/env bash
set -euo pipefail #è una configurazione di sicurezza per gli script Bash

BASE="src/main/java/ai/berticloud/enroll"

mkdir -p src/{main,test}/{java,resources}
mkdir -p "$BASE"/{api,db,security,ca}
mkdir -p "$BASE"/api/dto

touch \
pom.xml \
Dockerfile \
src/main/resources/application.yml \
"$BASE/EnrollmentApplication.java" \
"$BASE/api/EnrollmentController.java" \
"$BASE/api/dto/SignCsrRequest.java" \
"$BASE/api/dto/SignCsrResponse.java" \
"$BASE/db/EnrollmentRepository.java" \
"$BASE/security/BootstrapTokenService.java" \
"$BASE/ca/CaMaterial.java" \
"$BASE/ca/CaMaterialLoader.java" \
"$BASE/ca/CsrAndCert"

echo "Project structure created."