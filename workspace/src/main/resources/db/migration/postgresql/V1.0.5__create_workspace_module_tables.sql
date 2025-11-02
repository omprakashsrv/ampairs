
-- Workspace Module Database Migration Script (PostgreSQL)
-- Version: 4.9
-- Description: Create workspace core tables (workspaces, members, invitations, teams, modules, settings, activities, master modules)
-- Author: Codex CLI
-- Date: 2025-10-12
-- Dependencies: V1.0.1__create_auth_module_tables.sql

-- =====================================================
-- Workspaces
-- =====================================================
CREATE TABLE workspaces (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,


    name VARCHAR(100) NOT NULL,
    slug VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    workspace_type VARCHAR(50) NOT NULL,
    avatar_url VARCHAR(500),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    phone VARCHAR(20),
    email VARCHAR(255),
    website VARCHAR(255),
    tax_id VARCHAR(50),
    registration_number VARCHAR(100),
    subscription_plan VARCHAR(50) NOT NULL,
    max_users INT NOT NULL,
    storage_limit_gb INT NOT NULL,
    storage_used_gb INT NOT NULL,
    last_activity_at TIMESTAMP(6),
    status VARCHAR(50) NOT NULL,
    timezone VARCHAR(50),
    language VARCHAR(10),
    currency VARCHAR(3),
    date_format VARCHAR(20),
    time_format VARCHAR(10),
    subscription_updated_at TIMESTAMP(6),
    trial_expires_at TIMESTAMP(6),
    business_hours_start VARCHAR(5),
    business_hours_end VARCHAR(5),
    working_days TEXT,
    settings TEXT,
    features TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(36)
);

CREATE INDEX idx_workspace_owner ON workspaces(created_by);
CREATE INDEX idx_workspace_status ON workspaces(status);
CREATE INDEX idx_workspace_type ON workspaces(workspace_type);
CREATE INDEX idx_workspace_active ON workspaces(last_activity_at);

COMMENT ON TABLE workspaces IS 'Tenant workspaces for Ampairs SaaS';

-- =====================================================
-- Workspace Members
-- =====================================================
CREATE TABLE workspace_members (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,


    workspace_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    member_name VARCHAR(255),
    member_email VARCHAR(255),
    role VARCHAR(50) NOT NULL,
    permissions JSONB,
    invited_by VARCHAR(36),
    invited_by_name VARCHAR(255),
    invited_at TIMESTAMP(6),
    joined_at TIMESTAMP(6),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_active_at TIMESTAMP(6),
    notes TEXT,
    deactivated_at TIMESTAMP(6),
    deactivated_by VARCHAR(36),
    deactivated_by_name VARCHAR(255),
    deactivation_reason VARCHAR(500),
    job_title VARCHAR(100),
    phone VARCHAR(20),
    access_restrictions TEXT,
    team_ids JSONB,
    primary_team_id VARCHAR(36)
);

CREATE UNIQUE INDEX idx_member_workspace_user ON workspace_members(workspace_id, user_id);
CREATE INDEX idx_member_workspace ON workspace_members(workspace_id);
CREATE INDEX idx_member_user ON workspace_members(user_id);
CREATE INDEX idx_member_role ON workspace_members(role);
CREATE INDEX idx_member_active ON workspace_members(is_active);
CREATE INDEX idx_member_invited_by ON workspace_members(invited_by);
CREATE INDEX idx_member_joined_at ON workspace_members(joined_at);

COMMENT ON TABLE workspace_members IS 'Members belonging to a workspace';

-- =====================================================
-- Workspace Invitations
-- =====================================================
CREATE TABLE workspace_invitations (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,


    workspace_id VARCHAR(36) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20),
    country_code INT,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    token VARCHAR(100) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    message VARCHAR(500),
    invited_by VARCHAR(36),
    invited_by_name VARCHAR(255),
    department VARCHAR(100),
    team_ids JSONB,
    primary_team_id VARCHAR(36),
    job_title VARCHAR(100),
    metadata JSONB,
    accepted_at TIMESTAMP(6),
    rejected_at TIMESTAMP(6),
    rejection_reason VARCHAR(500),
    send_count INT NOT NULL DEFAULT 1,
    last_sent_at TIMESTAMP(6),
    cancelled_at TIMESTAMP(6),
    cancelled_by VARCHAR(36),
    cancellation_reason VARCHAR(500),
    auto_assign_teams BOOLEAN DEFAULT TRUE
);

CREATE UNIQUE INDEX idx_invitation_token ON workspace_invitations(token);
CREATE INDEX idx_invitation_workspace ON workspace_invitations(workspace_id);
CREATE INDEX idx_invitation_email ON workspace_invitations(email);
CREATE INDEX idx_invitation_phone ON workspace_invitations(phone);
CREATE INDEX idx_invitation_status ON workspace_invitations(status);
CREATE INDEX idx_invitation_expires_at ON workspace_invitations(expires_at);

COMMENT ON TABLE workspace_invitations IS 'Invitations to join a workspace';

-- =====================================================
-- Workspace Teams
-- =====================================================
CREATE TABLE workspace_teams (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,


    workspace_id VARCHAR(36) NOT NULL,
    team_code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    department VARCHAR(100),
    permissions JSONB,
    team_lead_id VARCHAR(36),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    max_members INT,
    created_by VARCHAR(36),
    updated_by VARCHAR(36)
);

CREATE UNIQUE INDEX idx_team_workspace_code ON workspace_teams(workspace_id, team_code);
CREATE INDEX idx_team_workspace ON workspace_teams(workspace_id);
CREATE INDEX idx_team_code ON workspace_teams(team_code);
CREATE INDEX idx_team_department ON workspace_teams(department);

COMMENT ON TABLE workspace_teams IS 'Teams within a workspace with permissions';

-- =====================================================
-- Master Modules Registry
-- =====================================================
CREATE TABLE master_modules (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,


    module_code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    tagline VARCHAR(500),
    category VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    required_tier VARCHAR(50) NOT NULL,
    required_role VARCHAR(50) NOT NULL,
    complexity VARCHAR(50) NOT NULL,
    version VARCHAR(50) NOT NULL,
    business_relevance JSONB,
    configuration JSONB,
    ui_metadata JSONB,
    route_info JSONB,
    navigation_index INT NOT NULL,
    provider VARCHAR(255),
    support_email VARCHAR(255),
    documentation_url VARCHAR(500),
    homepage_url VARCHAR(500),
    setup_guide_url VARCHAR(500),
    size_mb INT NOT NULL,
    install_count INT NOT NULL,
    rating DOUBLE PRECISION,
    rating_count INT NOT NULL,
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    release_notes TEXT,
    last_updated_at TIMESTAMP(6)
);

CREATE INDEX idx_master_module_category ON master_modules(category);
CREATE INDEX idx_master_module_status ON master_modules(status);
CREATE INDEX idx_master_module_tier ON master_modules(required_tier);
CREATE INDEX idx_master_module_complexity ON master_modules(complexity);
CREATE INDEX idx_master_module_active ON master_modules(active);
CREATE INDEX idx_master_module_featured ON master_modules(featured);

COMMENT ON TABLE master_modules IS 'Master registry of available modules';

-- =====================================================
-- Workspace Modules
-- =====================================================
CREATE TABLE workspace_modules (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,


    workspace_id VARCHAR(36) NOT NULL,
    master_module_id BIGINT NOT NULL REFERENCES master_modules(id),
    status VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    installed_version VARCHAR(50) NOT NULL,
    installed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    installed_by VARCHAR(36),
    installed_by_name VARCHAR(255),
    last_updated_at TIMESTAMP(6),
    last_updated_by VARCHAR(36),
    category_override VARCHAR(100),
    display_order INT NOT NULL DEFAULT 0,
    settings JSONB,
    usage_metrics JSONB,
    user_preferences JSONB,
    module_data JSONB,
    license_info VARCHAR(1000),
    license_expires_at TIMESTAMP(6),
    storage_used_mb INT NOT NULL DEFAULT 0,
    configuration_notes TEXT
);

CREATE UNIQUE INDEX idx_workspace_module_unique ON workspace_modules(workspace_id, master_module_id);
CREATE INDEX idx_workspace_module_workspace ON workspace_modules(workspace_id);
CREATE INDEX idx_workspace_module_master ON workspace_modules(master_module_id);
CREATE INDEX idx_workspace_module_status ON workspace_modules(status);
CREATE INDEX idx_workspace_module_enabled ON workspace_modules(enabled);
CREATE INDEX idx_workspace_module_installed ON workspace_modules(installed_at);
CREATE INDEX idx_workspace_module_order ON workspace_modules(display_order);
CREATE INDEX idx_workspace_module_category ON workspace_modules(category_override);

COMMENT ON TABLE workspace_modules IS 'Workspace-specific module installations';

-- =====================================================
-- Workspace Settings
-- =====================================================
CREATE TABLE workspace_settings (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,


    workspace_id VARCHAR(36) NOT NULL UNIQUE,
    logo_url VARCHAR(500),
    material_theme JSONB,
    material_colors JSONB,
    material_typography JSONB,
    business_settings JSONB,
    numbering_settings JSONB,
    document_templates JSONB,
    notification_settings JSONB,
    integration_settings JSONB,
    billing_settings JSONB,
    payment_settings JSONB,
    feature_toggles JSONB,
    security_settings JSONB,
    audit_settings JSONB,
    last_modified_by VARCHAR(36),
    last_modified_by_name VARCHAR(255)
);

COMMENT ON TABLE workspace_settings IS 'Workspace-specific appearance and configuration settings';

-- =====================================================
-- Workspace Activities
-- =====================================================
CREATE TABLE workspace_activities (
    id BIGSERIAL PRIMARY KEY,
    uid VARCHAR(200) NOT NULL UNIQUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,


    activity_type VARCHAR(50) NOT NULL,
    actor_id VARCHAR(36) NOT NULL,
    actor_name VARCHAR(255) NOT NULL,
    description VARCHAR(500) NOT NULL,
    target_entity_type VARCHAR(50),
    target_entity_id VARCHAR(36),
    target_entity_name VARCHAR(255),
    context_data JSONB,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    session_id VARCHAR(100),
    severity VARCHAR(10) NOT NULL DEFAULT 'INFO',
    workspace_id VARCHAR(36) NOT NULL
);

CREATE INDEX idx_workspace_activity_workspace_type ON workspace_activities(workspace_id, activity_type);
CREATE INDEX idx_workspace_activity_created_at ON workspace_activities(created_at);
CREATE INDEX idx_workspace_activity_actor ON workspace_activities(actor_id);
CREATE INDEX idx_workspace_activity_target ON workspace_activities(target_entity_type, target_entity_id);

COMMENT ON TABLE workspace_activities IS 'Audit log of workspace activities';
