# Ampairs Service CI/CD Deployment Guide

## Overview
This guide covers the complete CI/CD setup for the Ampairs business management system, including GitHub Actions workflow, server configuration, and deployment automation.

## 🚀 Quick Setup Checklist

### 1. GitHub Repository Setup
- [ ] Copy `.github/workflows/deploy.yml` to your repository
- [ ] Copy `scripts/` folder with deployment scripts
- [ ] Configure GitHub Secrets (see below)
- [ ] Test the workflow

### 2. Server Prerequisites  
- [ ] Ubuntu 20.04+ server
- [ ] Java 17 installed
- [ ] PostgreSQL installed and configured
- [ ] SSH access configured
- [ ] Firewall configured for port 8080

### 3. Environment Configuration
- [ ] Production environment variables set
- [ ] Database credentials configured
- [ ] SSL certificates (if using HTTPS)
- [ ] Log directories created

---

## 📋 Required GitHub Configuration

Configure these as **Repository Secrets** in your GitHub repository:

**Path**: `Repository → Settings → Secrets and variables → Actions → Repository secrets`

### 🔐 **SECRETS** (Sensitive Data)

```bash
# Authentication & Connection
SSH_PRIVATE_KEY       # 🔐 Private SSH key for server access
SSH_PASSPHRASE        # 🔐 SSH key passphrase/password (if encrypted)

# Database Security
DB_PASSWORD           # 🔐 Database password (secure)

# Application Security  
JWT_SECRET            # 🔐 Secure JWT secret key (256-bit recommended)

# External Service Keys
RECAPTCHA_SECRET_KEY  # 🔐 Google reCAPTCHA secret key
AWS_SECRET_ACCESS_KEY # 🔐 AWS secret access key
SMTP_PASSWORD         # 🔐 SMTP email password
MSG91_AUTH_KEY        # 🔐 MSG91 SMS authentication key

# Optional Service Passwords (if using)
REDIS_PASSWORD        # 🔐 Redis password (if using Redis)
S3_ENCRYPTION_KEY     # 🔐 S3 client-side encryption key (if using)
```

### 📊 **VARIABLES** (Non-Sensitive Configuration)

```bash
# Server Configuration
SSH_USER              # Username for SSH connection (e.g., 'ubuntu', 'ampairs')  
SERVER_HOST           # Server IP address or domain name
APP_PORT              # Application port (default: 8080)

# Database Configuration
DB_URL                # jdbc:postgresql://localhost:5432/ampairs_prod
DB_USERNAME           # Database username

# Application Settings
JWT_EXPIRATION        # Token expiration time (86400000 = 24 hours)
JWT_REFRESH_EXPIRATION # Refresh token expiration (604800000 = 7 days)

# AWS Configuration
AWS_ACCESS_KEY_ID     # AWS access key ID (not secret)
AWS_REGION            # AWS region (e.g., ap-south-1)
AWS_S3_BUCKET         # S3 bucket name

# Service Configuration
RECAPTCHA_SITE_KEY    # Google reCAPTCHA site key (public)
SMTP_HOST             # SMTP server host (e.g., smtp.gmail.com)
SMTP_USERNAME         # SMTP username/email
EMAIL_FROM            # From email address
MSG91_TEMPLATE_ID     # SMS template ID
MSG91_SENDER_ID       # SMS sender ID (e.g., AMPAIR)

# Application Settings
SPRING_PROFILES_ACTIVE # Application profile (production)
LOG_LEVEL_AMPAIRS     # Logging level (INFO, DEBUG)
AMPAIRS_UPLOAD_DIR    # Upload directory path
```

---

## 🖥️ Server Setup Guide

### 1. Initial Server Configuration

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Java 17
sudo apt install openjdk-17-jdk -y
java -version

# Install PostgreSQL
sudo apt install postgresql postgresql-contrib -y
sudo systemctl enable postgresql
sudo systemctl start postgresql

# Create application user
sudo useradd -r -s /bin/false -d /opt/ampairs ampairs
sudo mkdir -p /opt/ampairs
sudo mkdir -p /var/log/ampairs
sudo chown -R ampairs:ampairs /opt/ampairs
sudo chown -R ampairs:ampairs /var/log/ampairs
```

### 2. Database Setup

```bash
# Switch to postgres user
sudo -u postgres psql

-- Create database and user
CREATE DATABASE ampairs_prod;
CREATE USER ampairs_user WITH PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE ampairs_prod TO ampairs_user;
ALTER USER ampairs_user CREATEDB;
\q
```

### 3. Firewall Configuration

```bash
# Configure UFW firewall
sudo ufw enable
sudo ufw allow ssh
sudo ufw allow 8080/tcp  # Application port
sudo ufw allow 80/tcp    # HTTP (if using nginx)
sudo ufw allow 443/tcp   # HTTPS (if using nginx)
sudo ufw status
```

### 4. SSH Key Setup

```bash
# On your local machine, generate SSH key pair WITH PASSPHRASE
ssh-keygen -t rsa -b 4096 -C "github-actions@ampairs.com"
# When prompted, enter a strong passphrase for additional security

# Copy public key to server
ssh-copy-id -i ~/.ssh/id_rsa.pub username@your-server-ip

# Add private key to GitHub Secrets as SSH_PRIVATE_KEY
cat ~/.ssh/id_rsa  # Copy this to GitHub secret

# Add passphrase to GitHub Secrets as SSH_PASSPHRASE
# Use the passphrase you entered during key generation
```

**Alternative: Generate Key Without Passphrase (Less Secure)**
```bash
# Generate key without passphrase (not recommended for production)
ssh-keygen -t rsa -b 4096 -C "github-actions@ampairs.com" -N ""
# No SSH_PASSPHRASE secret needed in this case
```

**Option 2: Username/Password Authentication (Not Recommended)**
```bash
# If you must use password authentication, modify the workflow:
# Replace SSH key steps with sshpass approach
sudo apt-get install -y sshpass
sshpass -p "${{ secrets.SSH_PASSWORD }}" scp file.jar user@server:/path/
sshpass -p "${{ secrets.SSH_PASSWORD }}" ssh user@server "commands"

# GitHub Secrets needed:
# SSH_PASSWORD  # Server user password
```

**Option 3: Using SSH Agent with Multiple Keys**
```bash
# For multiple servers or keys, you can configure ssh-agent with multiple keys
# Add multiple SSH_PRIVATE_KEY_* secrets and configure accordingly
```

---

## 🔧 Environment Variables Setup

### Server Environment File
Create `/etc/environment` or use systemd service file:

```bash
# Database Configuration
DB_URL=jdbc:postgresql://localhost:5432/ampairs_prod
DB_USERNAME=ampairs_user  
DB_PASSWORD=your_secure_database_password

# JWT Security
JWT_SECRET=your_very_secure_jwt_secret_key_here
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# Application Settings
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=production

# Logging
LOG_LEVEL_AMPAIRS=INFO
LOG_LEVEL_ROOT=WARN

# Upload Configuration
AMPAIRS_UPLOAD_DIR=/opt/ampairs/uploads
AMPAIRS_TEMP_DIR=/tmp/ampairs

# External Services (configure as needed)
RECAPTCHA_ENABLED=true
RECAPTCHA_SECRET_KEY=your_recaptcha_secret
EMAIL_ENABLED=true
SMTP_HOST=smtp.gmail.com
SMTP_USERNAME=your_email@gmail.com
SMTP_PASSWORD=your_app_password
```

---

## 🚀 Deployment Process

### Automatic Deployment (Recommended)

1. **Push to main branch**: The CI/CD pipeline triggers automatically
2. **Build & Test**: Code is compiled and tests are executed
3. **Deploy**: JAR is built and deployed to the server
4. **Health Check**: Application health is verified

### Manual Deployment

If you need to deploy manually:

```bash
# On your local machine
./gradlew :ampairs_service:bootJar

# Copy to server
scp ampairs_service/build/libs/*.jar username@server:/opt/ampairs/

# SSH to server and run deployment
ssh username@server
cd /opt/ampairs
sudo ./deploy.sh
```

---

## 📊 Monitoring and Logs

### Application Logs
```bash
# View application logs
sudo journalctl -u ampairs -f

# View application file logs  
tail -f /var/log/ampairs/application.log

# View access logs
tail -f /var/log/ampairs/access_log.$(date +%Y-%m-%d).log
```

### Health Checks
```bash
# Check application health
curl http://localhost:8080/actuator/health

# Check service status
sudo systemctl status ampairs

# Check system resources
htop
df -h
free -h
```

---

## 🐛 Troubleshooting

### Common Issues

#### 1. Service Won't Start
```bash
# Check service logs
sudo journalctl -u ampairs -n 50

# Check if port is in use
sudo netstat -tlnp | grep 8080

# Verify Java installation
java -version
```

#### 2. Database Connection Issues
```bash
# Test database connection
sudo -u postgres psql -d ampairs_prod -c "SELECT version();"

# Check PostgreSQL status
sudo systemctl status postgresql
```

#### 3. Permission Issues
```bash
# Fix file permissions
sudo chown -R ampairs:ampairs /opt/ampairs
sudo chown -R ampairs:ampairs /var/log/ampairs
sudo chmod +x /opt/ampairs/*.jar
```

#### 4. Memory Issues
```bash
# Check memory usage
free -h
ps aux --sort=-%mem | head -10

# Adjust JVM memory in systemd service
# Edit JAVA_OPTS in /etc/systemd/system/ampairs.service
```

---

## 🔐 Security Best Practices

### 1. Server Security
- Keep system updated: `sudo apt update && sudo apt upgrade`
- Use fail2ban: `sudo apt install fail2ban`
- Configure SSH key-only authentication
- Use strong database passwords
- Enable firewall with minimal ports

### 2. Application Security
- Use environment variables for secrets
- Rotate JWT secrets regularly
- Enable HTTPS in production
- Configure rate limiting appropriately
- Regular security audits

### 3. Database Security
- Use strong passwords
- Limit database connections
- Regular backups
- Enable SSL for database connections
- Restrict database user permissions

---

## 📦 Backup Strategy

### Database Backup
```bash
# Create daily backup script
cat > /opt/ampairs/backup.sh << 'EOF'
#!/bin/bash
BACKUP_DIR="/opt/ampairs/backups"
DATE=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR
pg_dump -U ampairs_user ampairs_prod > $BACKUP_DIR/ampairs_prod_$DATE.sql
find $BACKUP_DIR -name "*.sql" -mtime +7 -delete
EOF

chmod +x /opt/ampairs/backup.sh

# Add to crontab
echo "0 2 * * * /opt/ampairs/backup.sh" | sudo crontab -
```

### Application Backup
```bash
# Backup uploads and configurations
tar -czf /opt/ampairs/backups/app_backup_$(date +%Y%m%d).tar.gz \
  /opt/ampairs/uploads \
  /opt/ampairs/ampairs-service.jar
```

---

## 📞 Support

For deployment issues:
1. Check the troubleshooting section above
2. Review application logs
3. Verify environment variables
4. Check system resources
5. Contact the development team with specific error messages

---

## 🔄 Update Process

### Application Updates
The CI/CD pipeline automatically handles updates when code is pushed to the main branch. The process includes:

1. **Backup**: Current JAR is backed up automatically
2. **Deploy**: New JAR is deployed
3. **Restart**: Service is restarted with zero-downtime approach
4. **Verify**: Health checks confirm successful deployment

### Rolling Back
If deployment fails, you can quickly rollback:

```bash
# List available backups
ls -la /opt/ampairs/backups/

# Restore previous version
sudo systemctl stop ampairs
sudo cp /opt/ampairs/backups/ampairs-service-YYYYMMDD-HHMMSS.jar /opt/ampairs/ampairs-service.jar
sudo systemctl start ampairs
```

---

**Last Updated**: January 2025
**Version**: 1.0