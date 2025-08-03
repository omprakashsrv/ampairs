#!/bin/bash

# Start Ampairs backend in development mode (without rate limiting)
echo "Starting Ampairs backend in development mode..."
echo "Rate limiting: DISABLED"
echo "reCAPTCHA: DISABLED"
echo "Profile: dev"
echo ""

cd ampairs_service

# Set environment variables for development
export SPRING_PROFILES_ACTIVE=dev
export BUCKET4J_ENABLED=false
export RECAPTCHA_ENABLED=false

# Start the application
./gradlew bootRun --args='--spring.profiles.active=dev'