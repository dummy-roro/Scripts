# Step-by-Step Guide: Monitoring Setup with Prometheus & Grafana

## Prerequisites

1. Two Ubuntu 22.04 LTS servers with sudo access.
2. Firewall open for ports: 9090 (Prometheus), 3000 (Grafana), 9115 (Blackbox), 80/443 (Nginx on app server).
3. Update packages: `sudo apt update && sudo apt upgrade -y`.
4. Install tools: `sudo apt install wget tar curl -y`.
5. Nginx installed on the application server (configured later).

## Step 1: Set Up the Monitoring Server

### 1.1 Install Prometheus (Version: 3.5.0)

1. Download and extract (verify at https://prometheus.io/download/):
    
    ```
    wget https://github.com/prometheus/prometheus/releases/download/v3.5.0/prometheus-3.5.0.linux-amd64.tar.gz
    tar xvfz prometheus-3.5.0.linux-amd64.tar.gz
    sudo mv prometheus-3.5.0.linux-amd64 /opt/prometheus
    
    ```
    
2. Create a user:
    
    ```
    sudo useradd --no-create-home --shell /bin/false prometheus
    sudo chown -R prometheus:prometheus /opt/prometheus
    
    ```
    
3. Create a data directory:
    
    ```
    sudo mkdir /opt/prometheus/data
    sudo chown -R prometheus:prometheus /opt/prometheus/data
    
    ```
    
4. Create a configuration file at `/etc/prometheus/prometheus.yml`:
    
    ```
    global:
      scrape_interval: 15s
      evaluation_interval: 15s
    
    scrape_configs:
      - job_name: 'prometheus'
        static_configs:
          - targets: ['localhost:9090']
    
    ```
    
5. Create a systemd service at `/etc/systemd/system/prometheus.service`:
    
    ```
    [Unit]
    Description=Prometheus
    After=network.target
    
    [Service]
    User=prometheus
    Group=prometheus
    Type=simple
    ExecStart=/opt/prometheus/prometheus \
      --config.file=/etc/prometheus/prometheus.yml \
      --storage.tsdb.path=/opt/prometheus/data
    Restart=always
    
    [Install]
    WantedBy=multi-user.target
    
    ```
    
6. Start and enable:
    
    ```
    sudo systemctl daemon-reload
    sudo systemctl enable prometheus
    sudo systemctl start prometheus
    
    ```
    
7. Verify: Access `http://monitoring-server-ip:9090`.

### 1.2 Install Grafana (Version: 12.1.1)

1. Add Grafana repository:
    
    ```
    sudo apt install -y software-properties-common
    wget -q -O- https://apt.grafana.com/gpg.key | sudo gpg --dearmor -o /usr/share/keyrings/grafana-archive-keyring.gpg
    echo "deb [signed-by=/usr/share/keyrings/grafana-archive-keyring.gpg] https://apt.grafana.com stable main" | sudo tee /etc/apt/sources.list.d/grafana.list
    sudo apt update
    
    ```
    
2. Install Grafana:
    
    ```
    sudo apt install grafana=12.1.1 -y
    
    ```
    
3. Start and enable:
    
    ```
    sudo systemctl enable grafana-server
    sudo systemctl start grafana-server
    
    ```
    
4. Verify: Access `http://monitoring-server-ip:3000`. Default login: admin/admin (change password).

### 1.3 Install Blackbox Exporter (Version: 0.27.0)

1. Download and extract:
    
    ```
    wget https://github.com/prometheus/blackbox_exporter/releases/download/v0.27.0/blackbox_exporter-0.27.0.linux-amd64.tar.gz
    tar xvfz blackbox_exporter-0.27.0.linux-amd64.tar.gz
    sudo mv blackbox_exporter-0.27.0.linux-amd64/blackbox_exporter /usr/local/bin/
    
    ```
    
2. Create a user:
    
    ```
    sudo useradd --no-create-home --shell /bin/false blackbox
    
    ```
    
3. Create a config file at `/etc/prometheus/blackbox.yml`:
    
    ```
    modules:
      http_2xx:
        prober: http
        http:
          method: GET
      icmp:
        prober: icmp
    
    ```
    
4. Create a systemd service at `/etc/systemd/system/blackbox.service`:
    
    ```
    [Unit]
    Description=Blackbox Exporter
    After=network.target
    
    [Service]
    User=blackbox
    Group=blackbox
    Type=simple
    ExecStart=/usr/local/bin/blackbox_exporter --config.file=/etc/prometheus/blackbox.yml
    Restart=always
    
    [Install]
    WantedBy=multi-user.target
    
    ```
    
5. Start and enable:
    
    ```
    sudo systemctl daemon-reload
    sudo systemctl enable blackbox
    sudo systemctl start blackbox
    
    ```
    
6. Verify: `curl http://localhost:9115/metrics`.

## Step 2: Set Up the Application Server

### 2.1 Install Node Exporter (Version: 1.9.1)

1. Download and extract:
    
    ```
    wget https://github.com/prometheus/node_exporter/releases/download/v1.9.1/node_exporter-1.9.1.linux-amd64.tar.gz
    tar xvfz node_exporter-1.9.1.linux-amd64.tar.gz
    sudo mv node_exporter-1.9.1.linux-amd64/node_exporter /usr/local/bin/
    ```
    
2. Create a user:
    
    ```
    sudo useradd --no-create-home --shell /bin/false node_exporter
    ```
    
3. Create a systemd service at `/etc/systemd/system/node_exporter.service`:
    
    ```
    [Unit]
    Description=Node Exporter
    After=network.target
    
    [Service]
    User=node_exporter
    Group=node_exporter
    Type=simple
    ExecStart=/usr/local/bin/node_exporter
    Restart=always
    
    [Install]
    WantedBy=multi-user.target
    
    ```
    
4. Start and enable:
    
    ```
    sudo systemctl daemon-reload
    sudo systemctl enable node_exporter
    sudo systemctl start node_exporter
    ```
    
5. Verify: `curl http://localhost:9100/metrics`.

### 2.2 Install Nginx Prometheus Exporter (Version: 1.4.2)

1. Download and extract:
    
    ```
    wget https://github.com/nginxinc/nginx-prometheus-exporter/releases/download/v1.4.2/nginx-prometheus-exporter_1.4.2_linux_amd64.tar.gz
    tar xvfz nginx-prometheus-exporter_1.4.2_linux_amd64.tar.gz
    sudo mv nginx-prometheus-exporter /usr/local/bin/
    
    ```
    
2. Create a user:
    
    ```
    sudo useradd --no-create-home --shell /bin/false nginx_exporter
    
    ```
    
3. Enable Nginx stub_status (edit `/etc/nginx/sites-enabled/default` or your app config):
    
    ```
    server {
        listen 127.0.0.1:8081;
    
        location /stub_status {
            stub_status;
            allow 127.0.0.1;
            deny all;
        }
    }
    ```
    
    Reload Nginx: `sudo nginx -t && sudo systemctl reload nginx`.
    
4. Create a systemd service at `/etc/systemd/system/nginx_exporter.service`:
    
    ```
    [Unit]
    Description=Nginx Exporter
    After=network.target
    
    [Service]
    User=nginx_exporter
    Group=nginx_exporter
    Type=simple
    ExecStart=/usr/local/bin/nginx-prometheus-exporter -nginx.scrape-uri=http://localhost/stub_status
    Restart=always
    
    [Install]
    WantedBy=multi-user.target
    
    ```
    
5. Start and enable:
    
    ```
    sudo systemctl daemon-reload
    sudo systemctl enable nginx_exporter
    sudo systemctl start nginx_exporter
    
    ```
    
6. Verify: `curl http://localhost:9113/metrics`.

### 2.3 Configure Nginx as Reverse Proxy

1. Install Nginx if not already: `sudo apt install nginx -y`.
2. Generate self-signed SSL cert (use Let’s Encrypt for production):
    
    ```
    sudo openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout /etc/ssl/private/nginx-selfsigned.key -out /etc/ssl/certs/nginx-selfsigned.crt
    sudo openssl dhparam -out /etc/ssl/certs/dhparam.pem 2048
    
    ```
    
3. Install Apache utils for basic auth: `sudo apt install apache2-utils -y`.
4. Create a basic auth user (username: monitor):
    
    ```
    sudo htpasswd -c /etc/nginx/.htpasswd monitor
    
    ```
    
5. Edit Nginx site config (e.g., `/etc/nginx/sites-enabled/default`):
    
    ```
    server {
      listen 80;
      server_name app-domain.com;
      return 301 https://$host$request_uri;
    }
    
    server {
      listen 443 ssl;
      server_name app-domain.com;
    
      ssl_certificate /etc/ssl/certs/nginx-selfsigned.crt;
      ssl_certificate_key /etc/ssl/private/nginx-selfsigned.key;
      ssl_dhparam /etc/ssl/certs/dhparam.pem;
    
      # Your existing app locations...
    
      location /metric/node {
        auth_basic "Protected Metrics";
        auth_basic_user_file /etc/nginx/.htpasswd;
        proxy_pass http://localhost:9100/metrics;
        proxy_set_header Host $host;
      }
    
      location /metric/nginx {
        auth_basic "Protected Metrics";
        auth_basic_user_file /etc/nginx/.htpasswd;
        proxy_pass http://localhost:9113/metrics;
        proxy_set_header Host $host;
      }
    }
    
    ```
    
6. Test and reload: `sudo nginx -t && sudo systemctl reload nginx`.
7. Verify: `curl -u monitor:yourpassword https://app-domain.com/metric/node`.

## Step 3: Configure Prometheus to Scrape Metrics

Edit `/etc/prometheus/prometheus.yml` on the monitoring server:

```
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  - job_name: 'node'
    metrics_path: /metric/node
    scheme: https
    basic_auth:
      username: monitor
      password: yourpassword
    static_configs:
      - targets: ['app-domain.com']

  - job_name: 'nginx'
    metrics_path: /metric/nginx
    scheme: https
    basic_auth:
      username: monitor
      password: yourpassword
    static_configs:
      - targets: ['app-domain.com']

  - job_name: 'blackbox'
    metrics_path: /probe
    params:
      module: [http_2xx]
    static_configs:
      - targets:
        - https://app-domain.com
    relabel_configs:
      - source_labels: [__address__]
        target_label: __param_target
      - source_labels: [__param_target]
        target_label: instance
      - target_label: __address__
        replacement: localhost:9115

```

Reload Prometheus: `sudo systemctl restart prometheus`.

Verify in Prometheus UI: Check Targets page at `http://monitoring-server-ip:9090/targets`; all should be "UP".

## Step 4: Configure Grafana

1. Log in to Grafana at `http://monitoring-server-ip:3000`.
2. Add Prometheus data source: Sidebar > Connections > Data sources > Add > Prometheus. URL: `http://localhost:9090`.
3. Import dashboards:
    - Node Exporter: ID 1860 (https://grafana.com/grafana/dashboards/1860).
    - Nginx: ID 12705.
    - Blackbox: ID 9965. Go to Dashboards > Import, enter ID, select Prometheus data source.

## Step 5: Testing and Next Steps

- Query metrics in Prometheus UI (e.g., `up`).
- View Grafana dashboards.
- Add alerts in Prometheus (create `rules.yml`, add to `prometheus.yml` under `rule_files`).
- For production: Use Let’s Encrypt for SSL, secure Grafana/Prometheus with reverse proxies, enable persistence, and expand Blackbox probes.

Check logs for issues: `journalctl -u prometheus`, etc. This setup ensures secure metric exposure via your app’s domain with the latest versions: Prometheus 3.5.0, Grafana 12.1.1, Node Exporter 1.9.1, Nginx Exporter 1.4.2, Blackbox Exporter 0.27.0.

# Step-by-Step Guide: Monitoring Setup with Prometheus & Grafana

## Prerequisites

1. Two Ubuntu 22.04 LTS servers with sudo access.
2. Firewall open for ports: 9090 (Prometheus), 3000 (Grafana), 9115 (Blackbox), 80/443 (Nginx on app server).
3. Update packages: `sudo apt update && sudo apt upgrade -y`.
4. Install tools: `sudo apt install wget tar curl -y`.
5. Nginx installed on the application server (configured later).

## Step 1: Set Up the Monitoring Server

### 1.1 Install Prometheus (Version: 3.5.0)

1. Download and extract (verify at https://prometheus.io/download/):
    
    ```
    wget https://github.com/prometheus/prometheus/releases/download/v3.5.0/prometheus-3.5.0.linux-amd64.tar.gz
    tar xvfz prometheus-3.5.0.linux-amd64.tar.gz
    sudo mv prometheus-3.5.0.linux-amd64 /opt/prometheus
    
    ```
    
2. Create a user:
    
    ```
    sudo useradd --no-create-home --shell /bin/false prometheus
    sudo chown -R prometheus:prometheus /opt/prometheus
    
    ```
    
3. Create a data directory:
    
    ```
    sudo mkdir /opt/prometheus/data
    sudo chown -R prometheus:prometheus /opt/prometheus/data
    
    ```
    
4. Create a configuration file at `/etc/prometheus/prometheus.yml`:
    
    ```
    global:
      scrape_interval: 15s
      evaluation_interval: 15s
    
    scrape_configs:
      - job_name: 'prometheus'
        static_configs:
          - targets: ['localhost:9090']
    
    ```
    
5. Create a systemd service at `/etc/systemd/system/prometheus.service`:
    
    ```
    [Unit]
    Description=Prometheus
    After=network.target
    
    [Service]
    User=prometheus
    Group=prometheus
    Type=simple
    ExecStart=/opt/prometheus/prometheus \
      --config.file=/etc/prometheus/prometheus.yml \
      --storage.tsdb.path=/opt/prometheus/data
    Restart=always
    
    [Install]
    WantedBy=multi-user.target
    
    ```
    
6. Start and enable:
    
    ```
    sudo systemctl daemon-reload
    sudo systemctl enable prometheus
    sudo systemctl start prometheus
    
    ```
    
7. Verify: Access `http://monitoring-server-ip:9090`.

### 1.2 Install Grafana (Version: 12.1.1)

1. Add Grafana repository:
    
    ```
    sudo apt install -y software-properties-common
    wget -q -O- https://apt.grafana.com/gpg.key | sudo gpg --dearmor -o /usr/share/keyrings/grafana-archive-keyring.gpg
    echo "deb [signed-by=/usr/share/keyrings/grafana-archive-keyring.gpg] https://apt.grafana.com stable main" | sudo tee /etc/apt/sources.list.d/grafana.list
    sudo apt update
    
    ```
    
2. Install Grafana:
    
    ```
    sudo apt install grafana=12.1.1 -y
    
    ```
    
3. Start and enable:
    
    ```
    sudo systemctl enable grafana-server
    sudo systemctl start grafana-server
    
    ```
    
4. Verify: Access `http://monitoring-server-ip:3000`. Default login: admin/admin (change password).

### 1.3 Install Blackbox Exporter (Version: 0.27.0)

1. Download and extract:
    
    ```
    wget https://github.com/prometheus/blackbox_exporter/releases/download/v0.27.0/blackbox_exporter-0.27.0.linux-amd64.tar.gz
    tar xvfz blackbox_exporter-0.27.0.linux-amd64.tar.gz
    sudo mv blackbox_exporter-0.27.0.linux-amd64/blackbox_exporter /usr/local/bin/
    
    ```
    
2. Create a user:
    
    ```
    sudo useradd --no-create-home --shell /bin/false blackbox
    
    ```
    
3. Create a config file at `/etc/prometheus/blackbox.yml`:
    
    ```
    modules:
      http_2xx:
        prober: http
        http:
          method: GET
      icmp:
        prober: icmp
    
    ```
    
4. Create a systemd service at `/etc/systemd/system/blackbox.service`:
    
    ```
    [Unit]
    Description=Blackbox Exporter
    After=network.target
    
    [Service]
    User=blackbox
    Group=blackbox
    Type=simple
    ExecStart=/usr/local/bin/blackbox_exporter --config.file=/etc/prometheus/blackbox.yml
    Restart=always
    
    [Install]
    WantedBy=multi-user.target
    
    ```
    
5. Start and enable:
    
    ```
    sudo systemctl daemon-reload
    sudo systemctl enable blackbox
    sudo systemctl start blackbox
    
    ```
    
6. Verify: `curl http://localhost:9115/metrics`.

## Step 2: Set Up the Application Server

### 2.1 Install Node Exporter (Version: 1.9.1)

1. Download and extract:
    
    ```
    wget https://github.com/prometheus/node_exporter/releases/download/v1.9.1/node_exporter-1.9.1.linux-amd64.tar.gz
    tar xvfz node_exporter-1.9.1.linux-amd64.tar.gz
    sudo mv node_exporter-1.9.1.linux-amd64/node_exporter /usr/local/bin/
    
    ```
    
2. Create a user:
    
    ```
    sudo useradd --no-create-home --shell /bin/false node_exporter
    
    ```
    
3. Create a systemd service at `/etc/systemd/system/node_exporter.service`:
    
    ```
    [Unit]
    Description=Node Exporter
    After=network.target
    
    [Service]
    User=node_exporter
    Group=node_exporter
    Type=simple
    ExecStart=/usr/local/bin/node_exporter
    Restart=always
    
    [Install]
    WantedBy=multi-user.target
    
    ```
    
4. Start and enable:
    
    ```
    sudo systemctl daemon-reload
    sudo systemctl enable node_exporter
    sudo systemctl start node_exporter
    
    ```
    
5. Verify: `curl http://localhost:9100/metrics`.

### 2.2 Install Nginx Prometheus Exporter (Version: 1.4.2)

1. Download and extract:
    
    ```
    wget https://github.com/nginxinc/nginx-prometheus-exporter/releases/download/v1.4.2/nginx-prometheus-exporter_1.4.2_linux_amd64.tar.gz
    tar xvfz nginx-prometheus-exporter_1.4.2_linux_amd64.tar.gz
    sudo mv nginx-prometheus-exporter /usr/local/bin/
    
    ```
    
2. Create a user:
    
    ```
    sudo useradd --no-create-home --shell /bin/false nginx_exporter
    
    ```
    
3. Enable Nginx stub_status (edit `/etc/nginx/sites-enabled/default` or your app config):
    
    ```
    server {
      ...
      location /stub_status {
        stub_status;
      }
    }
    
    ```
    
    Reload Nginx: `sudo nginx -t && sudo systemctl reload nginx`.
    
4. Create a systemd service at `/etc/systemd/system/nginx_exporter.service`:
    
    ```
    [Unit]
    Description=Nginx Exporter
    After=network.target
    
    [Service]
    User=nginx_exporter
    Group=nginx_exporter
    Type=simple
    ExecStart=/usr/local/bin/nginx-prometheus-exporter -nginx.scrape-uri=http://localhost/stub_status
    Restart=always
    
    [Install]
    WantedBy=multi-user.target
    
    ```
    
5. Start and enable:
    
    ```
    sudo systemctl daemon-reload
    sudo systemctl enable nginx_exporter
    sudo systemctl start nginx_exporter
    
    ```
    
6. Verify: `curl http://localhost:9113/metrics`.

### 2.3 Configure Nginx as Reverse Proxy

1. Install Nginx if not already: `sudo apt install nginx -y`.
2. Generate self-signed SSL cert (use Let’s Encrypt for production):
    
    ```
    sudo openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout /etc/ssl/private/nginx-selfsigned.key -out /etc/ssl/certs/nginx-selfsigned.crt
    sudo openssl dhparam -out /etc/ssl/certs/dhparam.pem 2048
    
    ```
    
3. Install Apache utils for basic auth: `sudo apt install apache2-utils -y`.
4. Create a basic auth user (username: monitor):
    
    ```
    sudo htpasswd -c /etc/nginx/.htpasswd monitor
    
    ```
    
5. Edit Nginx site config (e.g., `/etc/nginx/sites-enabled/default`):
    
    ```
    server {
      listen 80;
      server_name app-domain.com;
      return 301 https://$host$request_uri;
    }
    
    server {
      listen 443 ssl;
      server_name app-domain.com;
    
      ssl_certificate /etc/ssl/certs/nginx-selfsigned.crt;
      ssl_certificate_key /etc/ssl/private/nginx-selfsigned.key;
      ssl_dhparam /etc/ssl/certs/dhparam.pem;
    
      # Your existing app locations...
    
      location /metric/node {
        auth_basic "Protected Metrics";
        auth_basic_user_file /etc/nginx/.htpasswd;
        proxy_pass http://localhost:9100/metrics;
        proxy_set_header Host $host;
      }
    
      location /metric/nginx {
        auth_basic "Protected Metrics";
        auth_basic_user_file /etc/nginx/.htpasswd;
        proxy_pass http://localhost:9113/metrics;
        proxy_set_header Host $host;
      }
    }
    
    ```
    
6. Test and reload: `sudo nginx -t && sudo systemctl reload nginx`.
7. Verify: `curl -u monitor:yourpassword https://app-domain.com/metric/node`.

## Step 3: Configure Prometheus to Scrape Metrics

Edit `/etc/prometheus/prometheus.yml` on the monitoring server:

```
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  - job_name: 'node'
    metrics_path: /metric/node
    scheme: https
    basic_auth:
      username: monitor
      password: yourpassword
    static_configs:
      - targets: ['app-domain.com']

  - job_name: 'nginx'
    metrics_path: /metric/nginx
    scheme: https
    basic_auth:
      username: monitor
      password: yourpassword
    static_configs:
      - targets: ['app-domain.com']

  - job_name: 'blackbox'
    metrics_path: /probe
    params:
      module: [http_2xx]
    static_configs:
      - targets:
        - https://app-domain.com
    relabel_configs:
      - source_labels: [__address__]
        target_label: __param_target
      - source_labels: [__param_target]
        target_label: instance
      - target_label: __address__
        replacement: localhost:9115

```

Reload Prometheus: `sudo systemctl restart prometheus`.

Verify in Prometheus UI: Check Targets page at `http://monitoring-server-ip:9090/targets`; all should be "UP".

## Step 4: Configure Grafana

1. Log in to Grafana at `http://monitoring-server-ip:3000`.
2. Add Prometheus data source: Sidebar > Connections > Data sources > Add > Prometheus. URL: `http://localhost:9090`.
3. Import dashboards:
    - Node Exporter: ID 1860 (https://grafana.com/grafana/dashboards/1860).
    - Nginx: ID 12705.
    - Blackbox: ID 9965. Go to Dashboards > Import, enter ID, select Prometheus data source.

## Step 5: Testing and Next Steps

- Query metrics in Prometheus UI (e.g., `up`).
- View Grafana dashboards.
- Add alerts in Prometheus (create `rules.yml`, add to `prometheus.yml` under `rule_files`).
- For production: Use Let’s Encrypt for SSL, secure Grafana/Prometheus with reverse proxies, enable persistence, and expand Blackbox probes.

Check logs for issues: `journalctl -u prometheus`, etc. This setup ensures secure metric exposure via your app’s domain with the latest versions: Prometheus 3.5.0, Grafana 12.1.1, Node Exporter 1.9.1, Nginx Exporter 1.4.2, Blackbox Exporter 0.27.0.

## Install **Node Exporter** (v1.9.1)

```bash
wget https://github.com/prometheus/node_exporter/releases/download/v1.9.1/node_exporter-1.9.1.linux-amd64.tar.gz
tar xvfz node_exporter-1.9.1.linux-amd64.tar.gz
sudo mv node_exporter-1.9.1.linux-amd64/node_exporter /usr/local/bin/

```

Create unprivileged user:

```bash
sudo useradd --no-create-home --shell /bin/false node_exporter

```

Systemd service `/etc/systemd/system/node_exporter.service`:

```
[Unit]
Description=Prometheus Node Exporter
After=network.target

[Service]
User=node_exporter
Group=node_exporter
Type=simple
ExecStart=/usr/local/bin/node_exporter --web.listen-address=127.0.0.1:9100
Restart=always

# Hardening
NoNewPrivileges=true
ProtectSystem=strict
ProtectHome=true
PrivateTmp=true
ProtectControlGroups=true
ProtectKernelTunables=true
ProtectKernelModules=true
LockPersonality=true
MemoryDenyWriteExecute=true

[Install]
WantedBy=multi-user.target

```

Start:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now node_exporter

```

Verify:

```bash
curl http://localhost:9100/metrics

```

---

## 2. Install **NGINX Prometheus Exporter** (v1.4.2)

```bash
wget https://github.com/nginxinc/nginx-prometheus-exporter/releases/download/v1.4.2/nginx-prometheus-exporter_1.4.2_linux_amd64.tar.gz
tar xvfz nginx-prometheus-exporter_1.4.2_linux_amd64.tar.gz
sudo mv nginx-prometheus-exporter /usr/local/bin/

```

Create unprivileged user:

```bash
sudo useradd --no-create-home --shell /bin/false nginx_exporter

```

Enable NGINX stub_status (`/etc/nginx/sites-enabled/default` or app config):

```
location /stub_status {
    stub_status;
    allow 127.0.0.1;
    deny all;
}

```

Reload:

```bash
sudo nginx -t && sudo systemctl reload nginx

```

Systemd service `/etc/systemd/system/nginx_exporter.service`:

```
[Unit]
Description=NGINX Prometheus Exporter
After=network.target

[Service]
User=nginx_exporter
Group=nginx_exporter
Type=simple
ExecStart=/usr/local/bin/nginx-prometheus-exporter \
  -nginx.scrape-uri=http://127.0.0.1/stub_status \
  -web.listen-address=127.0.0.1:9113
Restart=always

# Hardening
NoNewPrivileges=true
ProtectSystem=strict
ProtectHome=true
PrivateTmp=true
ProtectControlGroups=true
ProtectKernelTunables=true
ProtectKernelModules=true
LockPersonality=true
MemoryDenyWriteExecute=true

[Install]
WantedBy=multi-user.target

```

Start:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now nginx_exporter

```

Verify:

```bash
curl http://localhost:9113/metrics

```

---

## 3. Secure Reverse Proxy with **NGINX + HTTPS + Basic Auth**

Install tools:

```bash
sudo apt install nginx apache2-utils -y

```

Generate SSL (use Let’s Encrypt in prod):

```bash
sudo openssl req -x509 -nodes -days 365 \
  -newkey rsa:2048 \
  -keyout /etc/ssl/private/nginx-selfsigned.key \
  -out /etc/ssl/certs/nginx-selfsigned.crt
sudo openssl dhparam -out /etc/ssl/certs/dhparam.pem 2048

```

Create auth user:

```bash
sudo htpasswd -c /etc/nginx/.htpasswd monitor

```

Update NGINX site config (`/etc/nginx/sites-enabled/default`):

```
server {
    listen 80;
    server_name app-domain.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name app-domain.com;

    ssl_certificate /etc/ssl/certs/nginx-selfsigned.crt;
    ssl_certificate_key /etc/ssl/private/nginx-selfsigned.key;
    ssl_dhparam /etc/ssl/certs/dhparam.pem;

    location /metric/node {
        auth_basic "Protected Metrics";
        auth_basic_user_file /etc/nginx/.htpasswd;
        proxy_pass http://127.0.0.1:9100/metrics;
    }

    location /metric/nginx {
        auth_basic "Protected Metrics";
        auth_basic_user_file /etc/nginx/.htpasswd;
        proxy_pass http://127.0.0.1:9113/metrics;
    }
}

```

Reload:

```bash
sudo nginx -t && sudo systemctl reload nginx

```

Test:

```bash
curl -u monitor:yourpassword https://app-domain.com/metric/node -k

```

.
