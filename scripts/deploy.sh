#!/bin/bash

# Ampairs Service Deployment Script
# This script handles the deployment of the Ampairs Spring Boot application

set -e

# Configuration
APP_NAME="ampairs-service"
APP_DIR="/opt/ampairs"
SERVICE_NAME="ampairs"
SERVICE_USER="ampairs"
BACKUP_DIR="/opt/ampairs/backups"
LOG_DIR="/var/log/ampairs"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}🚀 Starting Ampairs Service Deployment${NC}"
echo "=================================================="

# Function to log messages
log() {
    echo -e "${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')] $1${NC}"
}

error() {
    echo -e "${RED}[ERROR] $1${NC}"
    exit 1
}

warning() {
    echo -e "${YELLOW}[WARNING] $1${NC}"
}

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    error "This script must be run as root (use sudo)"
fi

# Create necessary directories
log "Creating application directories..."
mkdir -p $APP_DIR
mkdir -p $BACKUP_DIR
mkdir -p $LOG_DIR

# Create service user if it doesn't exist
if ! id "$SERVICE_USER" &>/dev/null; then
    log "Creating service user: $SERVICE_USER"
    useradd -r -s /bin/false -d $APP_DIR $SERVICE_USER
fi

# Find the JAR file
JAR_FILE=$(find $APP_DIR -name "*.jar" -not -path "*/backups/*" | head -1)
if [ -z "$JAR_FILE" ]; then
    error "No JAR file found in $APP_DIR"
fi

log "Found JAR file: $JAR_FILE"

# Get JAR filename
JAR_FILENAME=$(basename "$JAR_FILE")
NEW_JAR_NAME="ampairs-service.jar"

# Backup current JAR if it exists
if [ -f "$APP_DIR/$NEW_JAR_NAME" ]; then
    log "Backing up current JAR..."
    BACKUP_NAME="ampairs-service-$(date +%Y%m%d-%H%M%S).jar"
    cp "$APP_DIR/$NEW_JAR_NAME" "$BACKUP_DIR/$BACKUP_NAME"
    log "Backup created: $BACKUP_DIR/$BACKUP_NAME"
fi

# Stop the service if it's running
log "Stopping $SERVICE_NAME service..."
if systemctl is-active --quiet $SERVICE_NAME; then
    systemctl stop $SERVICE_NAME
    log "Service stopped"
else
    log "Service is not running"
fi

# Copy new JAR
log "Deploying new JAR..."
cp "$JAR_FILE" "$APP_DIR/$NEW_JAR_NAME"

# Set permissions
log "Setting permissions..."
chown -R $SERVICE_USER:$SERVICE_USER $APP_DIR
chown -R $SERVICE_USER:$SERVICE_USER $LOG_DIR
chmod +x "$APP_DIR/$NEW_JAR_NAME"

# Install/update systemd service
log "Installing systemd service..."
if [ -f "/tmp/ampairs.service" ]; then
    cp /tmp/ampairs.service /etc/systemd/system/
    systemctl daemon-reload
    systemctl enable $SERVICE_NAME
    log "Systemd service installed and enabled"
fi

# Start the service
log "Starting $SERVICE_NAME service..."
systemctl start $SERVICE_NAME

# Wait for service to start
log "Waiting for service to start..."
sleep 10

# Check service status
if systemctl is-active --quiet $SERVICE_NAME; then
    log "✅ Service started successfully"
    
    # Display service status
    echo ""
    echo "Service Status:"
    systemctl status $SERVICE_NAME --no-pager -l
    
    echo ""
    log "🎉 Deployment completed successfully!"
    log "📊 Service logs: journalctl -u $SERVICE_NAME -f"
    log "📈 Application logs: tail -f $LOG_DIR/application.log"
else
    error "❌ Service failed to start. Check logs: journalctl -u $SERVICE_NAME"
fi

# Clean up old backups (keep last 5)
log "Cleaning up old backups..."
cd $BACKUP_DIR
ls -t ampairs-service-*.jar 2>/dev/null | tail -n +6 | xargs -r rm
log "Cleanup completed"

echo "=================================================="
log "🎯 Deployment Summary:"
log "   • JAR: $NEW_JAR_NAME"
log "   • Service: $SERVICE_NAME"
log "   • User: $SERVICE_USER"
log "   • Directory: $APP_DIR"
log "   • Logs: $LOG_DIR"
echo "=================================================="