const axios = require("axios");

const BASE = "http://localhost:8081/api/v1/logs";

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const log = (msg) => console.log("[SEED]", msg);
const ok = (msg) => console.log("[OK]", msg);
const warn = (msg) => console.log("[WARN]", msg);

// Time helpers
const now = new Date();
const iso = (d) => d.toISOString();
const minus = (mins) => iso(new Date(Date.now() - mins * 60000));

async function waitForService() {
  log("Waiting for log-ingestion-service...");
  for (let i = 0; i < 30; i++) {
    try {
      await axios.get("http://localhost:8081/actuator/health");
      ok("Service is ready!");
      return;
    } catch {
      process.stdout.write(".");
      await sleep(3000);
    }
  }
  warn("Service not ready, continuing...");
}

async function postLog(data) {
  try {
    await axios.post(BASE, data);
  } catch (e) {
    console.error("POST failed:", e.message);
  }
}

async function postBatch(logs) {
  try {
    await axios.post(`${BASE}/batch`, { logs });
  } catch (e) {
    console.error("BATCH failed:", e.message);
  }
}

async function run() {
  await waitForService();

  console.log("\n==============================");
  console.log("Seeding incidents...");
  console.log("==============================\n");

  // INCIDENT 1
  log("Payment ERROR spike...");
  for (let i = 1; i <= 55; i++) {
    await postLog({
      serviceName: "payment-service",
      logLevel: "ERROR",
      message: "PaymentGatewayException: timeout",
      timestamp: iso(now),
      traceId: `trace-pay-${i}`,
      environment: "prod",
    });
  }
  ok("payment-service done");

  // INCIDENT 2
  log("Order DB failure...");
  await postLog({
    serviceName: "order-service",
    logLevel: "ERROR",
    message: "DB connection refused",
    timestamp: minus(1),
    traceId: "trace-ord-1",
    environment: "prod",
  });
  ok("order-service done");

  // INCIDENT 3
  log("Cart Redis timeout...");
  for (let i = 1; i <= 12; i++) {
    await postLog({
      serviceName: "cart-service",
      logLevel: "ERROR",
      message: "Redis timeout",
      timestamp: minus(5),
      traceId: `trace-cart-${i}`,
      environment: "prod",
    });
  }
  ok("cart-service done");

  // INCIDENT 4
  log("Notification log storm...");
  const logs = [];
  for (let i = 1; i <= 20; i++) {
    logs.push({
      serviceName: "notification-service",
      logLevel: "DEBUG",
      message: `Processing job ${i}`,
      timestamp: minus(5),
      environment: "prod",
    });
  }
  await postBatch(logs);
  ok("notification-service done");

  // INCIDENT 5
  await postLog({
    serviceName: "inventory-service",
    logLevel: "ERROR",
    message: "Upstream 503",
    timestamp: minus(5),
    traceId: "trace-inv-1",
    environment: "prod",
  });

  // INCIDENT 6
  for (let i = 1; i <= 11; i++) {
    await postLog({
      serviceName: "auth-service",
      logLevel: "WARN",
      message: "JWT verification failed",
      timestamp: minus(15),
      traceId: `trace-auth-${i}`,
      environment: "prod",
    });
  }

  // INCIDENT 7
  await postLog({
    serviceName: "search-service",
    logLevel: "ERROR",
    message: "Circuit breaker open",
    timestamp: minus(15),
    traceId: "trace-srch-1",
    environment: "prod",
  });

  // INCIDENT 8
  for (let i = 1; i <= 52; i++) {
    await postLog({
      serviceName: "pricing-service",
      logLevel: "ERROR",
      message: "Null pointer",
      timestamp: minus(30),
      traceId: `trace-price-${i}`,
      environment: "prod",
    });
  }

  // INCIDENT 9
  await postLog({
    serviceName: "shipping-service",
    logLevel: "ERROR",
    message: "Host unreachable",
    timestamp: minus(30),
    traceId: "trace-ship-1",
    environment: "prod",
  });

  // INCIDENT 10
  await postBatch([
    {
      serviceName: "api-gateway",
      logLevel: "INFO",
      message: "GET /products 200",
      timestamp: minus(60),
      environment: "prod",
    },
  ]);

  for (let i = 1; i <= 53; i++) {
    await postLog({
      serviceName: "api-gateway",
      logLevel: "ERROR",
      message: "502 Bad Gateway",
      timestamp: minus(60),
      traceId: `trace-gw-${i}`,
      environment: "prod",
    });
  }

  console.log("\n✅ Seeding complete!");
  console.log("👉 Open http://localhost:3000\n");
}

run();