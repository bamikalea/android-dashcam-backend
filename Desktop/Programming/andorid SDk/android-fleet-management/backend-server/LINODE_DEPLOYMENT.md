# Linode Deployment Guide for Android Fleet Management Backend

## Overview

This guide will help you deploy your Android Fleet Management backend to Linode using their $100 free trial credit.

## Prerequisites

- Linode account with $100 free trial credit
- Your backend code pushed to a GitHub repository
- Basic knowledge of SSH and command line

## Step 1: Create a Linode Instance

1. **Sign up for Linode**

   - Go to [linode.com](https://linode.com)
   - Create a new account
   - Verify your email

2. **Create a New Linode**
   - Click "Create Linode"
   - Choose **Ubuntu 22.04 LTS**
   - Select **Nanode 1GB** plan ($5/month)
   - Choose a datacenter close to your users
   - Set a strong root password
   - Click "Create Linode"

## Step 2: Connect to Your Linode

1. **Get your Linode's IP address** from the Linode dashboard
2. **Connect via SSH:**
   ```bash
   ssh root@YOUR_LINODE_IP
   ```

## Step 3: Deploy Your Backend

1. **Download the deployment script:**

   ```bash
   wget https://raw.githubusercontent.com/YOUR_USERNAME/YOUR_REPO/main/linode-deploy.sh
   ```

2. **Make it executable:**

   ```bash
   chmod +x linode-deploy.sh
   ```

3. **Edit the script** to use your actual repository:

   ```bash
   nano linode-deploy.sh
   ```

   Replace `YOUR_USERNAME/YOUR_REPO_NAME` with your actual GitHub repository URL.

4. **Run the deployment script:**
   ```bash
   ./linode-deploy.sh
   ```

## Step 4: Configure Your Android App

Update your Android app's server configuration to point to your Linode IP:

```java
// In ServerConfig.java
public static final String SERVER_URL = "http://YOUR_LINODE_IP";
public static final String API_BASE_URL = SERVER_URL + "/api";
```

## Step 5: Test Your Deployment

1. **Check if the server is running:**

   ```bash
   pm2 status
   ```

2. **View logs:**

   ```bash
   pm2 logs fleet-backend
   ```

3. **Test the API:**
   ```bash
   curl http://YOUR_LINODE_IP/api/health
   ```

## Step 6: Set Up Domain (Optional)

1. **Purchase a domain** from Linode or another provider
2. **Point DNS** to your Linode IP
3. **Update nginx configuration** with your domain name
4. **Enable HTTPS** with Let's Encrypt

## Monitoring and Maintenance

### PM2 Commands

```bash
# Check status
pm2 status

# View logs
pm2 logs fleet-backend

# Restart app
pm2 restart fleet-backend

# Stop app
pm2 stop fleet-backend

# Start app
pm2 start fleet-backend
```

### Nginx Commands

```bash
# Test configuration
sudo nginx -t

# Reload configuration
sudo systemctl reload nginx

# Check status
sudo systemctl status nginx
```

### System Updates

```bash
# Update system packages
sudo apt update && sudo apt upgrade -y

# Update Node.js dependencies
cd /var/www/fleet-backend
npm update
pm2 restart fleet-backend
```

## Cost Estimation

- **Nanode 1GB**: $5/month
- **With $100 credit**: ~20 months of free hosting
- **Additional costs**: Domain name (~$10-15/year) if desired

## Troubleshooting

### Common Issues

1. **Port 10000 not accessible**

   - Check if the app is running: `pm2 status`
   - Check firewall: `sudo ufw status`

2. **Nginx not serving the app**

   - Test nginx config: `sudo nginx -t`
   - Check nginx logs: `sudo tail -f /var/log/nginx/error.log`

3. **App crashes**
   - Check PM2 logs: `pm2 logs fleet-backend`
   - Check system resources: `htop`

### Useful Commands

```bash
# Check disk space
df -h

# Check memory usage
free -h

# Check running processes
ps aux | grep node

# Check open ports
netstat -tlnp
```

## Security Considerations

1. **Firewall Setup**

   ```bash
   sudo ufw allow ssh
   sudo ufw allow 'Nginx Full'
   sudo ufw enable
   ```

2. **Regular Updates**

   - Keep system packages updated
   - Update Node.js dependencies regularly
   - Monitor security advisories

3. **Backup Strategy**
   - Regular backups of your application data
   - Database backups if using one
   - Configuration backups

## Support

- **Linode Documentation**: [linode.com/docs](https://linode.com/docs)
- **Linode Community**: [linode.com/community](https://linode.com/community)
- **PM2 Documentation**: [pm2.keymetrics.io](https://pm2.keymetrics.io)
