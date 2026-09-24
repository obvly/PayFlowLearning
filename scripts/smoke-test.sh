#!/usr/bin/env bash
set -euo pipefail

command -v jq >/dev/null || { echo 'Install jq to run the smoke test' >&2; exit 1; }
PAYMENT_URL=${PAYMENT_URL:-http://localhost:8080}
AUDIT_URL=${AUDIT_URL:-http://localhost:8081}

wait_for_health() {
  for attempt in $(seq 1 60); do
    if curl --max-time 3 -fsS "$1/actuator/health" 2>/dev/null |
      jq -e '.status == "UP"' >/dev/null; then
      return 0
    fi
    sleep 1
  done
  echo "Service did not become healthy: $1" >&2
  return 1
}

wait_for_health "$PAYMENT_URL"
wait_for_health "$AUDIT_URL"

key="smoke-$(uuidgen)"
payload='{"orderId":"ORD-SMOKE-001","amount":1.00,"currency":"EUR","description":"Smoke test"}'
create_payment() {
  curl --max-time 10 -fsS -X POST "$PAYMENT_URL/api/v1/payments" \
    -H 'Content-Type: application/json' -H "Idempotency-Key: $key" -d "$payload"
}

response=$(create_payment)
payment_id=$(printf '%s' "$response" | jq -er '.id')
printf '%s' "$response" | jq -e '.status == "PENDING"' >/dev/null
create_payment | jq -e --arg id "$payment_id" '.id == $id' >/dev/null

# Reusing a key with different content must not silently return the old payment.
code=$(curl --max-time 10 -sS -o /dev/null -w '%{http_code}' \
  -X POST "$PAYMENT_URL/api/v1/payments" \
  -H 'Content-Type: application/json' -H "Idempotency-Key: $key" \
  -d '{"orderId":"OTHER","amount":2.00,"currency":"EUR"}')
[ "$code" = 409 ]

# PENDING cannot be completed directly.
code=$(curl --max-time 10 -sS -o /dev/null -w '%{http_code}' \
  -X POST "$PAYMENT_URL/api/v1/payments/$payment_id/complete")
[ "$code" = 409 ]

for transition in process:PROCESSING complete:COMPLETED refund:REFUNDED; do
  action=${transition%:*}
  expected=${transition#*:}
  curl --max-time 10 -fsS -X POST "$PAYMENT_URL/api/v1/payments/$payment_id/$action" |
    jq -e --arg status "$expected" '.status == $status' >/dev/null
  # Read twice to exercise both a cache miss and a cache hit.
  for read in 1 2; do
    curl --max-time 10 -fsS "$PAYMENT_URL/api/v1/payments/$payment_id" |
      jq -e --arg status "$expected" '.status == $status' >/dev/null
  done
done

for attempt in $(seq 1 30); do
  audit=$(curl --max-time 5 -fsS "$AUDIT_URL/api/v1/audit-events/payment/$payment_id")
  if printf '%s' "$audit" | jq -e '
    length == 4 and
    ([.[].status] | sort == ["COMPLETED", "PENDING", "PROCESSING", "REFUNDED"])
  ' >/dev/null; then
    echo "Smoke test passed: lifecycle, replay, conflicts, cache reads and audit for $payment_id"
    exit 0
  fi
  sleep 1
done

echo "Expected four audit events were not received in time: $audit" >&2
exit 1
