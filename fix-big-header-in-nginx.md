upstream sent too big header while reading response header from upstream
# Fix for Your Config
# Handle large client request headers (must be here, not inside location)
    large_client_header_buffers 4 16k;
    location / {
        proxy_pass http://localhost:8888;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;

        # WebSocket support
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;

        # Buffer settings (valid inside location)
        proxy_buffers 16 16k;
        proxy_buffer_size 32k;
        proxy_busy_buffers_size 64k;

        proxy_buffering off;
        proxy_request_buffering off;
    }

sudo nginx -t
sudo systemctl reload nginx






