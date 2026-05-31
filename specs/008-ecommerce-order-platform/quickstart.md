# Quickstart: Ecommerce Order Platform

**Branch**: `008-ecommerce-order-platform`

---

## Prerequisites

- Docker + Docker Compose running
- Java 21, Kotlin 2.3
- `./gradlew buildAll` passes on `main`

---

## 1. Start Local Services

```bash
# From repo root — starts PostgreSQL, Kafka
docker-compose up -d kafka postgres
```

Verify Kafka is up:
```bash
docker exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --list
```

---

## 2. Create Kafka Topics

Topics are auto-created by Kafka (`KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE: "true"`) but you can pre-create them for guaranteed partition counts:

```bash
docker exec kafka kafka-topics.sh --bootstrap-server localhost:9092 \
  --create --topic ecom-catalog-events --partitions 3 --replication-factor 1

docker exec kafka kafka-topics.sh --bootstrap-server localhost:9092 \
  --create --topic ecom-order-placed --partitions 3 --replication-factor 1

docker exec kafka kafka-topics.sh --bootstrap-server localhost:9092 \
  --create --topic ecom-order-status --partitions 3 --replication-factor 1
```

DLQ topics (auto-created on first failure, or pre-create):
```bash
for topic in ecom-catalog-events.dlq ecom-order-placed.dlq ecom-order-status.dlq; do
  docker exec kafka kafka-topics.sh --bootstrap-server localhost:9092 \
    --create --topic $topic --partitions 1 --replication-factor 1
done
```

---

## 3. Run Migrations

```bash
./gradlew :ampairs_service:flywayMigrate
```

Verify the new tables exist (V1.0.27–V1.0.35):
```bash
./gradlew :ampairs_service:flywayInfo
```

Expected new tables:
- `app_user.user_type` column (V1.0.27)
- `product.is_ecom_listed` column (V1.0.28)
- `customer_order.ecom_order_ref` column (V1.0.29)
- `ecom_storefront` (V1.0.30)
- `ecom_listed_product` (V1.0.31)
- `ecom_cart`, `ecom_cart_item` (V1.0.32)
- `ecom_order`, `ecom_order_line_item` (V1.0.33)
- `ecom_customer_address` (V1.0.34)
- Fulltext index on `ecom_listed_product` (V1.0.35)

---

## 4. Start the Service

```bash
./gradlew :ampairs_service:bootRun
```

---

## 5. End-to-End Happy Path

### Step 1: Register a merchant and create a storefront

```bash
# Register merchant (existing auth flow)
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"first_name":"Raj","email":"raj@greenmart.com","password":"Pass123!","phone":"9000000001","country_code":91}'

# Login and get JWT (replace with actual response token)
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"raj@greenmart.com","password":"Pass123!","device_id":"dev-001"}' \
  | jq -r '.data.access_token')

# Create workspace (existing workspace flow, get workspace_id)
WORKSPACE_ID="<your-workspace-id>"

# Create storefront
curl -X POST http://localhost:8080/api/v1/ecom/management/storefront \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Workspace-ID: $WORKSPACE_ID" \
  -H "Content-Type: application/json" \
  -d '{"name":"Green Mart","slug":"green-mart","description":"Fresh groceries"}'
```

### Step 2: List a product on the storefront

```bash
# Assuming product PROD-XYZ exists in the workspace
curl -X PUT http://localhost:8080/api/v1/products/PROD-XYZ/ecom/list \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Workspace-ID: $WORKSPACE_ID"

# Verify it appears in ecom (give Kafka ~5s to process)
sleep 5
curl http://localhost:8080/api/v1/store/green-mart/products
```

### Step 3: Publish the storefront

```bash
curl -X PUT http://localhost:8080/api/v1/ecom/management/storefront/publish \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Workspace-ID: $WORKSPACE_ID"
```

### Step 4: Customer browses, adds to cart, and checks out

```bash
# Register end customer (uses the existing auth endpoint; user_type=END_CUSTOMER skips workspace role)
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"first_name":"Priya","email":"priya@example.com","password":"Pass456!","phone":"9876543210","country_code":91,"user_type":"END_CUSTOMER"}'

# Login (same existing endpoint; returned JWT includes user_type claim)
CUST_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"priya@example.com","password":"Pass456!","device_id":"cust-dev-001"}' \
  | jq -r '.data.access_token')

# Create cart as guest (no auth needed)
SESSION_TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/store/green-mart/cart \
  | jq -r '.data.session_token')

# Add product to cart
PRODUCT_UID="LP-X1Y2Z3"   # from step 2 browse response
curl -X PUT http://localhost:8080/api/v1/store/green-mart/cart/$SESSION_TOKEN/items \
  -H "Content-Type: application/json" \
  -d "{\"listed_product_id\":\"$PRODUCT_UID\",\"quantity\":2}"

# After login, claim the guest cart to merge it with the customer's account cart
curl -X POST http://localhost:8080/api/v1/store/green-mart/cart/$SESSION_TOKEN/claim \
  -H "Authorization: Bearer $CUST_TOKEN"

# Checkout (auth required)
curl -X POST http://localhost:8080/api/v1/store/green-mart/cart/$SESSION_TOKEN/checkout \
  -H "Authorization: Bearer $CUST_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"delivery_address":{"address_line1":"42 MG Road","city":"Bangalore","state":"Karnataka","pin_code":"560001","country":"IN"}}'
```

Expected response: `{"success":true,"data":{"ecom_order_ref":"ECO-...","status":"PLACED",...}}`

### Step 5: Verify order appears in management

```bash
# Wait for Kafka processing (~5s)
sleep 5

curl http://localhost:8080/api/v1/ecom/management/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Workspace-ID: $WORKSPACE_ID"
```

---

## 6. Run Tests

```bash
# Unit tests (no Docker required)
./gradlew :ecom:test

# Integration tests (requires Docker)
./gradlew testAll
```

---

## 7. Monitor Kafka Events

```bash
# Watch catalog events
docker exec kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic ecom-catalog-events \
  --from-beginning

# Watch order placed events
docker exec kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic ecom-order-placed \
  --from-beginning
```
