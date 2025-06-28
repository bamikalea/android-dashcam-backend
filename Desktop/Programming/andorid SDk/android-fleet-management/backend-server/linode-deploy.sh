#!/bin/bash

# Linode Deployment Script for Android Fleet Management Backend
# Run this script on your Linode instance after connecting via SSH

echo "🚀 Starting Linode deployment for Android Fleet Management Backend..."

# Update system
echo "📦 Updating system packages..."
sudo apt update && sudo apt upgrade -y

# Install Node.js 18.x
echo "📦 Installing Node.js 18.x..."
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs

# Install PM2 for process management
echo "📦 Installing PM2..."
sudo npm install -g pm2

# Install nginx
echo "📦 Installing nginx..."
sudo apt install -y nginx

# Create application directory
echo "📁 Creating application directory..."
sudo mkdir -p /var/www/fleet-backend
sudo chown $USER:$USER /var/www/fleet-backend

# Navigate to app directory
cd /var/www/fleet-backend

# Clone your repository (replace with your actual repo URL)
echo "📥 Cloning repository..."
git clone https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git .

# Install dependencies
echo "📦 Installing Node.js dependencies..."
npm install --production

# Create uploads directory
mkdir -p uploads

# Set up environment variables
echo "🔧 Setting up environment variables..."
cat > .env << EOF
PORT=10000
NODE_ENV=production
CORS_ORIGIN=*
UPLOAD_DIR=uploads
MAX_FILE_SIZE=10485760
RATE_LIMIT_WINDOW=900000
RATE_LIMIT_MAX=100
EOF

# Configure nginx
echo "🔧 Configuring nginx..."
sudo tee /etc/nginx/sites-available/fleet-backend << EOF
server {
    listen 80;
    server_name your-domain.com;  # Replace with your domain or IP

    location / {
        proxy_pass http://localhost:10000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_cache_bypass \$http_upgrade;
    }

    # Serve static files
    location /uploads/ {
        alias /var/www/fleet-backend/uploads/;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
EOF

# Enable the site
sudo ln -sf /etc/nginx/sites-available/fleet-backend /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default

# Test nginx configuration
sudo nginx -t

# Start nginx
sudo systemctl start nginx
sudo systemctl enable nginx

# Start the application with PM2
echo "🚀 Starting application with PM2..."
pm2 start server.js --name "fleet-backend"

# Save PM2 configuration
pm2 save
pm2 startup

echo "✅ Deployment complete!"
echo "🌐 Your backend is now running on: http://your-linode-ip"
echo "📊 Monitor with: pm2 status"
echo "📝 View logs with: pm2 logs fleet-backend" 