# 🏗️ AI-Powered Log Intelligence Platform

Production-grade event-driven microservices: log ingestion → anomaly detection → AI root cause analysis → alerting → React dashboard.

---

## Architecture

```
┌──────────────────────────────────────────────────────┐
│            React Dashboard  :3000                    │
│  Dashboard · Incidents · Anomalies · Logs · Alerts  │
└───────────────────────┬──────────────────────────────┘
                        │ REST
                        ▼
┌──────────────────────────────────────────────────────┐
│         incident-query-service  :8085                │
│  Materialized view · Search · Dashboard stats API    │
└───────┬──────────────────┬──────────────────┬────────┘
        │ REST             │ REST             │ REST
        ▼                  ▼                  ▼
 anomaly-detector     rca-engine         alert-service
    :8082               :8083              :8084
                        │
                 Anthropic Claude API
        ▲                  ▲                  ▲
        └──────────────────┴──────────────────┘
                  Kafka: anomaly-events
                        ▲
┌──────────────────────────────────────────────────────┐
│         log-ingestion-service  :8081                 │
│  REST · Batch · Rate-limited · Redis cache           │
└──────────────────────────────────────────────────────┘
         Kafka: raw-logs (6p)  +  error-logs (3p)

  Infra: PostgreSQL · Redis · Kafka · Zookeeper · Kafka-UI :8090
```

---

## Services

| Service | Port | Responsibility |
|---|---|---|
| `log-ingestion-service` | 8081 | REST + batch log ingest → Kafka + PostgreSQL |
| `anomaly-detector-service` | 8082 | 4 detection strategies, Redis sliding windows |
| `rca-engine-service` | 8083 | Rule-based + AI (Claude) RCA reports |
| `alert-service` | 8084 | Slack / Email with retry scheduler |
| `incident-query-service` | 8085 | Unified search, dashboard stats, materialized view |
| `dashboard` | 3000 | React SPA with charts, incident table, RCA viewer |
| `kafka-ui` | 8090 | Kafka topic browser |

---

## Quick Start

```bash
# Start everything (first run ~3-4 min for Maven builds)
docker-compose up -d

# Open dashboard
open http://localhost:3000

# Verify all services healthy
for p in 8081 8082 8083 8084 8085; do
  echo -n "Port $p: " && curl -sf http://localhost:$p/actuator/health | python3 -c "import sys,json; print(json.load(sys.stdin)['status'])"
done
```

---

## Trigger an Anomaly (Demo)

Send 55 errors to trip the `ErrorRateSpikeStrategy` (threshold: 50 per 60s):

```bash
for i in $(seq 1 55); do
  curl -s -X POST http://localhost:8081/api/v1/logs \
    -H "Content-Type: application/json" \
    -d "{\"serviceName\":\"order-service\",\"logLevel\":\"ERROR\",
         \"message\":\"DB connection pool exhausted\",
         \"timestamp\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",
         \"environment\":\"prod\"}" > /dev/null
done

# Check anomaly was detected
curl http://localhost:8082/api/v1/anomalies?status=OPEN

# Check RCA was generated
curl http://localhost:8083/api/v1/rca

# Check incident appears in dashboard
curl http://localhost:8085/api/v1/incidents/dashboard
```

---

## API Reference

### log-ingestion-service (:8081)
| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/logs` | Ingest single log |
| POST | `/api/v1/logs/batch` | Batch ingest (max 1000) |
| GET | `/api/v1/logs` | Query (`?serviceName=&logLevel=&page=&size=`) |
| GET | `/api/v1/logs/trace/{traceId}` | All logs for a distributed trace |
| GET | `/api/v1/logs/stats` | Ingestion statistics |

### anomaly-detector-service (:8082)
| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/anomalies` | List (`?status=&severity=&serviceName=`) |
| PATCH | `/api/v1/anomalies/{id}/status` | Update status |
| GET | `/api/v1/anomalies/stats` | Stats for dashboard |

### rca-engine-service (:8083)
| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/rca` | All RCA reports |
| GET | `/api/v1/rca/anomaly/{anomalyId}` | RCA for specific anomaly |
| GET | `/api/v1/rca/pending` | In-progress reports |

### alert-service (:8084)
| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/alerts` | List alerts (`?status=&serviceName=`) |
| POST | `/api/v1/alerts/{id}/acknowledge` | Acknowledge alert |

### incident-query-service (:8085)
| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/incidents/dashboard` | **Full dashboard stats** |
| GET | `/api/v1/incidents` | Search (`?q=&status=&severity=&serviceName=`) |
| GET | `/api/v1/incidents/{id}` | Full incident + RCA detail |
| PATCH | `/api/v1/incidents/{id}/status` | Update status |

---

## Anomaly Detection Strategies

| Strategy | Trigger | Severity |
|---|---|---|
| `ErrorRateSpikeStrategy` | ≥50 ERRORs/service in 60s | HIGH/CRITICAL |
| `RepeatedErrorPatternStrategy` | Same fingerprint ≥10x in 120s | MEDIUM |
| `LogVolumeSpikeStrategy` | ≥500 logs in 30s | MEDIUM |
| `ServiceUnavailableStrategy` | "connection refused", "503", "circuit breaker open" | CRITICAL |

All thresholds configurable via `application.yml` or env vars.

---

## Optional Integrations

### AI-Powered RCA (Anthropic Claude)
```bash
export ANTHROPIC_API_KEY=sk-ant-...
export ANTHROPIC_AI_ENABLED=true
docker-compose up -d rca-engine-service
```

### Slack Alerts
```bash
export SLACK_ENABLED=true
export SLACK_WEBHOOK_URL=https://hooks.slack.com/services/YOUR/WEBHOOK
docker-compose up -d alert-service
```

---

## Design Patterns Used

| Pattern | Where |
|---|---|
| Strategy | Anomaly detection — 4 pluggable `AnomalyDetectionStrategy` implementations |
| Choreography Saga | Kafka chain: `raw-logs` → `error-logs` → `anomaly-events` |
| Sliding Window | Redis TTL counters for rate-based anomaly detection |
| Idempotent Consumer | One RCA + one Alert per anomaly (dedup checks) |
| Materialized View | `incident-query-service` aggregates all data locally for fast reads |
| CQRS (lite) | Separate read/write paths in ingestion; stats cached in Redis |
| Circuit Breaker | Resilience4j rate limiter (10k req/s) on ingestion |
| Bulkhead | Separate Kafka consumer groups per concern |
