#!/bin/bash

# Docker-based Linode Deployment Script
# This is an alternative to the traditional deployment

echo "🐳 Starting Docker-based Linode deployment..."

# Update system
echo "📦 Updating system packages..."
sudo apt update && sudo apt upgrade -y

# Install Docker
echo "🐳 Installing Docker..."
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Install Docker Compose
echo "🐳 Installing Docker Compose..."
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Create application directory
echo "📁 Creating application directory..."
sudo mkdir -p /opt/fleet-backend
sudo chown $USER:$USER /opt/fleet-backend
cd /opt/fleet-backend

# Clone your repository (replace with your actual repo URL)
echo "📥 Cloning repository..."
git clone https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git .

# Create uploads directory
mkdir -p uploads

# Start the application with Docker Compose
echo "🚀 Starting application with Docker Compose..."
docker-compose up -d

# Set up automatic restart
echo "🔄 Setting up automatic restart..."
sudo tee /etc/systemd/system/fleet-backend.service << EOF
[Unit]
Description=Fleet Management Backend
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/opt/fleet-backend
ExecStart=/usr/local/bin/docker-compose up -d
ExecStop=/usr/local/bin/docker-compose down
TimeoutStartSec=0

[Install]
WantedBy=multi-user.target
EOF

# Enable the service
sudo systemctl enable fleet-backend.service

echo "✅ Docker deployment complete!"
echo "🌐 Your backend is now running on: http://YOUR_LINODE_IP"
echo "📊 Check status with: docker-compose ps"
echo "📝 View logs with: docker-compose logs -f"
echo "🔄 Restart with: docker-compose restart" 