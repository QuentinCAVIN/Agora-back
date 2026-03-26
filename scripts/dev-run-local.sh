#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

if [[ ! -f ".env" ]]; then
  echo "Erreur: fichier .env introuvable à la racine du repo."
  echo "Copie d'abord: cp .env.example .env"
  exit 1
fi

docker compose up -d postgres

set -a
# shellcheck disable=SC1091
source ./.env
set +a

./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

