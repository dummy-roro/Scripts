# Logs Rotation

# Option 1: PM2 Logrotate (easy & portable)

Run these commands once:

```bash
# Install module
pm2 install pm2-logrotate

# Rotate when logs exceed 10 MB
pm2 set pm2-logrotate:max_size 10M

# Keep 14 days of logs
pm2 set pm2-logrotate:retain 14

# Compress old logs (gzip)
pm2 set pm2-logrotate:compress true

# Rotate every day at midnight
pm2 set pm2-logrotate:rotateInterval '0 0 * * *'

# Date format for rotated logs
pm2 set pm2-logrotate:dateFormat 'YYYY-MM-DD_HH-mm-ss'
```

Check configuration:

```bash
pm2 conf
```

Flush old logs if needed:

```bash
pm2 flush
```

👉 After this, logs in `/home/ubuntu/.pm2/logs/` will look like:

```
out.log
out.log-2025-09-12_00-00-00.gz
error.log-2025-09-11_00-00-00.gz
```

---

# 🔹 Option 2: System Logrotate (Linux-native)

Create a file:

`/etc/logrotate.d/pm2`

```
/home/ubuntu/.pm2/logs/*.log {
    daily
    rotate 14
    compress
    delaycompress
    missingok
    notifempty
    copytruncate
}
```

- `daily` → rotate once a day
- `rotate 14` → keep 14 archives
- `compress` → gzip logs
- `delaycompress` → start compressing from the **second-oldest** file (keeps yesterday’s uncompressed for easy reading)
- `copytruncate` → avoids restarting PM2, truncates logs in place

Force test:

```bash
sudo logrotate -f /etc/logrotate.d/pm2
```

Then check `/home/ubuntu/.pm2/logs/` — you’ll see rotated and compressed logs:

```
out.log
out.log.1.gz
error.log.2.gz
```

---

✅ **Recommendation**:

- If you only care about **PM2 apps**, use **pm2-logrotate** (simpler).
- If you want **system-wide log policy** (like Nginx, MySQL, PM2 together), use **logrotate**.
