$lines = @(
    "server {",
    "    listen 3000;",
    "    root /usr/share/nginx/html;",
    "    index index.html;",
    "    location / { try_files `$uri `$uri/ /index.html; }",
    "    location /api/v1/incidents { proxy_pass http://incident-query-service:8085; }",
    "    location /api/v1/anomalies { proxy_pass http://anomaly-detector-service:8082; }",
    "    location /api/v1/logs { proxy_pass http://log-ingestion-service:8081; }",
    "    location /api/v1/alerts { proxy_pass http://alert-service:8084; }",
    "    location /api/v1/rca { proxy_pass http://rca-engine-service:8083; }",
    "    gzip on;",
    "    gzip_types text/plain text/css application/json application/javascript;",
    "}"
)

[System.IO.File]::WriteAllLines("$PWD\nginx.conf", $lines, [System.Text.UTF8Encoding]::new($false))
Write-Host "nginx.conf written (no BOM)"
