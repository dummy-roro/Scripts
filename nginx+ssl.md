# Install Certbot + Nginx plug-in
```bash
sudo yum install -y epel-release
sudo yum install -y certbot python3-certbot-nginx
```
# Obtain Certificate
```bash
sudo certbot --nginx -d your-domain.com
```
•
When prompted, choose “Redirect HTTP to HTTPS” (option 2).

•
Certbot automatically:

•
downloads the certificate,

•
edits your nginx config,

•
installs the renew cron.

# Auto-renewal
Certbot already adds a systemd timer. 
Verify:
```bash
sudo systemctl list-timers | grep certbot
sudo certbot renew --dry-run
```
# Reload After Any Manual Edit
```bash
sudo nginx -t && sudo systemctl reload nginx
```
