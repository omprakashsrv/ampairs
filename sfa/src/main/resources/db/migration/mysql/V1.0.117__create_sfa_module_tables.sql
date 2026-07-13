-- SFA Module Database Migration (MySQL)
-- Version: 1.0.117
-- Description: Field-sales automation — beats, journey plans, planned visits, visits, attendance,
--              field orders. Offline-sync entities (soft-delete via `active`, incremental by updated_at).

CREATE TABLE beats
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    uid            VARCHAR(40)  NOT NULL,
    owner_id       VARCHAR(40)  NOT NULL,
    ref_id         VARCHAR(255),
    name           VARCHAR(150) NOT NULL,
    description    VARCHAR(500),
    rep_member_uid VARCHAR(40),
    scheduled_days VARCHAR(100),
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_beat_uid (uid),
    INDEX idx_beat_owner (owner_id),
    INDEX idx_beat_rep (rep_member_uid),
    INDEX idx_beat_updated_at (updated_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE beat_outlets
(
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    uid            VARCHAR(40) NOT NULL,
    owner_id       VARCHAR(40) NOT NULL,
    ref_id         VARCHAR(255),
    beat_uid       VARCHAR(40) NOT NULL,
    customer_uid   VARCHAR(40) NOT NULL,
    visit_sequence INT         NOT NULL DEFAULT 0,
    visit_day      VARCHAR(10),
    active         BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_beat_outlet_uid (uid),
    INDEX idx_beat_outlet_owner (owner_id),
    INDEX idx_beat_outlet_beat (beat_uid),
    INDEX idx_beat_outlet_customer (customer_uid),
    INDEX idx_beat_outlet_updated_at (updated_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE journey_plans
(
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    uid            VARCHAR(40) NOT NULL,
    owner_id       VARCHAR(40) NOT NULL,
    ref_id         VARCHAR(255),
    rep_member_uid VARCHAR(40) NOT NULL,
    beat_uid       VARCHAR(40) NOT NULL,
    weekday        VARCHAR(10),
    active         BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_journey_plan_uid (uid),
    INDEX idx_journey_plan_owner (owner_id),
    INDEX idx_journey_plan_rep (rep_member_uid),
    INDEX idx_journey_plan_updated_at (updated_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE planned_visits
(
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    uid              VARCHAR(40) NOT NULL,
    owner_id         VARCHAR(40) NOT NULL,
    ref_id           VARCHAR(255),
    journey_plan_uid VARCHAR(40),
    beat_uid         VARCHAR(40),
    customer_uid     VARCHAR(40) NOT NULL,
    rep_member_uid   VARCHAR(40) NOT NULL,
    planned_date     TIMESTAMP   NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    visit_sequence   INT         NOT NULL DEFAULT 0,
    active           BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_planned_visit_uid (uid),
    INDEX idx_planned_visit_owner (owner_id),
    INDEX idx_planned_visit_rep (rep_member_uid),
    INDEX idx_planned_visit_date (planned_date),
    INDEX idx_planned_visit_updated_at (updated_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE visits
(
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    uid               VARCHAR(40) NOT NULL,
    owner_id          VARCHAR(40) NOT NULL,
    ref_id            VARCHAR(255),
    customer_uid      VARCHAR(40) NOT NULL,
    rep_member_uid    VARCHAR(40) NOT NULL,
    planned_visit_uid VARCHAR(40),
    outcome           VARCHAR(20) NOT NULL DEFAULT 'NO_ORDER',
    latitude          DOUBLE,
    longitude         DOUBLE,
    distance_meters   DOUBLE,
    geo_fence_status  VARCHAR(20) NOT NULL DEFAULT 'NO_LOCATION',
    ad_hoc            BOOLEAN     NOT NULL DEFAULT FALSE,
    notes             VARCHAR(1000),
    order_uid         VARCHAR(40),
    visited_at        TIMESTAMP   NOT NULL,
    active            BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_visit_uid (uid),
    INDEX idx_visit_owner (owner_id),
    INDEX idx_visit_rep (rep_member_uid),
    INDEX idx_visit_customer (customer_uid),
    INDEX idx_visit_visited_at (visited_at),
    INDEX idx_visit_updated_at (updated_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE attendance
(
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    uid                 VARCHAR(40) NOT NULL,
    owner_id            VARCHAR(40) NOT NULL,
    ref_id              VARCHAR(255),
    rep_member_uid      VARCHAR(40) NOT NULL,
    check_in_at         TIMESTAMP   NULL,
    check_in_latitude   DOUBLE,
    check_in_longitude  DOUBLE,
    check_out_at        TIMESTAMP   NULL,
    check_out_latitude  DOUBLE,
    check_out_longitude DOUBLE,
    status              VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    active              BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_attendance_uid (uid),
    INDEX idx_attendance_owner (owner_id),
    INDEX idx_attendance_rep (rep_member_uid),
    INDEX idx_attendance_check_in (check_in_at),
    INDEX idx_attendance_updated_at (updated_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE field_orders
(
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    uid            VARCHAR(40)    NOT NULL,
    owner_id       VARCHAR(40)    NOT NULL,
    ref_id         VARCHAR(255),
    visit_uid      VARCHAR(40),
    customer_uid   VARCHAR(40)    NOT NULL,
    rep_member_uid VARCHAR(40)    NOT NULL,
    order_uid      VARCHAR(40),
    amount         DECIMAL(19, 4) NOT NULL DEFAULT 0,
    active         BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_field_order_uid (uid),
    INDEX idx_field_order_owner (owner_id),
    INDEX idx_field_order_visit (visit_uid),
    INDEX idx_field_order_order (order_uid),
    INDEX idx_field_order_updated_at (updated_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
