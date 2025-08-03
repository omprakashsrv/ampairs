#!/bin/bash

# Start Ampairs backend in development mode (without rate limiting)
echo "🚀 Starting Ampairs backend in development mode..."
echo "📍 Location: $(pwd)"
echo "🔒 Rate limiting: DISABLED"
echo "🤖 reCAPTCHA: DISABLED" 
echo "📋 Profile: dev"
echo "🌐 Server: http://localhost:8080"
echo "📊 Health Check: http://localhost:8080/actuator/health"
echo ""

# Change to ampairs_service directory
cd "$(dirname "$0")/ampairs_service" || {
    echo "❌ Error: Cannot find ampairs_service directory"
    echo "Make sure you're running this script from the project root"
    exit 1
}

# Set environment variables for development
export SPRING_PROFILES_ACTIVE=dev
export BUCKET4J_ENABLED=false
export RECAPTCHA_ENABLED=false

echo "Starting application..."
# Start the application
./gradlew bootRun