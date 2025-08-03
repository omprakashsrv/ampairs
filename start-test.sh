#!/bin/bash

# Start Ampairs backend in test mode (optimized for E2E testing)
echo "Starting Ampairs backend in test mode..."
echo "Rate limiting: DISABLED"
echo "reCAPTCHA: DISABLED"
echo "Profile: test"
echo "OTP: Fixed to 123456 for testing"
echo ""

cd ampairs_service

# Set environment variables for testing
export SPRING_PROFILES_ACTIVE=test
export BUCKET4J_ENABLED=false
export RECAPTCHA_ENABLED=false

# Start the application
./gradlew bootRun --args='--spring.profiles.active=test'