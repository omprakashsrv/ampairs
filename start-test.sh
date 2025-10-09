#!/bin/bash

# Start Ampairs backend in test mode (optimized for E2E testing)
echo "🧪 Starting Ampairs backend in test mode..."
echo "📍 Location: $(pwd)"
echo "🔒 Rate limiting: DISABLED"
echo "🤖 reCAPTCHA: DISABLED"
echo "📋 Profile: test"
echo "🔑 OTP: Fixed to 123456 for testing"
echo "🌐 Server: http://localhost:8080"
echo "📊 Health Check: http://localhost:8080/actuator/health"
echo "🎯 Optimized for E2E testing"
echo ""

# Change to ampairs_service directory
cd "$(dirname "$0")/ampairs_service" || {
    echo "❌ Error: Cannot find ampairs_service directory"
    echo "Make sure you're running this script from the project root"
    exit 1
}

# Set environment variables for testing
export SPRING_PROFILES_ACTIVE=test
export BUCKET4J_ENABLED=false
export RECAPTCHA_ENABLED=false

echo "Starting application..."
# Start the application
./gradlew bootRun