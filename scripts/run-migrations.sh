#!/bin/bash

# Script to run Flyway migrations without Hibernate schema validation
# This allows migrations to create tables before Hibernate validates the schema

echo "Running Flyway migrations..."
echo "Disabling Hibernate DDL validation temporarily..."

# Run the application with ddl-auto=none to disable validation
# This allows Flyway to run migrations first
JPA_DDL_AUTO=none ./gradlew :ampairs_service:bootRun --args='--spring.jpa.hibernate.ddl-auto=none'
