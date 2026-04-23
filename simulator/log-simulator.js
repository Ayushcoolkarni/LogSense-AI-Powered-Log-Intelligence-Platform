/**
 * LogSense Traffic Simulator
 * ══════════════════════════════════════════════════════════════════════
 * Continuously generates realistic microservice logs with:
 *   - Normal traffic (INFO/DEBUG) — 85% of the time
 *   - Warning conditions          — 10% of the time
 *   - Incident injection          — 5% of the time (configurable)
 *
 * Simulated services:
 *   payment-service, order-service, cart-service, inventory-service,
 *   auth-service, notification-service, search-service, pricing-service,
 *   shipping-service, api-gateway
 *
 * Usage:
 *   node log-simulator.js                  # default: 5s interval
 *   node log-simulator.js --interval 3     # 3 second interval
 *   node log-simulator.js --incident-rate 15  # 15% incident rate
 *   node log-simulator.js --burst          # send a burst of incidents now
 */

const INGESTION_URL = process.env.INGESTION_URL || 'http://localhost:8081/api/v1/logs';

// ── CLI args ───────────────────────────────────────────────────────────────
const args = process.argv.slice(2);
const getArg = (name, def) => {
  const i = args.indexOf(`--${name}`);
  return i !== -1 ? args[i + 1] : def;
};
const INTERVAL_MS    = parseInt(getArg('interval', '5')) * 1000;
const INCIDENT_RATE  = parseInt(getArg('incident-rate', '5')) / 100;
const BURST_MODE     = args.includes('--burst');

// ── Service definitions ────────────────────────────────────────────────────
const SERVICES = [
  { name: 'payment-service',      hosts: ['payment-pod-1', 'payment-pod-2', 'payment-pod-3'] },
  { name: 'order-service',        hosts: ['order-pod-1', 'order-pod-2'] },
  { name: 'cart-service',         hosts: ['cart-pod-1', 'cart-pod-2'] },
  { name: 'inventory-service',    hosts: ['inventory-pod-1', 'inventory-pod-2'] },
  { name: 'auth-service',         hosts: ['auth-pod-1', 'auth-pod-2'] },
  { name: 'notification-service', hosts: ['notification-pod-1'] },
  { name: 'search-service',       hosts: ['search-pod-1', 'search-pod-2'] },
  { name: 'pricing-service',      hosts: ['pricing-pod-1'] },
  { name: 'shipping-service',     hosts: ['shipping-pod-1', 'shipping-pod-2', 'shipping-pod-3'] },
  { name: 'api-gateway',          hosts: ['gateway-pod-1', 'gateway-pod-2'] },
];

// ── Normal log templates per service ──────────────────────────────────────
const NORMAL_LOGS = {
  'payment-service': [
    () => ({ level: 'INFO',  msg: `Payment processed successfully — orderId=ORD-${rand(10000, 99999)} amount=$${(rand(10, 500) + rand(0,99)/100).toFixed(2)} method=CARD` }),
    () => ({ level: 'INFO',  msg: `Stripe charge created — chargeId=ch_${randHex(24)} status=succeeded` }),
    () => ({ level: 'DEBUG', msg: `Payment validation passed — userId=USR-${rand(1000,9999)} currency=USD` }),
    () => ({ level: 'INFO',  msg: `Refund initiated — refundId=re_${randHex(24)} amount=$${rand(5,200)}` }),
  ],
  'order-service': [
    () => ({ level: 'INFO',  msg: `Order created — orderId=ORD-${rand(10000,99999)} items=${rand(1,8)} userId=USR-${rand(1000,9999)}` }),
    () => ({ level: 'INFO',  msg: `Order status updated — orderId=ORD-${rand(10000,99999)} status=CONFIRMED` }),
    () => ({ level: 'DEBUG', msg: `Order validation completed in ${rand(5,45)}ms` }),
    () => ({ level: 'INFO',  msg: `Order fulfilled — orderId=ORD-${rand(10000,99999)} warehouse=WH-${rand(1,5)}` }),
  ],
  'cart-service': [
    () => ({ level: 'INFO',  msg: `Item added to cart — cartId=CART-${rand(1000,9999)} productId=SKU-${rand(100,999)} qty=${rand(1,5)}` }),
    () => ({ level: 'INFO',  msg: `Cart retrieved from cache — userId=USR-${rand(1000,9999)} items=${rand(0,12)}` }),
    () => ({ level: 'DEBUG', msg: `Cart TTL refreshed — cartId=CART-${rand(1000,9999)} ttl=3600s` }),
    () => ({ level: 'INFO',  msg: `Cart checkout initiated — cartId=CART-${rand(1000,9999)} total=$${rand(20,800)}` }),
  ],
  'inventory-service': [
    () => ({ level: 'INFO',  msg: `Stock reserved — productId=SKU-${rand(100,999)} qty=${rand(1,10)} warehouse=WH-${rand(1,5)}` }),
    () => ({ level: 'INFO',  msg: `Inventory sync completed — ${rand(50,500)} products updated` }),
    () => ({ level: 'DEBUG', msg: `Stock check passed — productId=SKU-${rand(100,999)} available=${rand(10,1000)}` }),
    () => ({ level: 'WARN',  msg: `Low stock alert — productId=SKU-${rand(100,999)} remaining=${rand(1,9)}` }),
  ],
  'auth-service': [
    () => ({ level: 'INFO',  msg: `User authenticated — userId=USR-${rand(1000,9999)} method=JWT duration=${rand(2,15)}ms` }),
    () => ({ level: 'INFO',  msg: `Token refreshed — userId=USR-${rand(1000,9999)} expiry=3600s` }),
    () => ({ level: 'DEBUG', msg: `Permission check passed — userId=USR-${rand(1000,9999)} resource=/api/orders` }),
    () => ({ level: 'INFO',  msg: `Session created — sessionId=SES-${randHex(16)} userId=USR-${rand(1000,9999)}` }),
  ],
  'notification-service': [
    () => ({ level: 'INFO',  msg: `Email sent — to=user${rand(1,999)}@example.com template=ORDER_CONFIRMED` }),
    () => ({ level: 'INFO',  msg: `Push notification delivered — userId=USR-${rand(1000,9999)} type=SHIPMENT_UPDATE` }),
    () => ({ level: 'DEBUG', msg: `Notification queued — jobId=JOB-${rand(10000,99999)} priority=NORMAL` }),
    () => ({ level: 'INFO',  msg: `SMS dispatched — to=+91${rand(7000000000,9999999999)} status=DELIVERED` }),
  ],
  'search-service': [
    () => ({ level: 'INFO',  msg: `Search query executed — query="${pick(['shoes','laptop','phone','shirt'])}" results=${rand(0,200)} took=${rand(10,80)}ms` }),
    () => ({ level: 'INFO',  msg: `Index updated — documents=${rand(1,50)} index=products` }),
    () => ({ level: 'DEBUG', msg: `Cache hit for query — key=search:${randHex(8)} age=${rand(1,300)}s` }),
    () => ({ level: 'INFO',  msg: `Faceted search completed — filters=${rand(1,5)} results=${rand(0,100)}` }),
  ],
  'pricing-service': [
    () => ({ level: 'INFO',  msg: `Price calculated — productId=SKU-${rand(100,999)} finalPrice=$${rand(5,500)} discount=${rand(0,30)}%` }),
    () => ({ level: 'INFO',  msg: `Pricing rule applied — ruleId=RULE-${rand(1,20)} type=SEASONAL` }),
    () => ({ level: 'DEBUG', msg: `Price cache populated — ${rand(10,100)} products cached` }),
    () => ({ level: 'INFO',  msg: `Flash sale activated — products=${rand(5,50)} discount=${rand(10,50)}%` }),
  ],
  'shipping-service': [
    () => ({ level: 'INFO',  msg: `Shipment created — trackingId=TRK-${rand(100000,999999)} carrier=FEDEX eta=${rand(1,7)}days` }),
    () => ({ level: 'INFO',  msg: `Shipment status updated — trackingId=TRK-${rand(100000,999999)} status=IN_TRANSIT` }),
    () => ({ level: 'DEBUG', msg: `Rate calculated — carrier=UPS weight=${rand(1,20)}kg zones=3` }),
    () => ({ level: 'INFO',  msg: `Delivery confirmed — trackingId=TRK-${rand(100000,999999)} signedBy=customer` }),
  ],
  'api-gateway': [
    () => ({ level: 'INFO',  msg: `GET /api/products 200 OK — ${rand(15,80)}ms` }),
    () => ({ level: 'INFO',  msg: `POST /api/orders 201 Created — ${rand(50,200)}ms` }),
    () => ({ level: 'INFO',  msg: `GET /api/cart/${rand(1000,9999)} 200 OK — ${rand(10,40)}ms` }),
    () => ({ level: 'INFO',  msg: `PUT /api/users/${rand(1000,9999)} 200 OK — ${rand(20,60)}ms` }),
  ],
};

// ── Incident scenarios ─────────────────────────────────────────────────────
const INCIDENTS = [
  {
    name: 'Payment Gateway Timeout',
    service: 'payment-service',
    count: 55,
    level: 'ERROR',
    msg: () => `PaymentGatewayException: Connection to Stripe API timed out after 30000ms`,
    stackTrace: 'com.logplatform.payment.PaymentGatewayException: Connection timeout\n\tat com.logplatform.payment.StripeClient.charge(StripeClient.java:142)',
  },
  {
    name: 'DB Connection Refused',
    service: 'order-service',
    count: 1,
    level: 'ERROR',
    msg: () => `Cannot acquire connection from pool — connection refused to postgres:5432`,
    stackTrace: 'org.postgresql.util.PSQLException: Connection refused\n\tat org.postgresql.jdbc.PgConnection.<init>(PgConnection.java:214)',
  },
  {
    name: 'Redis Timeout Loop',
    service: 'cart-service',
    count: 12,
    level: 'ERROR',
    msg: () => `RedisTimeoutException: Could not get resource from pool — timeout after 2000ms`,
  },
  {
    name: 'JWT Validation Failures',
    service: 'auth-service',
    count: 11,
    level: 'WARN',
    msg: () => `JWTVerificationException: Token signature verification failed — possible key rotation issue`,
  },
  {
    name: 'Circuit Breaker Open',
    service: 'search-service',
    count: 1,
    level: 'ERROR',
    msg: () => `CircuitBreakerException: circuit breaker open — elasticsearch cluster unreachable after 10 failures`,
  },
  {
    name: 'NPE in Pricing Engine',
    service: 'pricing-service',
    count: 52,
    level: 'ERROR',
    msg: () => `NullPointerException in PricingEngine.calculate() — discount rule returned null for productId=SKU-${rand(100,999)}`,
    stackTrace: 'java.lang.NullPointerException\n\tat com.logplatform.pricing.PricingEngine.calculate(PricingEngine.java:87)',
  },
  {
    name: 'Carrier API Unreachable',
    service: 'shipping-service',
    count: 1,
    level: 'ERROR',
    msg: () => `ConnectException: No route to host — fedex-api.internal:443 unreachable. All 3 retry attempts failed.`,
  },
  {
    name: 'Upstream 503',
    service: 'inventory-service',
    count: 1,
    level: 'ERROR',
    msg: () => `503 Service Unavailable — upstream connect error: warehouse-api:8090 connection termination`,
  },
  {
    name: 'Gateway Bad Gateway Spike',
    service: 'api-gateway',
    count: 53,
    level: 'ERROR',
    msg: () => `502 Bad Gateway — downstream service payment-service returned connection refused`,
  },
  {
    name: 'Notification Storm',
    service: 'notification-service',
    count: 25,
    level: 'DEBUG',
    msg: () => `Processing notification job id=${rand(1,9999)} for userId=USR-${rand(1000,9999)} [RETRY ${rand(1,5)}]`,
  },
];

// ── Utility functions ──────────────────────────────────────────────────────
function rand(min, max)  { return Math.floor(Math.random() * (max - min + 1)) + min; }
function randHex(len)    { return [...Array(len)].map(() => Math.floor(Math.random()*16).toString(16)).join(''); }
function pick(arr)       { return arr[Math.floor(Math.random() * arr.length)]; }
function traceId(pfx)    { return `${pfx}-${randHex(8)}-${randHex(4)}`; }
function sleep(ms)       { return new Promise(r => setTimeout(r, ms)); }

async function postLog(payload) {
  const res = await fetch(INGESTION_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    const txt = await res.text();
    throw new Error(`${res.status}: ${txt.slice(0, 100)}`);
  }
}

async function postBatch(logs) {
  const res = await fetch(`${INGESTION_URL}/batch`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ logs }),
  });
  if (!res.ok) {
    const txt = await res.text();
    throw new Error(`batch ${res.status}: ${txt.slice(0, 100)}`);
  }
}

// ── Normal traffic tick ────────────────────────────────────────────────────
async function sendNormalTraffic() {
  // Pick 3-6 random services and send normal logs for each
  const count = rand(3, 6);
  const selected = [...SERVICES].sort(() => Math.random() - 0.5).slice(0, count);

  const logs = [];
  for (const svc of selected) {
    const templates = NORMAL_LOGS[svc.name];
    const { level, msg } = pick(templates)();
    logs.push({
      serviceName: svc.name,
      logLevel:    level,
      message:     msg,
      timestamp:   new Date().toISOString(),
      traceId:     traceId('trc'),
      spanId:      randHex(16),
      hostName:    pick(svc.hosts),
      environment: 'prod',
    });
  }

  await postBatch(logs);
  return logs.length;
}

// ── Warning traffic ────────────────────────────────────────────────────────
async function sendWarningTraffic() {
  const warnings = [
    { service: 'payment-service',   msg: `Slow payment response — took ${rand(5000,15000)}ms, threshold=3000ms` },
    { service: 'order-service',     msg: `High order queue depth — ${rand(500,2000)} orders pending` },
    { service: 'inventory-service', msg: `Low stock alert — ${rand(5,15)} products below reorder threshold` },
    { service: 'api-gateway',       msg: `Rate limit approaching — ${rand(80,95)}% of quota used for userId=USR-${rand(1000,9999)}` },
    { service: 'search-service',    msg: `Search latency degraded — p99=${rand(500,2000)}ms (normal: 100ms)` },
    { service: 'auth-service',      msg: `Multiple failed login attempts — userId=USR-${rand(1000,9999)} attempts=${rand(3,8)}` },
  ];

  const w = pick(warnings);
  const svc = SERVICES.find(s => s.name === w.service);
  await postLog({
    serviceName: w.service,
    logLevel:    'WARN',
    message:     w.msg,
    timestamp:   new Date().toISOString(),
    traceId:     traceId('warn'),
    hostName:    pick(svc.hosts),
    environment: 'prod',
  });
  return w.service;
}

// ── Incident injection ─────────────────────────────────────────────────────
async function injectIncident() {
  const incident = pick(INCIDENTS);
  const svc = SERVICES.find(s => s.name === incident.service);
  const tid = traceId('inc');

  console.log(`\n🚨 INJECTING INCIDENT: ${incident.name} → ${incident.service} (${incident.count} logs)`);

  if (incident.count === 1) {
    await postLog({
      serviceName: incident.service,
      logLevel:    incident.level,
      message:     incident.msg(),
      timestamp:   new Date().toISOString(),
      traceId:     tid,
      hostName:    pick(svc.hosts),
      environment: 'prod',
      stackTrace:  incident.stackTrace,
    });
  } else {
    // Send in batches of 20 to avoid overwhelming the API
    const batchSize = 20;
    for (let i = 0; i < incident.count; i += batchSize) {
      const batch = [];
      const end = Math.min(i + batchSize, incident.count);
      for (let j = i; j < end; j++) {
        batch.push({
          serviceName: incident.service,
          logLevel:    incident.level,
          message:     incident.msg(),
          timestamp:   new Date().toISOString(),
          traceId:     `${tid}-${j}`,
          hostName:    pick(svc.hosts),
          environment: 'prod',
          stackTrace:  incident.stackTrace,
        });
      }
      await postBatch(batch);
      await sleep(100);
    }
  }

  return incident.name;
}

// ── Wait for service ───────────────────────────────────────────────────────
async function waitForService() {
  process.stdout.write('[SIM] Waiting for log-ingestion-service');
  for (let i = 0; i < 30; i++) {
    try {
      const r = await fetch('http://localhost:8081/actuator/health');
      if (r.ok) { console.log(' ✓ Ready!\n'); return; }
    } catch (_) {}
    process.stdout.write('.');
    await sleep(3000);
  }
  console.log('\n[WARN] Service not ready — starting anyway');
}

// ── Stats display ──────────────────────────────────────────────────────────
async function printStats() {
  try {
    const stats = await fetch('http://localhost:8081/api/v1/logs/stats').then(r => r.json());
    const d = stats.data ?? stats;
    process.stdout.write(
      `\r[STATS] Total: ${d.totalLogsIngested ?? '?'} | LastHour: ${d.logsInLastHour ?? '?'} | ERRORs: ${d.logsByLevel?.ERROR ?? '?'}   `
    );
  } catch (_) {}
}

// ── Burst mode — inject all incidents immediately ──────────────────────────
async function burstMode() {
  console.log('💥 BURST MODE — injecting all incident scenarios\n');
  for (const incident of INCIDENTS) {
    const svc = SERVICES.find(s => s.name === incident.service);
    const tid = traceId('burst');
    console.log(`  → ${incident.name} (${incident.service}, ${incident.count} logs)`);

    const logs = Array.from({ length: incident.count }, (_, j) => ({
      serviceName: incident.service,
      logLevel:    incident.level,
      message:     incident.msg(),
      timestamp:   new Date().toISOString(),
      traceId:     `${tid}-${j}`,
      hostName:    pick(svc.hosts),
      environment: 'prod',
      stackTrace:  incident.stackTrace,
    }));

    // Single log or batch
    if (logs.length === 1) {
      await postLog(logs[0]);
    } else {
      for (let i = 0; i < logs.length; i += 20) {
        await postBatch(logs.slice(i, i + 20));
        await sleep(50);
      }
    }
    await sleep(200);
  }
  console.log('\n✅ Burst complete! Wait 30s then check http://localhost:3000\n');
}

// ── Main loop ──────────────────────────────────────────────────────────────
async function main() {
  console.log('╔══════════════════════════════════════════════════════════╗');
  console.log('║       LogSense Traffic Simulator                        ║');
  console.log('╚══════════════════════════════════════════════════════════╝');
  console.log(`  Ingestion URL  : ${INGESTION_URL}`);
  console.log(`  Interval       : ${INTERVAL_MS / 1000}s`);
  console.log(`  Incident rate  : ${(INCIDENT_RATE * 100).toFixed(0)}%`);
  console.log(`  Burst mode     : ${BURST_MODE}`);
  console.log('  Press Ctrl+C to stop\n');

  await waitForService();

  if (BURST_MODE) {
    await burstMode();
    return;
  }

  let tick = 0;
  let totalLogs = 0;
  let incidents = 0;

  while (true) {
    tick++;
    const roll = Math.random();

    try {
      if (roll < INCIDENT_RATE) {
        // Inject an incident
        const name = await injectIncident();
        incidents++;
        console.log(`   ✓ Incident injected [total incidents: ${incidents}]`);
      } else if (roll < INCIDENT_RATE + 0.10) {
        // Warning traffic
        const svc = await sendWarningTraffic();
        console.log(`\n⚠️  [tick ${tick}] Warning: ${svc}`);
      } else {
        // Normal traffic
        const count = await sendNormalTraffic();
        totalLogs += count;
      }

      // Print stats every 10 ticks
      if (tick % 10 === 0) {
        await printStats();
      }

    } catch (err) {
      console.error(`\n[ERROR] tick=${tick}: ${err.message}`);
    }

    await sleep(INTERVAL_MS);
  }
}

main().catch(err => {
  console.error('[FATAL]', err.message);
  process.exit(1);
});
