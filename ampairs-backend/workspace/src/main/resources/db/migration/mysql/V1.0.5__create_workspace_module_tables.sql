
-- Workspace Module Database Migration Script
-- Version: 4.9
-- Description: Create workspace core tables (workspaces, members, invitations, teams, modules, settings, activities, master modules)
-- Author: Codex CLI
-- Date: 2025-10-12
-- Dependencies: V1.0.1__create_auth_module_tables.sql

-- =====================================================
-- Workspaces
-- =====================================================
CREATE TABLE workspaces (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,


    name VARCHAR(100) NOT NULL,
    slug VARCHAR(50) NOT NULL,
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
    last_activity_at TIMESTAMP NULL,
    status VARCHAR(50) NOT NULL,
    timezone VARCHAR(50),
    language VARCHAR(10),
    currency VARCHAR(3),
    date_format VARCHAR(20),
    time_format VARCHAR(10),
    subscription_updated_at TIMESTAMP NULL,
    trial_expires_at TIMESTAMP NULL,
    business_hours_start VARCHAR(5),
    business_hours_end VARCHAR(5),
    working_days TEXT,
    settings TEXT,
    features TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(36),

    PRIMARY KEY (id),
    UNIQUE INDEX idx_workspace_uid (uid),
    UNIQUE INDEX idx_workspace_slug (slug),
    INDEX idx_workspace_owner (created_by),
    INDEX idx_workspace_status (status),
    INDEX idx_workspace_type (workspace_type),
    INDEX idx_workspace_active (last_activity_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Tenant workspaces for Ampairs SaaS';

-- =====================================================
-- Workspace Members
-- =====================================================
CREATE TABLE workspace_members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,


    workspace_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    member_name VARCHAR(255),
    member_email VARCHAR(255),
    role VARCHAR(50) NOT NULL,
    permissions JSON,
    invited_by VARCHAR(36),
    invited_by_name VARCHAR(255),
    invited_at TIMESTAMP NULL,
    joined_at TIMESTAMP NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_active_at TIMESTAMP NULL,
    notes TEXT,
    deactivated_at TIMESTAMP NULL,
    deactivated_by VARCHAR(36),
    deactivated_by_name VARCHAR(255),
    deactivation_reason VARCHAR(500),
    job_title VARCHAR(100),
    phone VARCHAR(20),
    access_restrictions TEXT,
    team_ids JSON,
    primary_team_id VARCHAR(36),

    PRIMARY KEY (id),
    UNIQUE INDEX idx_member_workspace_user (workspace_id, user_id),
    INDEX idx_member_workspace (workspace_id),
    INDEX idx_member_user (user_id),
    INDEX idx_member_role (role),
    INDEX idx_member_active (is_active),
    INDEX idx_member_invited_by (invited_by),
    INDEX idx_member_joined_at (joined_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Members belonging to a workspace';

-- =====================================================
-- Workspace Invitations
-- =====================================================
CREATE TABLE workspace_invitations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,


    workspace_id VARCHAR(36) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20),
    country_code INT,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    token VARCHAR(100) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    message VARCHAR(500),
    invited_by VARCHAR(36),
    invited_by_name VARCHAR(255),
    department VARCHAR(100),
    team_ids JSON,
    primary_team_id VARCHAR(36),
    job_title VARCHAR(100),
    metadata JSON,
    accepted_at TIMESTAMP NULL,
    rejected_at TIMESTAMP NULL,
    rejection_reason VARCHAR(500),
    send_count INT NOT NULL DEFAULT 1,
    last_sent_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    cancelled_by VARCHAR(36),
    cancellation_reason VARCHAR(500),
    auto_assign_teams BOOLEAN DEFAULT TRUE,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_invitation_token (token),
    INDEX idx_invitation_workspace (workspace_id),
    INDEX idx_invitation_email (email),
    INDEX idx_invitation_phone (phone),
    INDEX idx_invitation_status (status),
    INDEX idx_invitation_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Invitations to join a workspace';

-- =====================================================
-- Workspace Teams
-- =====================================================
CREATE TABLE workspace_teams (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,


    workspace_id VARCHAR(36) NOT NULL,
    team_code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    department VARCHAR(100),
    permissions JSON,
    team_lead_id VARCHAR(36),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    max_members INT,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),

    PRIMARY KEY (id),
    UNIQUE INDEX idx_team_workspace_code (workspace_id, team_code),
    INDEX idx_team_workspace (workspace_id),
    INDEX idx_team_code (team_code),
    INDEX idx_team_department (department)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Teams within a workspace with permissions';

-- =====================================================
-- Master Modules Registry
-- =====================================================
CREATE TABLE master_modules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,


    module_code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    tagline VARCHAR(500),
    category VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    required_tier VARCHAR(50) NOT NULL,
    required_role VARCHAR(50) NOT NULL,
    complexity VARCHAR(50) NOT NULL,
    version VARCHAR(50) NOT NULL,
    business_relevance JSON,
    configuration JSON,
    ui_metadata JSON,
    route_info JSON,
    navigation_index INT NOT NULL,
    provider VARCHAR(255),
    support_email VARCHAR(255),
    documentation_url VARCHAR(500),
    homepage_url VARCHAR(500),
    setup_guide_url VARCHAR(500),
    size_mb INT NOT NULL,
    install_count INT NOT NULL,
    rating DOUBLE,
    rating_count INT NOT NULL,
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    release_notes TEXT,
    last_updated_at TIMESTAMP NULL,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_master_module_code (module_code),
    INDEX idx_master_module_category (category),
    INDEX idx_master_module_status (status),
    INDEX idx_master_module_tier (required_tier),
    INDEX idx_master_module_complexity (complexity),
    INDEX idx_master_module_active (active),
    INDEX idx_master_module_featured (featured)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Master registry of available modules';

-- =====================================================
-- Workspace Modules
-- =====================================================
CREATE TABLE workspace_modules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,


    workspace_id VARCHAR(36) NOT NULL,
    master_module_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    installed_version VARCHAR(50) NOT NULL,
    installed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    installed_by VARCHAR(36),
    installed_by_name VARCHAR(255),
    last_updated_at TIMESTAMP NULL,
    last_updated_by VARCHAR(36),
    category_override VARCHAR(100),
    display_order INT NOT NULL DEFAULT 0,
    settings JSON,
    usage_metrics JSON,
    user_preferences JSON,
    module_data JSON,
    license_info VARCHAR(1000),
    license_expires_at TIMESTAMP NULL,
    storage_used_mb INT NOT NULL DEFAULT 0,
    configuration_notes TEXT,

    PRIMARY KEY (id),
    UNIQUE INDEX idx_workspace_module_unique (workspace_id, master_module_id),
    INDEX idx_workspace_module_workspace (workspace_id),
    INDEX idx_workspace_module_master (master_module_id),
    INDEX idx_workspace_module_status (status),
    INDEX idx_workspace_module_enabled (enabled),
    INDEX idx_workspace_module_installed (installed_at),
    INDEX idx_workspace_module_order (display_order),
    INDEX idx_workspace_module_category (category_override),
    CONSTRAINT fk_workspace_module_master FOREIGN KEY (master_module_id) REFERENCES master_modules (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Workspace-specific module installations';

-- =====================================================
-- Workspace Settings
-- =====================================================
CREATE TABLE workspace_settings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,


    workspace_id VARCHAR(36) NOT NULL,
    logo_url VARCHAR(500),
    material_theme JSON,
    material_colors JSON,
    material_typography JSON,
    business_settings JSON,
    numbering_settings JSON,
    document_templates JSON,
    notification_settings JSON,
    integration_settings JSON,
    billing_settings JSON,
    payment_settings JSON,
    feature_toggles JSON,
    security_settings JSON,
    audit_settings JSON,
    last_modified_by VARCHAR(36),
    last_modified_by_name VARCHAR(255),

    PRIMARY KEY (id),
    UNIQUE INDEX idx_settings_workspace (workspace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Workspace-specific appearance and configuration settings';

-- =====================================================
-- Workspace Activities
-- =====================================================
CREATE TABLE workspace_activities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    uid VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,


    activity_type VARCHAR(50) NOT NULL,
    actor_id VARCHAR(36) NOT NULL,
    actor_name VARCHAR(255) NOT NULL,
    description VARCHAR(500) NOT NULL,
    target_entity_type VARCHAR(50),
    target_entity_id VARCHAR(36),
    target_entity_name VARCHAR(255),
    context_data JSON,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    session_id VARCHAR(100),
    severity VARCHAR(10) NOT NULL DEFAULT 'INFO',
    workspace_id VARCHAR(36) NOT NULL,

    PRIMARY KEY (id),
    INDEX idx_workspace_activity_workspace_type (workspace_id, activity_type),
    INDEX idx_workspace_activity_created_at (created_at),
    INDEX idx_workspace_activity_actor (actor_id),
    INDEX idx_workspace_activity_target (target_entity_type, target_entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Audit log of workspace activities';
