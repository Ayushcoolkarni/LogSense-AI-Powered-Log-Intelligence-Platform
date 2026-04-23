/**
 * Log Intelligence Platform — Sample Data Seeder
 * Windows-compatible Node.js version of seed.sh
 *
 * Usage:
 *   node seed.js
 *
 * Requirements:
 *   Node.js 18+ (uses built-in fetch)
 *   All Docker containers must be running first
 */

const INGESTION = 'http://localhost:8081/api/v1/logs';

const now       = new Date();
const minus1m   = new Date(now - 1  * 60 * 1000);
const minus5m   = new Date(now - 5  * 60 * 1000);
const minus15m  = new Date(now - 15 * 60 * 1000);
const minus30m  = new Date(now - 30 * 60 * 1000);
const minus60m  = new Date(now - 60 * 60 * 1000);

const ts  = (d) => d.toISOString();

// ── helpers ────────────────────────────────────────────────────────────────

async function postLog(payload) {
  const res = await fetch(INGESTION, {
    method:  'POST',
    headers: { 'Content-Type': 'application/json' },
    body:    JSON.stringify(payload),
  });
  if (!res.ok) throw new Error(`POST /logs failed: ${res.status} ${await res.text()}`);
}

async function postBatch(logs) {
  const res = await fetch(`${INGESTION}/batch`, {
    method:  'POST',
    headers: { 'Content-Type': 'application/json' },
    body:    JSON.stringify({ logs }),
  });
  if (!res.ok) throw new Error(`POST /logs/batch failed: ${res.status} ${await res.text()}`);
}

async function waitForService(maxWaitSec = 90) {
  process.stdout.write('[SEED] Waiting for log-ingestion-service');
  for (let i = 0; i < maxWaitSec / 3; i++) {
    try {
      const r = await fetch('http://localhost:8081/actuator/health');
      if (r.ok) { console.log('\n[OK]  Service is ready!'); return; }
    } catch (_) {}
    process.stdout.write('.');
    await sleep(3000);
  }
  console.log('\n[WARN] Service not ready after ' + maxWaitSec + 's — proceeding anyway');
}

function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

function range(n) { return Array.from({ length: n }, (_, i) => i + 1); }

// ── main ───────────────────────────────────────────────────────────────────

async function seed() {
  await waitForService();

  console.log('\n============================================================');
  console.log('  Seeding 10 realistic incident scenarios...');
  console.log('============================================================\n');

  // ── INCIDENT 1: payment-service ERROR RATE SPIKE (CRITICAL) ──────────────
  console.log('[SEED] 1/10: payment-service ERROR rate spike (CRITICAL)...');
  for (const i of range(55)) {
    await postLog({
      serviceName: 'payment-service',
      logLevel:    'ERROR',
      message:     'PaymentGatewayException: Connection to Stripe API timed out after 30000ms',
      timestamp:   ts(now),
      traceId:     `trace-pay-${String(i).padStart(3, '0')}`,
      environment: 'prod',
      hostName:    'payment-pod-3',
      stackTrace:  'com.logplatform.payment.PaymentGatewayException: Connection timeout\n\tat com.logplatform.payment.StripeClient.charge(StripeClient.java:142)\n\tat com.logplatform.payment.PaymentService.processPayment(PaymentService.java:87)',
    });
  }
  console.log('[OK]  payment-service: 55 errors → ErrorRateSpikeStrategy triggered');

  // ── INCIDENT 2: order-service DB CONNECTION REFUSED (CRITICAL) ───────────
  console.log('[SEED] 2/10: order-service DB connection refused (CRITICAL)...');
  await postLog({
    serviceName: 'order-service',
    logLevel:    'ERROR',
    message:     'Cannot acquire connection from pool — connection refused to postgres:5432',
    timestamp:   ts(minus1m),
    traceId:     'trace-ord-001',
    environment: 'prod',
    hostName:    'order-pod-1',
    stackTrace:  'org.postgresql.util.PSQLException: Connection refused\n\tat org.postgresql.jdbc.PgConnection.<init>(PgConnection.java:214)',
  });
  console.log('[OK]  order-service: DB unavailable → ServiceUnavailableStrategy triggered');

  // ── INCIDENT 3: cart-service REPEATED Redis timeout (MEDIUM) ─────────────
  console.log('[SEED] 3/10: cart-service repeated Redis timeout (MEDIUM)...');
  for (const i of range(12)) {
    await postLog({
      serviceName: 'cart-service',
      logLevel:    'ERROR',
      message:     'RedisTimeoutException: Could not get resource from pool — timeout after 2000ms',
      timestamp:   ts(minus5m),
      traceId:     `trace-cart-${String(i).padStart(2, '0')}`,
      environment: 'prod',
      hostName:    'cart-pod-2',
    });
  }
  console.log('[OK]  cart-service: 12 Redis errors → RepeatedErrorPatternStrategy triggered');

  // ── INCIDENT 4: notification-service LOG VOLUME SPIKE (MEDIUM) ───────────
  console.log('[SEED] 4/10: notification-service log storm (MEDIUM)...');
  await postBatch(
    range(20).map(i => ({
      serviceName: 'notification-service',
      logLevel:    'DEBUG',
      message:     `Processing notification job id=${i} for userId=user-${Math.floor(Math.random() * 1000)}`,
      timestamp:   ts(minus5m),
      environment: 'prod',
      hostName:    'notification-pod-1',
    }))
  );
  console.log('[OK]  notification-service: 20 debug logs → LogVolumeSpikeStrategy triggered');

  // ── INCIDENT 5: inventory-service UPSTREAM 503 (CRITICAL) ────────────────
  console.log('[SEED] 5/10: inventory-service upstream unavailable (CRITICAL)...');
  await postLog({
    serviceName: 'inventory-service',
    logLevel:    'ERROR',
    message:     '503 Service Unavailable: upstream connect error or disconnect/reset before headers. reset reason: connection termination — warehouse-api:8090',
    timestamp:   ts(minus5m),
    traceId:     'trace-inv-001',
    environment: 'prod',
    hostName:    'inventory-pod-1',
  });
  console.log('[OK]  inventory-service: upstream 503 → ServiceUnavailableStrategy triggered');

  // ── INCIDENT 6: auth-service JWT VALIDATION FAILURES (MEDIUM) ────────────
  console.log('[SEED] 6/10: auth-service JWT validation failures (MEDIUM)...');
  for (const i of range(11)) {
    await postLog({
      serviceName: 'auth-service',
      logLevel:    'WARN',
      message:     'JWTVerificationException: Token signature verification failed — possible key rotation issue',
      timestamp:   ts(minus15m),
      traceId:     `trace-auth-${String(i).padStart(2, '0')}`,
      environment: 'prod',
      hostName:    'auth-pod-1',
    });
  }
  console.log('[OK]  auth-service: 11 JWT warnings → RepeatedErrorPatternStrategy triggered');

  // ── INCIDENT 7: search-service CIRCUIT BREAKER OPEN (CRITICAL) ───────────
  console.log('[SEED] 7/10: search-service circuit breaker open (CRITICAL)...');
  await postLog({
    serviceName: 'search-service',
    logLevel:    'ERROR',
    message:     'CircuitBreakerException: circuit breaker open — elasticsearch cluster host unreachable after 10 failures',
    timestamp:   ts(minus15m),
    traceId:     'trace-srch-001',
    environment: 'prod',
    hostName:    'search-pod-2',
  });
  console.log('[OK]  search-service: circuit breaker open → ServiceUnavailableStrategy triggered');

  // ── INCIDENT 8: pricing-service NPE SPIKE (HIGH) ─────────────────────────
  console.log('[SEED] 8/10: pricing-service NullPointerException spike (HIGH)...');
  for (const i of range(52)) {
    await postLog({
      serviceName: 'pricing-service',
      logLevel:    'ERROR',
      message:     `NullPointerException in PricingEngine.calculate() — discount rule returned null for productId=SKU-${Math.floor(Math.random() * 9999)}`,
      timestamp:   ts(minus30m),
      traceId:     `trace-price-${String(i).padStart(3, '0')}`,
      environment: 'prod',
      hostName:    'pricing-pod-1',
    });
  }
  console.log('[OK]  pricing-service: 52 NPE errors → ErrorRateSpikeStrategy triggered');

  // ── INCIDENT 9: shipping-service HOST UNREACHABLE (CRITICAL) ─────────────
  console.log('[SEED] 9/10: shipping-service carrier API unreachable (CRITICAL)...');
  await postLog({
    serviceName: 'shipping-service',
    logLevel:    'ERROR',
    message:     'ConnectException: No route to host — fedex-api.internal:443 is unreachable. All 3 retry attempts failed.',
    timestamp:   ts(minus30m),
    traceId:     'trace-ship-001',
    environment: 'prod',
    hostName:    'shipping-pod-3',
  });
  console.log('[OK]  shipping-service: no route to host → ServiceUnavailableStrategy triggered');

  // ── INCIDENT 10: api-gateway DOWNSTREAM FAILURES (HIGH) ──────────────────
  console.log('[SEED] 10/10: api-gateway downstream failures (HIGH) + normal traffic...');
  await postBatch([
    { serviceName: 'api-gateway', logLevel: 'INFO', message: 'GET /api/products 200 OK — 45ms',     timestamp: ts(minus60m), environment: 'prod', hostName: 'gateway-pod-1' },
    { serviceName: 'api-gateway', logLevel: 'INFO', message: 'POST /api/orders 201 Created — 123ms', timestamp: ts(minus60m), environment: 'prod', hostName: 'gateway-pod-1' },
    { serviceName: 'api-gateway', logLevel: 'INFO', message: 'GET /api/cart 200 OK — 28ms',          timestamp: ts(minus60m), environment: 'prod', hostName: 'gateway-pod-1' },
  ]);
  for (const i of range(53)) {
    await postLog({
      serviceName: 'api-gateway',
      logLevel:    'ERROR',
      message:     '502 Bad Gateway — downstream service payment-service returned connection refused',
      timestamp:   ts(minus60m),
      traceId:     `trace-gw-${String(i).padStart(3, '0')}`,
      environment: 'prod',
      hostName:    'gateway-pod-1',
    });
  }
  console.log('[OK]  api-gateway: 53 bad gateway errors → ErrorRateSpikeStrategy triggered');

  // ── Results ───────────────────────────────────────────────────────────────
  console.log('\n============================================================');
  console.log('  All 10 incidents seeded!');
  console.log('============================================================\n');
  console.log('Waiting 30 seconds for anomaly detection + RCA generation...\n');
  await sleep(30000);

  // Stats
  try {
    const stats = await fetch('http://localhost:8081/api/v1/logs/stats').then(r => r.json());
    console.log('[INFO] Ingestion stats:');
    console.log(`  Total logs ingested : ${stats.totalLogsIngested ?? stats.data?.totalLogsIngested ?? 'N/A'}`);
    console.log(`  Last hour           : ${stats.logsInLastHour  ?? stats.data?.logsInLastHour  ?? 'N/A'}`);
  } catch (e) {
    console.log('[WARN] Could not fetch ingestion stats:', e.message);
  }

  try {
    const anomalies = await fetch('http://localhost:8082/api/v1/anomalies?size=20').then(r => r.json());
    const list = anomalies.content ?? [];
    console.log(`\n[INFO] Anomalies detected: ${anomalies.totalElements ?? list.length}`);
    for (const a of list) {
      console.log(`  [${(a.severity ?? '').padEnd(8)}] ${(a.serviceName ?? '').padEnd(25)} ${(a.anomalyType ?? '').padEnd(30)} → ${a.status}`);
    }
  } catch (e) {
    console.log('[WARN] Anomalies still processing:', e.message);
  }

  console.log('\n[OK]  Open http://localhost:3000 to see the dashboard!');
  console.log('\n  Dashboard : http://localhost:3000');
  console.log('  Stats API : http://localhost:8085/api/v1/incidents/dashboard');
  console.log('  Anomalies : http://localhost:8082/api/v1/anomalies');
  console.log('  RCA       : http://localhost:8083/api/v1/rca');
  console.log('  Logs      : http://localhost:8081/api/v1/logs/stats\n');
}

seed().catch(err => {
  console.error('[ERROR]', err.message);
  process.exit(1);
});
