-- SFA Reporting Migration (PostgreSQL)
-- Version: 1.0.121
-- Description: Field-ops reporting/survey/leave (Phase 8b) — leaves + visit_survey_responses.
--              Attendance summaries and visit productivity are derived (no tables).

CREATE TABLE leaves
(
    id             BIGSERIAL PRIMARY KEY,
    uid            VARCHAR(40)  NOT NULL UNIQUE,
    owner_id       VARCHAR(40)  NOT NULL,
    ref_id         VARCHAR(255),
    rep_member_uid VARCHAR(40)  NOT NULL,
    leave_date     TIMESTAMP(6) NOT NULL,
    reason         VARCHAR(500),
    marked_by      VARCHAR(40),
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_leave_owner ON leaves (owner_id);
CREATE INDEX idx_leave_rep ON leaves (rep_member_uid);
CREATE INDEX idx_leave_date ON leaves (leave_date);
CREATE INDEX idx_leave_updated_at ON leaves (updated_at);

CREATE TABLE visit_survey_responses
(
    id             BIGSERIAL PRIMARY KEY,
    uid            VARCHAR(40)  NOT NULL UNIQUE,
    owner_id       VARCHAR(40)  NOT NULL,
    ref_id         VARCHAR(255),
    visit_uid      VARCHAR(40)  NOT NULL,
    rep_member_uid VARCHAR(40),
    responses      TEXT,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_visit_survey_owner ON visit_survey_responses (owner_id);
CREATE INDEX idx_visit_survey_visit ON visit_survey_responses (visit_uid);
CREATE INDEX idx_visit_survey_updated_at ON visit_survey_responses (updated_at);
