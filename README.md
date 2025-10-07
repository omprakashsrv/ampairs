# 🚀 Ampairs Business Management Platform

> **Comprehensive multi-platform business management solution with workspace-based architecture**

## 📁 Project Structure

```
ampairs/
├── 📁 ampairs-backend/         # Spring Boot Backend Services
│   ├── ampairs_service/        # Main application service
│   ├── core/                   # Shared core utilities
│   ├── auth/                   # Authentication & authorization
│   ├── workspace/              # Workspace management
│   ├── customer/               # Customer relationship management
│   ├── product/                # Product catalog management
│   ├── order/                  # Order processing
│   ├── invoice/                # Invoice generation
│   └── notification/           # Notification services
├── 📁 ampairs-web/             # Angular Web Application
├── 📁 ampairs-mp-app/          # Kotlin Multiplatform Mobile App
├── 📁 .github/workflows/       # CI/CD Pipeline
└── 📁 scripts/                 # Deployment scripts
```

## 🛠️ Development Setup

### Prerequisites
- **Java 25+** for backend development
- **Node.js 18+** for web frontend
- **Android Studio** for mobile development
- **PostgreSQL** for database

### Quick Start

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd ampairs
   ```

2. **Setup development environment**
   ```bash
   ./gradlew devSetup
   ```

3. **Start backend services**
   ```bash
   cd ampairs-backend
   ./gradlew bootRun
   ```

4. **Start web frontend** (if available)
   ```bash
   cd ampairs-web
   npm install
   npm start
   ```

## 🔧 Available Commands

### Root Level Commands
```bash
./gradlew buildAll      # Build all project components
./gradlew testAll       # Run tests for all components  
./gradlew cleanAll      # Clean all project components
./gradlew ciBuild       # CI/CD build with tests
```

### Backend Development
```bash
cd ampairs-backend
./gradlew bootRun              # Start the application
./gradlew test                 # Run tests
./gradlew :ampairs_service:bootJar  # Build JAR file
```

## 🏗️ Architecture

### Backend (Spring Boot + Kotlin)
- **Modular Architecture**: Domain-driven design with separate modules
- **Multi-tenancy**: Workspace-based isolation
- **JWT Authentication**: Secure token-based auth
- **PostgreSQL**: Primary database
- **REST APIs**: RESTful services for all modules

### Frontend Options
- **Web**: Angular with Material Design 3
- **Mobile**: Kotlin Multiplatform (Android/iOS)

## 🚀 Deployment

### Automated CI/CD
Push to `main` branch triggers automatic deployment:
1. ✅ Build & compile verification
2. 🧪 Automated testing  
3. 📦 JAR creation
4. 🚀 SSH deployment to Ubuntu server
5. 🔄 Service restart
6. 💚 Health verification

### Manual Deployment
```bash
cd ampairs-backend
./gradlew :ampairs_service:bootJar
```

For detailed deployment instructions, see [DEPLOYMENT.md](DEPLOYMENT.md)

## 📊 Monitoring

### Health Checks
- **Application**: `http://localhost:8080/actuator/health`
- **Service Status**: `systemctl status ampairs`
- **Logs**: `journalctl -u ampairs -f`

## 🤝 Contributing

1. Create feature branch: `git checkout -b feature/amazing-feature`
2. Make changes and test: `./gradlew testAll`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push branch: `git push origin feature/amazing-feature`
5. Create Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Support

For questions and support:
- 📧 Email: support@ampairs.com
- 📖 Documentation: [DEPLOYMENT.md](DEPLOYMENT.md)
- 🐛 Issues: GitHub Issues

---

**Made with ❤️ by the Ampairs Team**
