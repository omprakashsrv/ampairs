-- SFA Module Database Migration (PostgreSQL)
-- Version: 1.0.117
-- Description: Field-sales automation — beats, journey plans, planned visits, visits, attendance,
--              field orders. Offline-sync entities (soft-delete via `active`, incremental by updated_at).

-- ===================================================== beats =====================================================
CREATE TABLE beats
(
    id             BIGSERIAL PRIMARY KEY,
    uid            VARCHAR(40)  NOT NULL UNIQUE,
    owner_id       VARCHAR(40)  NOT NULL,
    ref_id         VARCHAR(255),
    name           VARCHAR(150) NOT NULL,
    description    VARCHAR(500),
    rep_member_uid VARCHAR(40),
    scheduled_days VARCHAR(100),
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_beat_owner ON beats (owner_id);
CREATE INDEX idx_beat_rep ON beats (rep_member_uid);
CREATE INDEX idx_beat_updated_at ON beats (updated_at);

-- ================================================= beat_outlets ==================================================
CREATE TABLE beat_outlets
(
    id             BIGSERIAL PRIMARY KEY,
    uid            VARCHAR(40)  NOT NULL UNIQUE,
    owner_id       VARCHAR(40)  NOT NULL,
    ref_id         VARCHAR(255),
    beat_uid       VARCHAR(40)  NOT NULL,
    customer_uid   VARCHAR(40)  NOT NULL,
    visit_sequence INT          NOT NULL DEFAULT 0,
    visit_day      VARCHAR(10),
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_beat_outlet_owner ON beat_outlets (owner_id);
CREATE INDEX idx_beat_outlet_beat ON beat_outlets (beat_uid);
CREATE INDEX idx_beat_outlet_customer ON beat_outlets (customer_uid);
CREATE INDEX idx_beat_outlet_updated_at ON beat_outlets (updated_at);

-- ================================================ journey_plans ==================================================
CREATE TABLE journey_plans
(
    id             BIGSERIAL PRIMARY KEY,
    uid            VARCHAR(40)  NOT NULL UNIQUE,
    owner_id       VARCHAR(40)  NOT NULL,
    ref_id         VARCHAR(255),
    rep_member_uid VARCHAR(40)  NOT NULL,
    beat_uid       VARCHAR(40)  NOT NULL,
    weekday        VARCHAR(10),
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_journey_plan_owner ON journey_plans (owner_id);
CREATE INDEX idx_journey_plan_rep ON journey_plans (rep_member_uid);
CREATE INDEX idx_journey_plan_updated_at ON journey_plans (updated_at);

-- ================================================ planned_visits =================================================
CREATE TABLE planned_visits
(
    id               BIGSERIAL PRIMARY KEY,
    uid              VARCHAR(40)  NOT NULL UNIQUE,
    owner_id         VARCHAR(40)  NOT NULL,
    ref_id           VARCHAR(255),
    journey_plan_uid VARCHAR(40),
    beat_uid         VARCHAR(40),
    customer_uid     VARCHAR(40)  NOT NULL,
    rep_member_uid   VARCHAR(40)  NOT NULL,
    planned_date     TIMESTAMP(6) NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    visit_sequence   INT          NOT NULL DEFAULT 0,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_planned_visit_owner ON planned_visits (owner_id);
CREATE INDEX idx_planned_visit_rep ON planned_visits (rep_member_uid);
CREATE INDEX idx_planned_visit_date ON planned_visits (planned_date);
CREATE INDEX idx_planned_visit_updated_at ON planned_visits (updated_at);

-- ==================================================== visits =====================================================
CREATE TABLE visits
(
    id                BIGSERIAL PRIMARY KEY,
    uid               VARCHAR(40)      NOT NULL UNIQUE,
    owner_id          VARCHAR(40)      NOT NULL,
    ref_id            VARCHAR(255),
    customer_uid      VARCHAR(40)      NOT NULL,
    rep_member_uid    VARCHAR(40)      NOT NULL,
    planned_visit_uid VARCHAR(40),
    outcome           VARCHAR(20)      NOT NULL DEFAULT 'NO_ORDER',
    latitude          DOUBLE PRECISION,
    longitude         DOUBLE PRECISION,
    distance_meters   DOUBLE PRECISION,
    geo_fence_status  VARCHAR(20)      NOT NULL DEFAULT 'NO_LOCATION',
    ad_hoc            BOOLEAN          NOT NULL DEFAULT FALSE,
    notes             VARCHAR(1000),
    order_uid         VARCHAR(40),
    visited_at        TIMESTAMP(6)     NOT NULL,
    active            BOOLEAN          NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_visit_owner ON visits (owner_id);
CREATE INDEX idx_visit_rep ON visits (rep_member_uid);
CREATE INDEX idx_visit_customer ON visits (customer_uid);
CREATE INDEX idx_visit_visited_at ON visits (visited_at);
CREATE INDEX idx_visit_updated_at ON visits (updated_at);

-- ================================================== attendance ===================================================
CREATE TABLE attendance
(
    id                  BIGSERIAL PRIMARY KEY,
    uid                 VARCHAR(40)      NOT NULL UNIQUE,
    owner_id            VARCHAR(40)      NOT NULL,
    ref_id              VARCHAR(255),
    rep_member_uid      VARCHAR(40)      NOT NULL,
    check_in_at         TIMESTAMP(6),
    check_in_latitude   DOUBLE PRECISION,
    check_in_longitude  DOUBLE PRECISION,
    check_out_at        TIMESTAMP(6),
    check_out_latitude  DOUBLE PRECISION,
    check_out_longitude DOUBLE PRECISION,
    status              VARCHAR(20)      NOT NULL DEFAULT 'OPEN',
    active              BOOLEAN          NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_attendance_owner ON attendance (owner_id);
CREATE INDEX idx_attendance_rep ON attendance (rep_member_uid);
CREATE INDEX idx_attendance_check_in ON attendance (check_in_at);
CREATE INDEX idx_attendance_updated_at ON attendance (updated_at);

-- ================================================= field_orders ==================================================
CREATE TABLE field_orders
(
    id             BIGSERIAL PRIMARY KEY,
    uid            VARCHAR(40)    NOT NULL UNIQUE,
    owner_id       VARCHAR(40)    NOT NULL,
    ref_id         VARCHAR(255),
    visit_uid      VARCHAR(40),
    customer_uid   VARCHAR(40)    NOT NULL,
    rep_member_uid VARCHAR(40)    NOT NULL,
    order_uid      VARCHAR(40),
    amount         DECIMAL(19, 4) NOT NULL DEFAULT 0,
    active         BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_field_order_owner ON field_orders (owner_id);
CREATE INDEX idx_field_order_visit ON field_orders (visit_uid);
CREATE INDEX idx_field_order_order ON field_orders (order_uid);
CREATE INDEX idx_field_order_updated_at ON field_orders (updated_at);
