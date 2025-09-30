# Loki & Promtail Multi-Server Logging Setup

---

## 🔹 1. Server A (Central Loki + Promtail + Grafana)

### Install Loki (Docker)

```bash
mkdir -p /home/ubuntu/loki/wal
chmod 777 /home/ubuntu/loki/wal
```

```jsx
docker run -d --name=loki \
  --network monitoring_monitoring \
  -p 127.0.0.1:3100:3100 \
  -v /home/ubuntu/monitoring/loki/loki-config.yaml:/etc/loki/config.yaml \
  -v /home/ubuntu/loki:/loki \
  -v /home/ubuntu/loki/wal:/wal \
  grafana/loki:latest \
  -config.file=/etc/loki/config.yaml
```

```jsx
docker logs -f loki
```

```jsx
curl http://localhost:3100/ready
```

**`loki-config.yaml` example:**

```yaml
auth_enabled: false

server:
  http_listen_port: 3100

ingester:
  lifecycler:
    address: 127.0.0.1
    ring:
      kvstore:
        store: inmemory
      replication_factor: 1
    final_sleep: 0s
  chunk_idle_period: 5m
  max_chunk_age: 1h
  chunk_target_size: 1048576
  chunk_retain_period: 30s

schema_config:
  configs:
    - from: 2022-01-01
      store: boltdb-shipper
      object_store: filesystem
      schema: v12
      index:
        prefix: index_
        period: 24h

storage_config:
  boltdb_shipper:
    active_index_directory: /loki/index
    cache_location: /loki/cache
  filesystem:
    directory: /loki/chunks

compactor:
  working_directory: /loki/compactor
  compaction_interval: 10m
  retention_enabled: true
  retention_delete_delay: 2h
  delete_request_store: filesystem

limits_config:
  ingestion_rate_mb: 10
  ingestion_burst_size_mb: 15
  allow_structured_metadata: false
  retention_period: 168h
```

---

### Install Promtail (to collect local logs)

```bash
docker run -d --name=promtail \
  --network monitoring_monitoring \
  -v /home/ubuntu/.pm2/logs:/home/ubuntu/.pm2/logs:ro \
  -v /home/ubuntu/monitoring/promtail/promtail-config.yaml:/etc/promtail/config.yaml \
  -v /home/ubuntu/promtail-data:/data \
  grafana/promtail:latest \
  -config.file=/etc/promtail/config.yaml
```

**`promtail-config.yaml` example (Server A):**

```yaml
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /data/positions.yaml
  sync_period: 10s

clients:
  - url: http://loki:3100/loki/api/v1/push

scrape_configs:
  # ──────────────── System Logs ────────────────
  - job_name: system
    static_configs:
      - targets: [localhost]
        labels:
          job: varlogs
          app: system
          __path__: /var/log/*log
```

---

```jsx
docker logs -f promtail | grep "batch"
```

### Add Loki as Grafana datasource

1. Open Grafana → `http://<server-ip>:3030`
2. Go to **Configuration → Data sources**
3. Add **Loki**
    - URL: `http://loki:3100`
    - Save & Test → should say *Data source is working* ✅

---

## 🔹 2. Server B (Remote Promtail Only)

On **Server B**, we don’t need Loki, just Promtail.

### Run Promtail

```bash
docker run -d --name=promtail \
  --network monitoring_monitoring \
  -v /home/ubuntu/.pm2/logs:/home/ubuntu/.pm2/logs:ro \
  -v /home/ubuntu/monitoring/promtail/promtail-config.yaml:/etc/promtail/config.yaml \
  -v /home/ubuntu/promtail-data:/data \
  grafana/promtail:latest \
  -config.file=/etc/promtail/config.yaml
```

**`promtail-config.yaml` (Server B):**

```yaml
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /data/positions.yaml
  sync_period: 10s

clients:
  - url: http://loki:3100/loki/api/v1/push

scrape_configs:
  # ──────────────── System Logs ────────────────
  - job_name: system
    static_configs:
      - targets: [localhost]
        labels:
          server: newnext
          job: varlogs
          app: system
          __path__: /var/log/*log

```

🔑 Key point:

- **Server A IP** must be reachable from Server B.
- Labels (`job: server-b`) help you distinguish logs per server in Grafana.

---

## 🔹 3. Testing the setup

### Check Loki readiness

```bash
curl http://<SERVER_A_IP>:3100/ready
```

Should return `ready`.

### Query logs from Server B

```bash
curl "http://<SERVER_A_IP>:3100/loki/api/v1/query_range?query={job=\"server-b\"}&limit=5"
```

### Query in Grafana Explore

In Grafana Explore, run:

```
{job="server-b"}
```

---

## 🔹 4. Grafana Dashboard Queries

- All PM2 logs:
    
    ```
    {app="nodejs"}
    ```
    
- Errors across all servers:
    
    ```
    {stream="stderr"} |= "error"
    ```
    
- Server-specific logs:
    
    ```
    {job="server-b"}
    ```
    

---

✅ With this setup:

- Server A: Central **Loki + Grafana + Promtail**
- Server B: Only **Promtail**, shipping to Loki
- Grafana queries logs from both servers seamlessly

```jsx
{
  "id": null,
  "title": "Multi-Service Logs Dashboard",
  "tags": ["loki", "pm2", "system", "logs"],
  "timezone": "browser",
  "schemaVersion": 36,
  "version": 2,
  "panels": [
    {
      "type": "timeseries",
      "title": "Log Volume (All Services)",
      "targets": [
        {
          "expr": "count_over_time({job=~\".*\"}[5m])",
          "refId": "A"
        }
      ],
      "gridPos": { "x": 0, "y": 0, "w": 12, "h": 6 }
    },
    {
      "type": "timeseries",
      "title": "Error Rate (All Services)",
      "targets": [
        {
          "expr": "count_over_time({stream=\"stderr\"}[5m])",
          "refId": "B"
        }
      ],
      "gridPos": { "x": 12, "y": 0, "w": 12, "h": 6 }
    },
    {
      "type": "table",
      "title": "Recent Errors (PM2 + System)",
      "targets": [
        {
          "expr": "{stream=\"stderr\"} |~ \"ERROR|Error|error\"",
          "refId": "C"
        }
      ],
      "gridPos": { "x": 0, "y": 6, "w": 24, "h": 6 }
    },
    {
      "type": "logs",
      "title": "Live Tail – Chroma DB Logs",
      "targets": [
        {
          "expr": "{job=\"chroma-db\"}",
          "refId": "D"
        }
      ],
      "gridPos": { "x": 0, "y": 12, "w": 12, "h": 6 }
    },
    {
      "type": "logs",
      "title": "Live Tail – Frontend Logs",
      "targets": [
        {
          "expr": "{job=\"frontend\"}",
          "refId": "E"
        }
      ],
      "gridPos": { "x": 12, "y": 12, "w": 12, "h": 6 }
    },
    {
      "type": "logs",
      "title": "Live Tail – Backend Logs",
      "targets": [
        {
          "expr": "{job=\"backend\"}",
          "refId": "F"
        }
      ],
      "gridPos": { "x": 0, "y": 18, "w": 24, "h": 6 }
    },
    {
      "type": "logs",
      "title": "Live Tail – System Logs",
      "targets": [
        {
          "expr": "{job=\"varlogs\"}",
          "refId": "G"
        }
      ],
      "gridPos": { "x": 0, "y": 24, "w": 24, "h": 8 }
    }
  ]
}
```

```jsx
networks:
  monitoring:
    driver: bridge

services:
  node-exporter:
    image: prom/node-exporter:latest
    container_name: node-exporter
    restart: unless-stopped
    ports:
      - "127.0.0.1:9100:9100"
    networks:
      - monitoring
    command:
      - '--path.procfs=/host/proc'
      - '--path.sysfs=/host/sys'
      - '--collector.filesystem.ignored-mount-points=^/(sys|proc|dev|host|etc)($$|/)'
    volumes:
      - /proc:/host/proc:ro
      - /sys:/host/sys:ro
      - /:/rootfs:ro

  nginx-exporter:
    image: nginx/nginx-prometheus-exporter:latest
    container_name: nginx-exporter
    restart: unless-stopped
    ports:
      - "127.0.0.1:9113:9113"
    networks:
      - monitoring
    command:
      - -nginx.scrape-uri=http://127.0.0.1/nginx_status
```

`/etc/nginx/nginx.conf` (or site config), inside `http {}`:

```
server {
    listen 127.0.0.1:8080;
    server_name localhost;

    location /nginx_status {
        stub_status;
        allow 127.0.0.1;
        deny all;
    }
}
```
