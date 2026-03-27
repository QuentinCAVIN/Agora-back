#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:${BACKEND_PORT:-8082}}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@agora.local}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Password123!}"

echo "Base URL: ${BASE_URL}"

login_json="$(curl -sS -X POST "${BASE_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"${ADMIN_EMAIL}\",\"password\":\"${ADMIN_PASSWORD}\"}")"

token="$(python3 - <<'PY'
import json,sys
data=json.loads(sys.stdin.read())
print(data["accessToken"])
PY
<<<"${login_json}")"

echo "OK login (token obtenu)"

curl -sS -o /dev/null -w "OK /api/auth/me (%{http_code})\n" \
  -H "Authorization: Bearer ${token}" \
  "${BASE_URL}/api/auth/me"

curl -sS -o /dev/null -w "OK /api/resources (%{http_code})\n" \
  "${BASE_URL}/api/resources"

create_json="$(curl -sS -X POST "${BASE_URL}/api/resources" \
  -H "Authorization: Bearer ${token}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Smoke Test - Ressource temporaire",
    "resourceType": "MOBILIER",
    "capacity": 1,
    "description": "Créée par scripts/smoke-test.sh",
    "depositAmountCents": 0,
    "imageUrl": null,
    "accessibilityTags": []
  }')"

resource_id="$(python3 - <<'PY'
import json,sys
data=json.loads(sys.stdin.read())
print(data["id"])
PY
<<<"${create_json}")"

echo "OK create resource id=${resource_id}"

curl -sS -o /dev/null -w "OK delete resource (%{http_code})\n" \
  -H "Authorization: Bearer ${token}" \
  -X DELETE "${BASE_URL}/api/resources/${resource_id}"

echo "Smoke test terminé."

