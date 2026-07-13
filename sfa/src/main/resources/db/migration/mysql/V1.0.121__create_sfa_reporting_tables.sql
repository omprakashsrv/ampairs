-- SFA Reporting Migration (MySQL)
-- Version: 1.0.121
-- Description: Field-ops reporting/survey/leave (Phase 8b) — leaves + visit_survey_responses.

CREATE TABLE leaves
(
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    uid            VARCHAR(40) NOT NULL,
    owner_id       VARCHAR(40) NOT NULL,
    ref_id         VARCHAR(255),
    rep_member_uid VARCHAR(40) NOT NULL,
    leave_date     TIMESTAMP   NOT NULL,
    reason         VARCHAR(500),
    marked_by      VARCHAR(40),
    active         BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_leave_uid (uid),
    INDEX idx_leave_owner (owner_id),
    INDEX idx_leave_rep (rep_member_uid),
    INDEX idx_leave_date (leave_date),
    INDEX idx_leave_updated_at (updated_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE visit_survey_responses
(
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    uid            VARCHAR(40) NOT NULL,
    owner_id       VARCHAR(40) NOT NULL,
    ref_id         VARCHAR(255),
    visit_uid      VARCHAR(40) NOT NULL,
    rep_member_uid VARCHAR(40),
    responses      TEXT,
    active         BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_visit_survey_uid (uid),
    INDEX idx_visit_survey_owner (owner_id),
    INDEX idx_visit_survey_visit (visit_uid),
    INDEX idx_visit_survey_updated_at (updated_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
