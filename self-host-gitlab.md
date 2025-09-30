# Self Host GitLab server

# **GitLab installation requirements**

- **Tier**: Free, Premium, Ultimate
- **Offering**: GitLab Self-Managed

GitLab has specific installation requirements.

# **Storage**

The necessary storage space largely depends on the size of the repositories you want to have in GitLab. As a guideline, you should have at least as much free space as all your repositories combined.

The Linux package requires about 2.5 GB of storage space for installation. For storage flexibility, consider mounting your hard drive through logical volume management. You should have a hard drive with at least 7,200 RPM or a solid-state drive to reduce response times.

Because file system performance might affect the overall performance of GitLab, you should [avoid using cloud-based file systems for storage](https://docs.gitlab.com/administration/nfs/#avoid-using-cloud-based-file-systems).

# **CPU**

CPU requirements depend on the number of users and expected workload. The workload includes your users’ activity, use of automation and mirroring, and repository size.

For a maximum of 20 requests per second or 1,000 users, you should have 8 vCPU. For more users or higher workload, see [reference architectures](https://docs.gitlab.com/administration/reference_architectures/).

# **Memory**

Memory requirements depend on the number of users and expected workload. The workload includes your users’ activity, use of automation and mirroring, and repository size.

For a maximum of 20 requests per second or 1,000 users, you should have 16 GB of memory. For more users or higher workload, see [reference architectures](https://docs.gitlab.com/administration/reference_architectures/).

In some cases, GitLab can run with at least 8 GB of memory. For more information, see [running GitLab in a memory-constrained environment](https://docs.gitlab.com/omnibus/settings/memory_constrained_envs.html).

## **Step 1: Prepare GitLab Home Directory**

1. Create a directory to store GitLab persistent data:

```bash
export GITLAB_HOME=/srv/gitlab
mkdir -p $GITLAB_HOME/config $GITLAB_HOME/logs $GITLAB_HOME/data $GITLAB_HOME/backups
```

1. Make sure the directory is owned by your user or Docker user:

```bash
sudo chown -R 1000:1000 $GITLAB_HOME
```

> GitLab Docker expects files to be accessible by UID 1000.
> 

---

## **Step 2: Save Docker Compose File**

1. Create a folder for GitLab Docker Compose:

```bash
mkdir -p ~/gitlab-docker
cd ~/gitlab-docker
```

1. Save your `docker-compose.yml` file there:

```bash
services:
  gitlab:
    image: gitlab/gitlab-ce:latest        # consider pinning to a specific version for stability
    container_name: gitlab
    restart: unless-stopped
    hostname: gitlab                      # change if you have a domain
    shm_size: "512m"                      # avoids internal Postgres SHM issues
    ports:
      - "80:80"                           # HTTP (keep if using a reverse proxy)
      # - "443:443"                       # enable if GitLab terminates TLS itself
      - "2222:22"                         # SSH for Git over SSH (host 2222 -> container 22)
      # Optional: expose built-in exporters ONLY to localhost so a host Prometheus can scrape them
      - "127.0.0.1:9168:9168"             # gitlab-exporter
      - "127.0.0.1:9187:9187"             # postgres-exporter
      - "127.0.0.1:9121:9121"             # redis-exporter
      - "127.0.0.1:9100:9100"             # node-exporter
    environment:
      TZ: "UTC"
      GITLAB_OMNIBUS_CONFIG: |
        ## ===== Core URL & SSH =====
        external_url 'http://YOUR_DOMAIN_OR_IP'      # switch to https://… if you enable TLS
        gitlab_rails['gitlab_shell_ssh_port'] = 2222

        ## ===== Basic hardening =====
        gitlab_rails['gitlab_signup_enabled'] = false
        gitlab_rails['rack_attack_git_basic_auth'] = {
          'enabled' => true,
          'maxretry' => 10,
          'findtime' => 60,
          'bantime'  => 3600
        }
        # Disable registry by default (turn on later if you need it)
        gitlab_rails['gitlab_default_projects_features_container_registry'] = false

        ## ===== Metrics (scrape with host Prometheus on localhost) =====
        gitlab_rails['monitoring_enabled'] = true
        gitlab_rails['monitoring_whitelist'] = ['0.0.0.0/0']
        node_exporter['listen_address']     = '0.0.0.0:9100'
        redis_exporter['listen_address']    = '0.0.0.0:9121'
        postgres_exporter['listen_address'] = '0.0.0.0:9187'
        gitlab_exporter['listen_address']   = '0.0.0.0'
        gitlab_exporter['listen_port']      = 9168
        # If you prefer to disable GitLab's internal Prometheus:
        # prometheus['enable'] = false

        ## ===== Backups =====
        gitlab_rails['backup_path'] = "/var/opt/gitlab/backups"
        gitlab_rails['backup_keep_time'] = 604800   # 7 days

        ## ===== (Optional) SMTP for emails =====
        # gitlab_rails['smtp_enable'] = true
        # gitlab_rails['smtp_address'] = "smtp.example.com"
        # gitlab_rails['smtp_port'] = 587
        # gitlab_rails['smtp_user_name'] = "no-reply@example.com"
        # gitlab_rails['smtp_password'] = "CHANGE_ME"
        # gitlab_rails['smtp_domain'] = "example.com"
        # gitlab_rails['smtp_authentication'] = "login"
        # gitlab_rails['smtp_enable_starttls_auto'] = true
        # gitlab_rails['gitlab_email_from'] = "no-reply@example.com"

    volumes:
      - ./config:/etc/gitlab
      - ./logs:/var/log/gitlab
      - ./data:/var/opt/gitlab

    healthcheck:
      test: ["CMD", "/opt/gitlab/bin/gitlab-healthcheck", "--fail"]
      interval: 30s
      timeout: 10s
      retries: 5
```

Paste your configuration and save.

---

## **Step 3: Start GitLab Container**

```bash
docker-compose up -d
```

- The container will download `gitlab/gitlab-ce:latest` and start.
- Internal HTTP port: `8081`, SSH port: `2222`.

Check logs:

```bash
docker logs -f gitlab
```

- Wait until GitLab finishes initial setup (~5–10 minutes).

---

## **Step 4: Configure Nginx Reverse Proxy for HTTPS**

1. Install Nginx (if not already installed):

```bash
sudo apt update
sudo apt install nginx -y
```

1. Create Nginx config for GitLab:

```bash
sudo vi /etc/nginx/sites-available/gitlab
```

Paste the following:

```
server {
    listen 80;
    server_name gitlab.domain.com;

    location / {
        return 301 https://$host$request_uri;
    }
}

server {
    listen 443 ssl;
    server_name gitlab.solaratek.net;

    ssl_certificate /etc/letsencrypt/live/gitlab.domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/gitlab.domain.com/privkey.pem;

    proxy_read_timeout 300;
    proxy_connect_timeout 300;
    proxy_send_timeout 300;

    location / {
    proxy_pass https://127.0.0.1:8443;  # HTTPS inside container
    proxy_ssl_verify off;               # skip self-signed cert verification
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
}
```

1. Enable the site:

```bash
sudo ln -s /etc/nginx/sites-available/gitlab /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

---

## **Step 5: Get Let’s Encrypt Certificate**

1. Install Certbot:

```bash
sudo apt install certbot python3-certbot-nginx -y
```

1. Issue a certificate for your subdomain:

```bash
sudo certbot --nginx -d gitlab.domain.com
```

- Follow prompts. Certbot will automatically configure SSL in Nginx.

---

## **Step 6: Access GitLab**

- Open a browser: `https://gitlab.domain.com`
- First-time setup: set admin password.
- Git SSH is available on host port `2222`:

```bash
ssh -p 2222 git@gitlab.domain.com
```
