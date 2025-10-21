# Install Certbot + Nginx plug-in
```bash
sudo apt update
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

# Auto-renewal (Optional)
Certbot already adds a systemd timer. 
Verify:
```bash
sudo systemctl list-timers | grep certbot
sudo certbot renew --dry-run
```
# Enable the site with a symbolic link
```bash
sudo ln -s /etc/nginx/sites-available/<conf-name>.conf /etc/nginx/sites-enabled/
```
# If the symlink already exists, you can remove it first:
```bash
sudo rm /etc/nginx/sites-enabled/<conf-name>.conf
sudo ln -s /etc/nginx/sites-available/<conf-name>.conf /etc/nginx/sites-enabled/
```
# Reload After Any Manual Edit
```bash
sudo nginx -t && sudo systemctl reload nginx
```
