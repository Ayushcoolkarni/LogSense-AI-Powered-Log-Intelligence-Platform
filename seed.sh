#!/bin/bash
# ============================================================
#  Log Intelligence Platform — Sample Data Seeder
#  Run this after docker-compose up -d
#  Usage: chmod +x seed.sh && ./seed.sh
# ============================================================

BASE="http://localhost:8081/api/v1/logs"
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

log() { echo -e "${BLUE}[SEED]${NC} $1"; }
ok()  { echo -e "${GREEN}[OK]${NC} $1"; }
warn(){ echo -e "${YELLOW}[WARN]${NC} $1"; }

wait_for_service() {
  log "Waiting for log-ingestion-service to be ready..."
  for i in $(seq 1 30); do
    if curl -sf http://localhost:8081/actuator/health > /dev/null 2>&1; then
      ok "Service is ready!"
      return 0
    fi
    echo -n "."
    sleep 3
  done
  echo ""
  warn "Service didn't respond in 90s. Proceeding anyway..."
}

post_log() {
  curl -s -X POST "$BASE" \
    -H "Content-Type: application/json" \
    -d "$1" > /dev/null
}

post_batch() {
  curl -s -X POST "$BASE/batch" \
    -H "Content-Type: application/json" \
    -d "$1" > /dev/null
}

NOW=$(date -u +%Y-%m-%dT%H:%M:%SZ)
MINUS1=$(date -u -d '1 minute ago' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v-1M +%Y-%m-%dT%H:%M:%SZ)
MINUS5=$(date -u -d '5 minutes ago' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v-5M +%Y-%m-%dT%H:%M:%SZ)
MINUS15=$(date -u -d '15 minutes ago' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v-15M +%Y-%m-%dT%H:%M:%SZ)
MINUS30=$(date -u -d '30 minutes ago' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v-30M +%Y-%m-%dT%H:%M:%SZ)
MINUS60=$(date -u -d '60 minutes ago' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v-60M +%Y-%m-%dT%H:%M:%SZ)

wait_for_service

echo ""
echo "============================================================"
echo "  Seeding 10 realistic incident scenarios..."
echo "============================================================"
echo ""

# ── INCIDENT 1: Payment Service — ERROR RATE SPIKE (CRITICAL) ──────────────
log "Incident 1/10: payment-service ERROR rate spike (CRITICAL)..."
for i in $(seq 1 55); do
  post_log "{
    \"serviceName\": \"payment-service\",
    \"logLevel\": \"ERROR\",
    \"message\": \"PaymentGatewayException: Connection to Stripe API timed out after 30000ms\",
    \"timestamp\": \"$NOW\",
    \"traceId\": \"trace-pay-$(printf '%03d' $i)\",
    \"environment\": \"prod\",
    \"hostName\": \"payment-pod-3\",
    \"stackTrace\": \"com.logplatform.payment.PaymentGatewayException: Connection timeout\\n\\tat com.logplatform.payment.StripeClient.charge(StripeClient.java:142)\\n\\tat com.logplatform.payment.PaymentService.processPayment(PaymentService.java:87)\"
  }"
done
ok "payment-service: 55 errors sent → ErrorRateSpikeStrategy triggered"

# ── INCIDENT 2: Order Service — SERVICE UNAVAILABLE (CRITICAL) ────────────
log "Incident 2/10: order-service → DB connection refused (CRITICAL)..."
post_log "{
  \"serviceName\": \"order-service\",
  \"logLevel\": \"ERROR\",
  \"message\": \"Cannot acquire connection from pool — connection refused to postgres:5432\",
  \"timestamp\": \"$MINUS1\",
  \"traceId\": \"trace-ord-001\",
  \"environment\": \"prod\",
  \"hostName\": \"order-pod-1\",
  \"stackTrace\": \"org.postgresql.util.PSQLException: Connection refused\\n\\tat org.postgresql.jdbc.PgConnection.<init>(PgConnection.java:214)\"
}"
ok "order-service: DB unavailable error sent → ServiceUnavailableStrategy triggered"

# ── INCIDENT 3: Cart Service — REPEATED ERROR PATTERN (MEDIUM) ────────────
log "Incident 3/10: cart-service repeated Redis timeout (MEDIUM)..."
for i in $(seq 1 12); do
  post_log "{
    \"serviceName\": \"cart-service\",
    \"logLevel\": \"ERROR\",
    \"message\": \"RedisTimeoutException: Could not get resource from pool — timeout after 2000ms\",
    \"timestamp\": \"$MINUS5\",
    \"traceId\": \"trace-cart-$(printf '%02d' $i)\",
    \"environment\": \"prod\",
    \"hostName\": \"cart-pod-2\"
  }"
done
ok "cart-service: 12 identical Redis errors → RepeatedErrorPatternStrategy triggered"

# ── INCIDENT 4: Notification Service — LOG VOLUME SPIKE (MEDIUM) ──────────
log "Incident 4/10: notification-service log storm (MEDIUM)..."
post_batch "{
  \"logs\": [
    $(for i in $(seq 1 20); do
      echo "{\"serviceName\":\"notification-service\",\"logLevel\":\"DEBUG\",\"message\":\"Processing notification job id=$i for userId=user-$(( RANDOM % 1000 ))\",\"timestamp\":\"$MINUS5\",\"environment\":\"prod\"}"
      [ $i -lt 20 ] && echo ","
    done)
  ]
}"
ok "notification-service: batch of debug logs sent → LogVolumeSpikeStrategy triggered"

# ── INCIDENT 5: Inventory Service — UPSTREAM UNAVAILABLE (CRITICAL) ────────
log "Incident 5/10: inventory-service → upstream API unavailable (CRITICAL)..."
post_log "{
  \"serviceName\": \"inventory-service\",
  \"logLevel\": \"ERROR\",
  \"message\": \"503 Service Unavailable: upstream connect error or disconnect/reset before headers. reset reason: connection termination — warehouse-api:8090\",
  \"timestamp\": \"$MINUS5\",
  \"traceId\": \"trace-inv-001\",
  \"environment\": \"prod\",
  \"hostName\": \"inventory-pod-1\"
}"
ok "inventory-service: upstream 503 → ServiceUnavailableStrategy triggered"

# ── INCIDENT 6: Auth Service — REPEATED TOKEN FAILURES (MEDIUM) ───────────
log "Incident 6/10: auth-service JWT validation failures (MEDIUM)..."
for i in $(seq 1 11); do
  post_log "{
    \"serviceName\": \"auth-service\",
    \"logLevel\": \"WARN\",
    \"message\": \"JWTVerificationException: Token signature verification failed — possible key rotation issue\",
    \"timestamp\": \"$MINUS15\",
    \"traceId\": \"trace-auth-$(printf '%02d' $i)\",
    \"environment\": \"prod\",
    \"hostName\": \"auth-pod-1\"
  }"
done
ok "auth-service: 11 JWT warnings → RepeatedErrorPatternStrategy triggered"

# ── INCIDENT 7: Search Service — CIRCUIT BREAKER OPEN (CRITICAL) ──────────
log "Incident 7/10: search-service circuit breaker open (CRITICAL)..."
post_log "{
  \"serviceName\": \"search-service\",
  \"logLevel\": \"ERROR\",
  \"message\": \"CircuitBreakerException: circuit breaker open — elasticsearch cluster host unreachable after 10 failures\",
  \"timestamp\": \"$MINUS15\",
  \"traceId\": \"trace-srch-001\",
  \"environment\": \"prod\",
  \"hostName\": \"search-pod-2\"
}"
ok "search-service: circuit breaker open → ServiceUnavailableStrategy triggered"

# ── INCIDENT 8: Pricing Service — ERROR SPIKE (HIGH) ──────────────────────
log "Incident 8/10: pricing-service calculation errors (HIGH)..."
for i in $(seq 1 52); do
  post_log "{
    \"serviceName\": \"pricing-service\",
    \"logLevel\": \"ERROR\",
    \"message\": \"NullPointerException in PricingEngine.calculate() — discount rule returned null for productId=SKU-$(( RANDOM % 9999 ))\",
    \"timestamp\": \"$MINUS30\",
    \"traceId\": \"trace-price-$(printf '%03d' $i)\",
    \"environment\": \"prod\",
    \"hostName\": \"pricing-pod-1\"
  }"
done
ok "pricing-service: 52 NPE errors → ErrorRateSpikeStrategy triggered"

# ── INCIDENT 9: Shipping Service — HOST UNREACHABLE (CRITICAL) ────────────
log "Incident 9/10: shipping-service → carrier API unreachable (CRITICAL)..."
post_log "{
  \"serviceName\": \"shipping-service\",
  \"logLevel\": \"ERROR\",
  \"message\": \"ConnectException: No route to host — fedex-api.internal:443 is unreachable. All 3 retry attempts failed.\",
  \"timestamp\": \"$MINUS30\",
  \"traceId\": \"trace-ship-001\",
  \"environment\": \"prod\",
  \"hostName\": \"shipping-pod-3\"
}"
ok "shipping-service: no route to host → ServiceUnavailableStrategy triggered"

# ── INCIDENT 10: API Gateway — ERROR SPIKE + NORMAL TRAFFIC ───────────────
log "Incident 10/10: api-gateway downstream failures (HIGH) + normal INFO logs..."
# Normal traffic first
post_batch "{
  \"logs\": [
    {\"serviceName\":\"api-gateway\",\"logLevel\":\"INFO\",\"message\":\"GET /api/products 200 OK — 45ms\",\"timestamp\":\"$MINUS60\",\"environment\":\"prod\"},
    {\"serviceName\":\"api-gateway\",\"logLevel\":\"INFO\",\"message\":\"POST /api/orders 201 Created — 123ms\",\"timestamp\":\"$MINUS60\",\"environment\":\"prod\"},
    {\"serviceName\":\"api-gateway\",\"logLevel\":\"INFO\",\"message\":\"GET /api/cart 200 OK — 28ms\",\"timestamp\":\"$MINUS60\",\"environment\":\"prod\"}
  ]
}"
# Then errors
for i in $(seq 1 53); do
  post_log "{
    \"serviceName\": \"api-gateway\",
    \"logLevel\": \"ERROR\",
    \"message\": \"502 Bad Gateway — downstream service payment-service returned connection refused\",
    \"timestamp\": \"$MINUS60\",
    \"traceId\": \"trace-gw-$(printf '%03d' $i)\",
    \"environment\": \"prod\",
    \"hostName\": \"gateway-pod-1\"
  }"
done
ok "api-gateway: 53 bad gateway errors → ErrorRateSpikeStrategy triggered"

echo ""
echo "============================================================"
echo -e "  ${GREEN}All 10 incidents seeded!${NC}"
echo "============================================================"
echo ""
echo "Wait ~30 seconds for anomaly detection + RCA generation, then:"
echo ""
echo "  Dashboard:  http://localhost:3000"
echo "  Stats API:  http://localhost:8085/api/v1/incidents/dashboard"
echo "  Anomalies:  http://localhost:8082/api/v1/anomalies"
echo "  RCA:        http://localhost:8083/api/v1/rca"
echo "  Logs:       http://localhost:8081/api/v1/logs/stats"
echo ""
echo "Checking results now..."
sleep 5

echo ""
log "Ingestion stats:"
curl -s http://localhost:8081/api/v1/logs/stats | python3 -c "
import sys, json
d = json.load(sys.stdin)
print(f\"  Total logs ingested : {d.get('totalLogsIngested', 0)}\")
print(f\"  Last hour           : {d.get('logsInLastHour', 0)}\")
print(f\"  By level            : {d.get('logsByLevel', {})}\")
" 2>/dev/null || curl -s http://localhost:8081/api/v1/logs/stats

echo ""
log "Anomalies detected (wait 10-30s for detection):"
curl -s "http://localhost:8082/api/v1/anomalies?size=20" | python3 -c "
import sys, json
d = json.load(sys.stdin)
content = d.get('content', [])
print(f'  Total anomalies: {d.get(\"totalElements\", 0)}')
for a in content:
    print(f\"  [{a['severity']:8}] {a['serviceName']:25} {a['anomalyType']:30} → {a['status']}\")
" 2>/dev/null || echo "  (anomalies may still be processing)"

echo ""
ok "Seeding complete. Open http://localhost:3000 to see the dashboard!"
