# ============================================================
# pom.xml CHANGES for rca-engine-service
# ============================================================

# REMOVE these dependencies (Anthropic SDK):
# -------------------------------------------
# <dependency>
#   <groupId>com.anthropic</groupId>
#   <artifactId>sdk</artifactId>
#   ...
# </dependency>

# ADD these dependencies:
# -------------------------------------------

# 1. RestTemplate support (already in spring-boot-starter-web, but make explicit):
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

# 2. Micrometer for provider metrics (already in most Spring Boot projects):
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
</dependency>

# 3. (Optional) Prometheus export:
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <scope>runtime</scope>
</dependency>

# That's it — no Gemini SDK needed.
# Gemini and Ollama are called via plain REST (RestTemplate).
# This keeps zero new transitive dependencies.


# ============================================================
# RcaReport entity — add providerUsed column migration
# ============================================================
# If you use Flyway/Liquibase, add this migration:
# V4__add_provider_used_to_rca_report.sql
#
#   ALTER TABLE rca_report ADD COLUMN IF NOT EXISTS provider_used VARCHAR(50);
#   ALTER TABLE rca_report ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP;
#
# If you use ddl-auto: update, JPA will add it automatically.


# ============================================================
# docker-compose.yml — ADD Ollama service (optional)
# ============================================================
# Add this to your existing docker-compose.yml services block:

  ollama:
    image: ollama/ollama:latest
    container_name: logsense-ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama
    networks:
      - logsense-network
    # Pull a model on first run (choose one):
    # Small & fast (recommended for dev):  llama3.2 (~2GB), mistral (~4GB)
    # Better quality (needs 8GB+ RAM):     llama3.1:8b, qwen2.5:7b
    entrypoint: ["/bin/sh", "-c", "ollama serve & sleep 5 && ollama pull llama3.2 && wait"]
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:11434/api/tags"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 120s   # allow time for model download on first run

# Add to volumes section:
volumes:
  ollama_data:


# ============================================================
# Environment variables summary
# ============================================================
#
# Option A — Gemini only (recommended for cloud/CI):
#   GEMINI_ENABLED=true
#   GEMINI_API_KEY=AIza...           # from aistudio.google.com (free, no card)
#
# Option B — Ollama only (recommended for air-gapped/local):
#   OLLAMA_ENABLED=true
#   OLLAMA_BASE_URL=http://ollama:11434   # Docker service name
#   OLLAMA_MODEL=llama3.2
#
# Option C — Both (full chain, recommended for production demo):
#   GEMINI_ENABLED=true
#   GEMINI_API_KEY=AIza...
#   OLLAMA_ENABLED=true
#   OLLAMA_BASE_URL=http://ollama:11434
#
# In all cases, RuleBased fires automatically if both above fail.
# No ANTHROPIC_API_KEY needed anywhere.
