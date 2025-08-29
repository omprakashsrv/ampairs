/**
 * Workspace Module Management Interfaces
 *
 * Comprehensive interfaces for module management functionality
 * including installation, configuration, and analytics.
 */

export interface WorkspaceModule {
  id: string;
  seq_id: number;
  workspace_id: string;
  master_module: MasterModule;
  status: WorkspaceModuleStatus;
  enabled: boolean;
  installed_version: string;
  installed_at: string;
  installed_by?: string;
  installed_by_name?: string;
  last_updated_at?: string;
  last_updated_by?: string;
  category_override?: string;
  display_order: number;
  settings: ModuleSettings;
  usage_metrics: ModuleUsageMetrics;
  user_preferences: UserModulePreferences[];
  license_info?: string;
  license_expires_at?: string;
  storage_used_mb: number;
  configuration_notes?: string;
  effective_name: string;
  effective_description: string;
  effective_icon: string;
  effective_color: string;
  effective_category: string;
  is_operational: boolean;
  has_valid_license: boolean;
  can_be_updated: boolean;
  needs_attention: boolean;
  health_score: number;
  engagement_level: number;
  is_popular: boolean;
  created_at: string;
  updated_at: string;
}

export interface MasterModule {
  id: string;
  seq_id: number;
  module_code: string;
  name: string;
  description?: string;
  tagline?: string;
  category: string;
  status: string;
  required_tier: string;
  required_role: string;
  complexity: string;
  version: string;
  business_relevance: BusinessRelevance[];
  configuration: ModuleConfiguration;
  ui_metadata: ModuleUIMetadata;
  provider: string;
  support_email?: string;
  documentation_url?: string;
  homepage_url?: string;
  setup_guide_url?: string;
  size_mb: number;
  install_count: number;
  rating: number;
  rating_count: number;
  featured: boolean;
  display_order: number;
  active: boolean;
  release_notes?: string;
  last_updated_at?: string;
  created_at: string;
  updated_at: string;
}

export interface ModuleSettings {
  [key: string]: any;
}

export interface ModuleUsageMetrics {
  daily_active_users?: number;
  monthly_access?: number;
  average_session_duration?: string;
  last_accessed?: string;
  total_operations?: number;
  error_count?: number;
}

export interface UserModulePreferences {
  user_id: string;
  preferences: { [key: string]: any };
  created_at: string;
  updated_at: string;
}

export interface BusinessRelevance {
  industry: string;
  use_case: string;
  priority: number;
}

export interface ModuleConfiguration {
  default_settings: { [key: string]: any };
  required_fields: string[];
  optional_fields: string[];
  validation_rules: { [key: string]: any };
}

export interface ModuleUIMetadata {
  icon: string;
  color: string;
  theme?: string;
  thumbnail_url?: string;
  screenshots?: string[];
  category_icon?: string;
  category_color?: string;
}

export enum WorkspaceModuleStatus {
  INSTALLED = 'INSTALLED',
  PENDING = 'PENDING',
  FAILED = 'FAILED',
  UPDATING = 'UPDATING',
  DISABLED = 'DISABLED',
  UNINSTALLING = 'UNINSTALLING'
}

export enum ModuleCategory {
  CUSTOMER_MANAGEMENT = 'CUSTOMER_MANAGEMENT',
  SALES_MANAGEMENT = 'SALES_MANAGEMENT',
  INVENTORY_MANAGEMENT = 'INVENTORY_MANAGEMENT',
  FINANCIAL_MANAGEMENT = 'FINANCIAL_MANAGEMENT',
  PROJECT_MANAGEMENT = 'PROJECT_MANAGEMENT',
  ANALYTICS_REPORTING = 'ANALYTICS_REPORTING',
  MARKETING_AUTOMATION = 'MARKETING_AUTOMATION',
  HUMAN_RESOURCES = 'HUMAN_RESOURCES',
  OPERATIONS_MANAGEMENT = 'OPERATIONS_MANAGEMENT',
  COMMUNICATION = 'COMMUNICATION',
  INTEGRATION = 'INTEGRATION',
  UTILITIES = 'UTILITIES'
}

export interface ModuleSearchResponse {
  modules: WorkspaceModule[];
  total_elements: number;
  total_pages: number;
  current_page: number;
  page_size: number;
  has_next: boolean;
  has_previous: boolean;
  search_metadata?: ModuleSearchMetadata;
}

export interface ModuleSearchMetadata {
  applied_filters: { [key: string]: any };
  available_categories: string[];
  available_statuses: string[];
  featured_count: number;
  installed_count: number;
  enabled_count: number;
}

export interface AvailableModulesResponse {
  available_modules: MasterModule[];
  recommended_modules: MasterModule[];
  essential_modules: MasterModule[];
  total_available: number;
  business_type?: string;
}

export interface ModuleDashboardResponse {
  total_modules: number;
  active_modules: number;
  inactive_modules: number;
  modules_needing_attention: number;
  modules_needing_updates: number;
  storage_usage_mb: number;
  most_used_modules: WorkspaceModule[];
  least_used_modules: WorkspaceModule[];
  category_distribution: { [key: string]: number };
  usage_trends: { [key: string]: any };
  health_overview: ModuleHealthOverview;
}

export interface ModuleHealthOverview {
  overall_health_score: number;
  healthy_modules: number;
  warning_modules: number;
  critical_modules: number;
  error_rate: number;
  user_satisfaction: number;
}

export interface ModuleActionRequest {
  action: string;
  parameters?: { [key: string]: any };
}

export interface ModuleActionResponse {
  module_id: string;
  action: string;
  workspace_id: string;
  success: boolean;
  message: string;
  action_details: {
    executed_at: string;
    executed_by: string;
    duration: string;
    affected_components: string[];
  };
  impact: {
    users_affected: number;
    data_changed: boolean;
    requires_restart: boolean;
    immediately_available: boolean;
  };
  next_steps: string[];
}

export interface BulkOperationRequest {
  operation: string;
  module_ids: string[];
  parameters?: { [key: string]: any };
}

export interface BulkOperationResponse {
  operation: string;
  total_requested: number;
  successful_operations: string[];
  failed_operations: FailedOperationDetail[];
  success_count: number;
  failure_count: number;
  warnings: string[];
}

export interface FailedOperationDetail {
  module_id: string;
  error_code: string;
  error_message: string;
  module_name?: string;
}

export interface ModuleInstallationRequest {
  master_module_id: string;
  display_order?: number;
  initial_settings?: { [key: string]: any };
  category_override?: string;
}

export interface ModuleConfigurationRequest {
  settings: { [key: string]: any };
  user_preferences?: { [key: string]: any };
  notes?: string;
}

export interface ModuleSearchRequest {
  query?: string;
  category?: string;
  status?: WorkspaceModuleStatus;
  enabled?: boolean;
  featured?: boolean;
  sort_by?: string;
  sort_direction?: 'asc' | 'desc';
  page?: number;
  size?: number;
}
