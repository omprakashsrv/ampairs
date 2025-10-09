package com.ampairs.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * OpenAPI 3.0 configuration for Ampairs Business Management System
 * 
 * Provides comprehensive API documentation including:
 * - JWT Bearer token authentication
 * - Workspace context header requirements
 * - Multi-tenant API structure
 * - Complete endpoint documentation
 */
@Configuration
class OpenApiConfiguration {

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(apiInfo())
            .servers(serverList())
            .components(securityComponents())
            .security(globalSecurityRequirements())
            .externalDocs(externalDocumentation())
    }

    private fun apiInfo(): Info {
        return Info()
            .title("Ampairs Business Management System API")
            .description("""
                ## Comprehensive Business Management Platform
                
                Ampairs provides a complete suite of business management tools including:
                
                ### 🏗️ **System Architecture**
                - **Backend**: Spring Boot REST API with Kotlin
                - **Web Frontend**: Angular with Material Design 3
                - **Mobile Apps**: Kotlin Multiplatform (Android/iOS/Desktop)
                
                ### 🔐 **Authentication & Authorization**
                - **JWT-based Authentication**: Secure token-based access
                - **Multi-Device Support**: Login from multiple devices simultaneously
                - **Role-Based Access Control**: Workspace-level permissions
                - **Multi-Tenant Architecture**: Complete workspace isolation
                
                ### 📱 **Core Business Modules**
                - **Authentication**: Phone/OTP login with multi-device support
                - **Workspace Management**: Multi-tenant workspace configuration
                - **Customer Management**: Comprehensive CRM functionality
                - **Product Catalog**: Product management with inventory tracking
                - **Order Processing**: Order creation and management workflows
                - **Invoice Generation**: GST-compliant invoicing with PDF generation
                
                ### 🔄 **API Usage Patterns**
                
                #### **Step 1: Authentication**
                ```http
                POST /auth/v1/init
                {
                  "phone": "9591781662",
                  "country_code": 91
                }
                
                POST /auth/v1/verify
                {
                  "session_id": "SESSION_ID",
                  "otp": "123456"
                }
                ```
                
                #### **Step 2: Workspace Selection**
                ```http
                GET /workspace/v1
                Authorization: Bearer {jwt_token}
                ```
                
                #### **Step 3: Workspace-Scoped Operations**
                ```http
                GET /workspace/v1/modules
                Authorization: Bearer {jwt_token}
                X-Workspace-ID: {workspace_uid}
                ```
                
                ### ⚡ **Key Features**
                - **Offline-First Mobile Apps**: Store5-based caching and synchronization
                - **Real-time Updates**: Live data synchronization across platforms
                - **Scalable Architecture**: Microservice-ready modular design
                - **Cloud Integration**: AWS S3, SNS, and other cloud services
                - **Multi-Platform**: Web, Android, iOS, and Desktop applications
                
                ### 📋 **API Standards**
                - **RESTful Design**: Standard HTTP methods and status codes
                - **JSON API**: Consistent request/response formats
                - **Pagination**: Standardized pagination for list endpoints
                - **Error Handling**: Comprehensive error response structure
                - **API Versioning**: URL-based versioning strategy
            """.trimIndent())
            .version("1.0.0")
            .contact(
                Contact()
                    .name("Ampairs Development Team")
                    .email("support@ampairs.com")
                    .url("https://ampairs.com")
            )
            .license(
                License()
                    .name("Proprietary License")
                    .url("https://ampairs.com/license")
            )
    }

    private fun serverList(): List<Server> {
        return listOf(
            Server()
                .url("http://localhost:8080")
                .description("Development Server"),
            Server()
                .url("https://api.ampairs.com")
                .description("Production Server"),
            Server()
                .url("https://staging-api.ampairs.com")
                .description("Staging Server")
        )
    }

    private fun securityComponents(): Components {
        return Components()
            .securitySchemes(
                mapOf(
                    "BearerAuth" to SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("""
                            ## JWT Bearer Token Authentication
                            
                            **How to obtain a token:**
                            1. Call `POST /auth/v1/init` with phone number
                            2. Call `POST /auth/v1/verify` with OTP
                            3. Use the returned `access_token` in Authorization header
                            
                            **Token Format:** `Bearer eyJhbGciOiJIUzI1NiJ9...`
                            
                            **Token Expiry:** 15 minutes (use refresh token to get new access token)
                        """.trimIndent()),
                    
                    "WorkspaceContext" to SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .`in`(SecurityScheme.In.HEADER)
                        .name("X-Workspace-ID")
                        .description("""
                            ## Workspace Context Header
                            
                            **Required for all workspace-scoped operations:**
                            - Customer management
                            - Product management
                            - Order processing
                            - Invoice generation
                            - Module management
                            
                            **How to get workspace ID:**
                            1. After authentication, call `GET /workspace/v1` to list available workspaces
                            2. Select a workspace and use its `id` field as the header value
                            
                            **Header Format:** `X-Workspace-ID: WORKSPACE_UID_HERE`
                            
                            **Multi-Tenancy:** This header ensures data isolation between workspaces
                        """.trimIndent())
                )
            )
    }

    private fun globalSecurityRequirements(): List<SecurityRequirement> {
        return listOf(
            SecurityRequirement()
                .addList("BearerAuth")
                .addList("WorkspaceContext")
        )
    }

    private fun externalDocumentation(): ExternalDocumentation {
        return ExternalDocumentation()
            .description("Ampairs API Documentation & Integration Guide")
            .url("https://docs.ampairs.com")
    }
}