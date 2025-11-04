-- Add retail management modules to master_modules table
-- These modules support the unified retail management platform

-- Product Management Module
INSERT INTO master_modules (
    id, module_code, name, description, tagline, category, status, required_tier, required_role, 
    complexity, version, business_relevance, configuration, ui_metadata, provider, 
    support_email, documentation_url, size_mb, install_count, rating, rating_count, 
    featured, display_order, active, release_notes, created_at, updated_at
) VALUES (
    'MMD_PRODUCT_MGMT_001', 'product-management', 'Product Catalog Management', 
    'Comprehensive product catalog management with inventory tracking, pricing, and categorization. Supports multiple business types including retail, wholesale, and specialized stores like jewelry and hardware.',
    'Manage your complete product catalog with advanced inventory features',
    'COMMERCE', 'ACTIVE', 'STARTER', 'MANAGER', 'STANDARD', '1.0.0',
    '[
        {"businessType": "RETAIL", "relevanceScore": 10, "isEssential": true, "recommendationReason": "Core functionality for product catalog"},
        {"businessType": "WHOLESALE", "relevanceScore": 10, "isEssential": true, "recommendationReason": "Essential for bulk product management"},
        {"businessType": "KIRANA", "relevanceScore": 9, "isEssential": true, "recommendationReason": "Manage local store inventory"},
        {"businessType": "JEWELRY", "relevanceScore": 10, "isEssential": true, "recommendationReason": "Track precious metals and designs"},
        {"businessType": "HARDWARE", "relevanceScore": 9, "isEssential": true, "recommendationReason": "Manage construction supplies"}
    ]',
    '{
        "requiredPermissions": ["PRODUCT_READ", "PRODUCT_WRITE"],
        "optionalPermissions": ["PRODUCT_DELETE", "INVENTORY_MANAGE"],
        "defaultEnabled": true,
        "dependencies": [],
        "conflictsWith": []
    }',
    '{
        "icon": "inventory",
        "primaryColor": "#4CAF50",
        "tags": ["product", "catalog", "inventory", "retail"],
        "keywords": ["product management", "inventory", "catalog", "stock"]
    }',
    'Ampairs', 'support@ampairs.com', 'https://docs.ampairs.com/modules/product-management',
    25, 0, 0.0, 0, true, 10, true, 'Initial release with core product management features',
    NOW(), NOW()
);

-- Inventory Management Module  
INSERT INTO master_modules (
    id, module_code, name, description, tagline, category, status, required_tier, required_role,
    complexity, version, business_relevance, configuration, ui_metadata, provider,
    support_email, documentation_url, size_mb, install_count, rating, rating_count,
    featured, display_order, active, release_notes, created_at, updated_at
) VALUES (
    'MMD_INVENTORY_001', 'inventory-management', 'Advanced Inventory Control',
    'Real-time inventory management with stock tracking, low stock alerts, movement history, and multi-location support. Includes automatic stock reservations for orders.',
    'Keep perfect track of your stock levels and movements',
    'OPERATIONS', 'ACTIVE', 'STARTER', 'EMPLOYEE', 'STANDARD', '1.0.0',
    '[
        {"businessType": "RETAIL", "relevanceScore": 10, "isEssential": true, "recommendationReason": "Critical for stock management"},
        {"businessType": "WHOLESALE", "relevanceScore": 10, "isEssential": true, "recommendationReason": "Manage bulk inventory"},
        {"businessType": "KIRANA", "relevanceScore": 8, "isEssential": false, "recommendationReason": "Track local store stock"},
        {"businessType": "JEWELRY", "relevanceScore": 9, "isEssential": true, "recommendationReason": "Track valuable inventory"},
        {"businessType": "HARDWARE", "relevanceScore": 9, "isEssential": true, "recommendationReason": "Manage large inventory"}
    ]',
    '{
        "requiredPermissions": ["INVENTORY_READ", "INVENTORY_WRITE"],
        "optionalPermissions": ["INVENTORY_ADJUST", "MOVEMENT_HISTORY"],
        "defaultEnabled": true,
        "dependencies": ["product-management"],
        "conflictsWith": []
    }',
    '{
        "icon": "warehouse",
        "primaryColor": "#FF9800",
        "tags": ["inventory", "stock", "warehouse", "tracking"],
        "keywords": ["inventory management", "stock tracking", "warehouse"]
    }',
    'Ampairs', 'support@ampairs.com', 'https://docs.ampairs.com/modules/inventory',
    20, 0, 0.0, 0, true, 20, true, 'Real-time inventory tracking and management',
    NOW(), NOW()
);

-- Order Management Module
INSERT INTO master_modules (
    id, module_code, name, description, tagline, category, status, required_tier, required_role,
    complexity, version, business_relevance, configuration, ui_metadata, provider,
    support_email, documentation_url, size_mb, install_count, rating, rating_count,
    featured, display_order, active, release_notes, created_at, updated_at
) VALUES (
    'MMD_ORDER_MGMT_001', 'order-management', 'Sales Order Processing',
    'Complete order management system with status workflows, inventory integration, and customer tracking. Supports draft orders, confirmations, and fulfillment tracking.',
    'Streamline your entire order processing workflow',
    'COMMERCE', 'ACTIVE', 'STARTER', 'EMPLOYEE', 'STANDARD', '1.0.0',
    '[
        {"businessType": "RETAIL", "relevanceScore": 10, "isEssential": true, "recommendationReason": "Core sales functionality"},
        {"businessType": "WHOLESALE", "relevanceScore": 10, "isEssential": true, "recommendationReason": "Manage bulk orders"},
        {"businessType": "KIRANA", "relevanceScore": 8, "isEssential": false, "recommendationReason": "Track customer orders"},
        {"businessType": "JEWELRY", "relevanceScore": 9, "isEssential": true, "recommendationReason": "Custom order management"},
        {"businessType": "HARDWARE", "relevanceScore": 9, "isEssential": true, "recommendationReason": "Contractor order tracking"}
    ]',
    '{
        "requiredPermissions": ["ORDER_READ", "ORDER_WRITE"],
        "optionalPermissions": ["ORDER_DELETE", "ORDER_STATUS_CHANGE"],
        "defaultEnabled": true,
        "dependencies": ["product-management", "customer-management"],
        "conflictsWith": []
    }',
    '{
        "icon": "shopping_cart",
        "primaryColor": "#2196F3",
        "tags": ["orders", "sales", "processing", "workflow"],
        "keywords": ["order management", "sales", "processing", "fulfillment"]
    }',
    'Ampairs', 'support@ampairs.com', 'https://docs.ampairs.com/modules/order-management',
    30, 0, 0.0, 0, true, 30, true, 'Complete order processing with workflow management',
    NOW(), NOW()
);

-- Customer Management Module
INSERT INTO master_modules (
    id, module_code, name, description, tagline, category, status, required_tier, required_role,
    complexity, version, business_relevance, configuration, ui_metadata, provider,
    support_email, documentation_url, size_mb, install_count, rating, rating_count,
    featured, display_order, active, release_notes, created_at, updated_at
) VALUES (
    'MMD_CUSTOMER_MGMT_001', 'customer-management', 'Customer Relationship Management',
    'Comprehensive customer management with profiles, contact information, GST details, credit limits, and transaction history. Supports both individual and business customers.',
    'Build stronger relationships with your customers',
    'CUSTOMER_RELATIONS', 'ACTIVE', 'FREE', 'EMPLOYEE', 'STANDARD', '1.0.0',
    '[
        {"businessType": "RETAIL", "relevanceScore": 9, "isEssential": true, "recommendationReason": "Customer database management"},
        {"businessType": "WHOLESALE", "relevanceScore": 10, "isEssential": true, "recommendationReason": "B2B customer tracking"},
        {"businessType": "KIRANA", "relevanceScore": 8, "isEssential": false, "recommendationReason": "Local customer records"},
        {"businessType": "JEWELRY", "relevanceScore": 9, "isEssential": true, "recommendationReason": "High-value customer tracking"},
        {"businessType": "HARDWARE", "relevanceScore": 9, "isEssential": true, "recommendationReason": "Contractor relationships"}
    ]',
    '{
        "requiredPermissions": ["CUSTOMER_READ", "CUSTOMER_WRITE"],
        "optionalPermissions": ["CUSTOMER_DELETE", "TRANSACTION_HISTORY"],
        "defaultEnabled": true,
        "dependencies": [],
        "conflictsWith": []
    }',
    '{
        "icon": "people",
        "primaryColor": "#9C27B0",
        "tags": ["customers", "crm", "contacts", "relationships"],
        "keywords": ["customer management", "crm", "contacts", "relationships"]
    }',
    'Ampairs', 'support@ampairs.com', 'https://docs.ampairs.com/modules/customer-management',
    20, 0, 0.0, 0, true, 40, true, 'Complete customer relationship management system',
    NOW(), NOW()
);

-- Invoice Generation Module
INSERT INTO master_modules (
    id, module_code, name, description, tagline, category, status, required_tier, required_role,
    complexity, version, business_relevance, configuration, ui_metadata, provider,
    support_email, documentation_url, size_mb, install_count, rating, rating_count,
    featured, display_order, active, release_notes, created_at, updated_at
) VALUES (
    'MMD_INVOICE_GEN_001', 'invoice-generation', 'Professional Invoice Management',
    'Generate professional invoices with GST compliance, payment tracking, and PDF generation. Supports automatic invoice creation from orders and manual invoice creation.',
    'Create professional invoices with GST compliance',
    'FINANCE', 'ACTIVE', 'STARTER', 'MANAGER', 'STANDARD', '1.0.0',
    '[
        {"businessType": "RETAIL", "relevanceScore": 10, "isEssential": true, "recommendationReason": "GST compliant billing"},
        {"businessType": "WHOLESALE", "relevanceScore": 10, "isEssential": true, "recommendationReason": "B2B invoice generation"},
        {"businessType": "KIRANA", "relevanceScore": 7, "isEssential": false, "recommendationReason": "Simple billing needs"},
        {"businessType": "JEWELRY", "relevanceScore": 10, "isEssential": true, "recommendationReason": "High-value transactions"},
        {"businessType": "HARDWARE", "relevanceScore": 9, "isEssential": true, "recommendationReason": "Contractor billing"}
    ]',
    '{
        "requiredPermissions": ["INVOICE_READ", "INVOICE_WRITE"],
        "optionalPermissions": ["INVOICE_DELETE", "PAYMENT_RECORD", "PDF_GENERATE"],
        "defaultEnabled": true,
        "dependencies": ["order-management", "customer-management", "tax-management"],
        "conflictsWith": []
    }',
    '{
        "icon": "receipt",
        "primaryColor": "#E91E63",
        "tags": ["invoices", "billing", "gst", "payments"],
        "keywords": ["invoice generation", "billing", "gst", "payments", "pdf"]
    }',
    'Ampairs', 'support@ampairs.com', 'https://docs.ampairs.com/modules/invoice-generation',
    25, 0, 0.0, 0, true, 50, true, 'Professional invoice generation with GST compliance',
    NOW(), NOW()
);

-- Tax Management Module
INSERT INTO master_modules (
    id, module_code, name, description, tagline, category, status, required_tier, required_role,
    complexity, version, business_relevance, configuration, ui_metadata, provider,
    support_email, documentation_url, size_mb, install_count, rating, rating_count,
    featured, display_order, active, release_notes, created_at, updated_at
) VALUES (
    'MMD_TAX_MGMT_001', 'tax-management', 'GST & Tax Code Management',
    'Comprehensive tax management with GST compliance, tax code configuration, and automated tax calculations. Supports SGST, CGST, IGST calculations based on location.',
    'Ensure GST compliance with automated tax calculations',
    'FINANCE', 'ACTIVE', 'STARTER', 'MANAGER', 'ADVANCED', '1.0.0',
    '[
        {"businessType": "RETAIL", "relevanceScore": 10, "isEssential": true, "recommendationReason": "GST compliance required"},
        {"businessType": "WHOLESALE", "relevanceScore": 10, "isEssential": true, "recommendationReason": "Complex tax calculations"},
        {"businessType": "KIRANA", "relevanceScore": 8, "isEssential": true, "recommendationReason": "Simple GST compliance"},
        {"businessType": "JEWELRY", "relevanceScore": 10, "isEssential": true, "recommendationReason": "Precious metals tax rates"},
        {"businessType": "HARDWARE", "relevanceScore": 9, "isEssential": true, "recommendationReason": "Construction materials tax"}
    ]',
    '{
        "requiredPermissions": ["TAX_READ", "TAX_WRITE"],
        "optionalPermissions": ["TAX_CALCULATE", "TAX_REPORT"],
        "defaultEnabled": true,
        "dependencies": [],
        "conflictsWith": []
    }',
    '{
        "icon": "calculate",
        "primaryColor": "#FF5722",
        "tags": ["tax", "gst", "compliance", "calculations"],
        "keywords": ["tax management", "gst", "compliance", "sgst", "cgst", "igst"]
    }',
    'Ampairs', 'support@ampairs.com', 'https://docs.ampairs.com/modules/tax-management',
    15, 0, 0.0, 0, true, 60, true, 'GST compliance and tax calculation engine',
    NOW(), NOW()
);

-- Analytics Module
INSERT INTO master_modules (
    id, module_code, name, description, tagline, category, status, required_tier, required_role,
    complexity, version, business_relevance, configuration, ui_metadata, provider,
    support_email, documentation_url, size_mb, install_count, rating, rating_count,
    featured, display_order, active, release_notes, created_at, updated_at
) VALUES (
    'MMD_ANALYTICS_001', 'retail-analytics', 'Business Intelligence & Reports',
    'Comprehensive business analytics with sales reports, inventory analysis, customer insights, and performance dashboards. Real-time metrics and trend analysis.',
    'Get actionable insights from your business data',
    'ANALYTICS', 'ACTIVE', 'PROFESSIONAL', 'MANAGER', 'ADVANCED', '1.0.0',
    '[
        {"businessType": "RETAIL", "relevanceScore": 8, "isEssential": false, "recommendationReason": "Business performance insights"},
        {"businessType": "WHOLESALE", "relevanceScore": 9, "isEssential": false, "recommendationReason": "Bulk sales analysis"},
        {"businessType": "KIRANA", "relevanceScore": 6, "isEssential": false, "recommendationReason": "Simple reporting needs"},
        {"businessType": "JEWELRY", "relevanceScore": 8, "isEssential": false, "recommendationReason": "High-value analytics"},
        {"businessType": "HARDWARE", "relevanceScore": 7, "isEssential": false, "recommendationReason": "Seasonal analysis"}
    ]',
    '{
        "requiredPermissions": ["ANALYTICS_READ"],
        "optionalPermissions": ["REPORT_EXPORT", "DASHBOARD_CONFIGURE"],
        "defaultEnabled": false,
        "dependencies": ["order-management", "product-management", "customer-management"],
        "conflictsWith": []
    }',
    '{
        "icon": "analytics",
        "primaryColor": "#673AB7",
        "tags": ["analytics", "reports", "insights", "dashboard"],
        "keywords": ["business intelligence", "reports", "analytics", "dashboard", "metrics"]
    }',
    'Ampairs', 'support@ampairs.com', 'https://docs.ampairs.com/modules/analytics',
    40, 0, 0.0, 0, false, 70, true, 'Business intelligence and reporting platform',
    NOW(), NOW()
);

-- Notifications Module
INSERT INTO master_modules (
    id, module_code, name, description, tagline, category, status, required_tier, required_role,
    complexity, version, business_relevance, configuration, ui_metadata, provider,
    support_email, documentation_url, size_mb, install_count, rating, rating_count,
    featured, display_order, active, release_notes, created_at, updated_at
) VALUES (
    'MMD_NOTIFICATIONS_001', 'smart-notifications', 'Smart Business Notifications',
    'Intelligent notification system for business events like low stock alerts, payment reminders, order updates, and customer communications via email, SMS, and push notifications.',
    'Stay informed with intelligent business alerts',
    'COMMUNICATION', 'ACTIVE', 'FREE', 'EMPLOYEE', 'STANDARD', '1.0.0',
    '[
        {"businessType": "RETAIL", "relevanceScore": 7, "isEssential": false, "recommendationReason": "Stay updated on business events"},
        {"businessType": "WHOLESALE", "relevanceScore": 8, "isEssential": false, "recommendationReason": "Critical business alerts"},
        {"businessType": "KIRANA", "relevanceScore": 6, "isEssential": false, "recommendationReason": "Simple alerts"},
        {"businessType": "JEWELRY", "relevanceScore": 7, "isEssential": false, "recommendationReason": "Security alerts"},
        {"businessType": "HARDWARE", "relevanceScore": 7, "isEssential": false, "recommendationReason": "Inventory alerts"}
    ]',
    '{
        "requiredPermissions": ["NOTIFICATION_READ"],
        "optionalPermissions": ["NOTIFICATION_SEND", "NOTIFICATION_CONFIGURE"],
        "defaultEnabled": true,
        "dependencies": [],
        "conflictsWith": []
    }',
    '{
        "icon": "notifications",
        "primaryColor": "#FFC107",
        "tags": ["notifications", "alerts", "communication", "reminders"],
        "keywords": ["notifications", "alerts", "email", "sms", "push notifications"]
    }',
    'Ampairs', 'support@ampairs.com', 'https://docs.ampairs.com/modules/notifications',
    10, 0, 0.0, 0, false, 80, true, 'Smart notification system for business events',
    NOW(), NOW()
);