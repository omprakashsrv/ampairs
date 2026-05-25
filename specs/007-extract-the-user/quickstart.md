# Quickstart: Verify the User Module Split

## After implementation, validate with these commands:

```bash
# 1. Build user module in isolation
./gradlew :user:build

# 2. Build auth module (depends on user)
./gradlew :auth:build

# 3. Run auth tests
./gradlew :auth:test

# 4. Full CI gate
./gradlew ciBuild
```

## Check the dependency graph is acyclic:

```bash
./gradlew :user:dependencies --configuration compileClasspath | grep "ampairs"
# Expected: core, file — no auth
```

## Verify migrations are found:

```bash
./gradlew :ampairs_service:flywayInfo
# Expected: V1.0.13, V1.0.15, V1.0.16, V1.0.23 shown as Applied (from user location)
```
