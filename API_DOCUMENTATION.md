# 📚 Ampairs API Documentation Guide

## 🚀 **OpenAPI/Swagger Configuration**

Ampairs now provides comprehensive API documentation through **OpenAPI 3.0** and **Swagger UI** to help frontend developers understand and integrate with the available APIs.

---

## 🔗 **Accessing API Documentation**

### **Development Environment**
- **Swagger UI**: http://localhost:8080/swagger-ui
- **OpenAPI JSON**: http://localhost:8080/api-docs
- **OpenAPI YAML**: http://localhost:8080/api-docs.yaml

### **Production Environment**
- **Swagger UI**: https://api.ampairs.com/swagger-ui
- **OpenAPI JSON**: https://api.ampairs.com/api-docs

---

## 📋 **API Groups & Categories**

The API documentation is organized into logical groups for easier navigation:

### 🔓 **Public APIs** (`/public`)
**No workspace context required**
- **Authentication**: `/auth/v1/**` - Phone/OTP login, token management
- **User Management**: `/user/v1/**` - User profiles and settings
- **Workspace Discovery**: 
  - `GET /workspace/v1` - List user's workspaces
  - `POST /workspace/v1` - Create new workspace
  - `GET /workspace/v1/{workspaceId}` - Get workspace details
  - `GET /workspace/v1/check-slug/{slug}` - Check slug availability
  - `GET /workspace/v1/search` - Search workspaces

### 🏢 **Workspace Management** (`/workspace`)
**Requires authentication + workspace context**
- **Workspace Operations**: Complete workspace CRUD and management
- **Member Management**: User invitations, roles, and permissions
- **Workspace Settings**: Configuration and preferences

### 🧩 **Module Management** (`/modules`)
**Requires authentication + workspace context**
- **Module Discovery**: Browse available business modules
- **Installation Management**: Install/uninstall modules
- **Configuration Control**: Customize module settings
- **Activity Monitoring**: Usage analytics and performance tracking

### 💼 **Business Operations** (`/business`)
**Requires authentication + workspace context**
- **Customer Management**: `/customer/v1/**` - CRM functionality
- **Product Management**: `/product/v1/**` - Catalog and inventory
- **Order Processing**: `/order/v1/**` - Order workflows
- **Invoice Generation**: `/invoice/v1/**` - Billing and invoicing

---

## 🔐 **Authentication & Security**

### **JWT Bearer Token Authentication**

All APIs require JWT authentication except public endpoints.

#### **How to Authenticate:**

1. **Initialize Authentication**
   ```http
   POST /auth/v1/init
   Content-Type: application/json
   
   {
     "phone": "9591781662",
     "country_code": 91,
     "recaptcha_token": "your_recaptcha_token"
   }
   ```

2. **Verify OTP**
   ```http
   POST /auth/v1/verify
   Content-Type: application/json
   
   {
     "session_id": "LSQ20250804100456522TBFOQ8U44LIBLX",
     "otp": "123456",
     "auth_mode": "SMS"
   }
   ```

3. **Use Access Token**
   ```http
   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ...
   ```

### **Workspace Context Header**

Most business APIs require workspace context for multi-tenancy:

```http
X-Workspace-ID: WS_ABC123_XYZ789
```

#### **How to Get Workspace ID:**

1. **List Available Workspaces**
   ```http
   GET /workspace/v1
   Authorization: Bearer {your_jwt_token}
   ```

2. **Select Workspace**
   Use the `id` field from the workspace list response as the header value.

---

## 📖 **API Documentation Features**

### **🎯 Comprehensive Information**

Each API endpoint includes:

- **📝 Detailed Descriptions**: Business context and use cases
- **🔧 Parameter Documentation**: Required/optional parameters with examples
- **📊 Response Examples**: Complete JSON response samples
- **🚨 Error Handling**: All possible error codes and messages
- **🔐 Security Requirements**: Authentication and authorization details
- **💡 Usage Guidelines**: Best practices and integration tips

### **🎨 Enhanced Swagger UI Features**

- **🔍 Interactive Testing**: Test APIs directly from the documentation
- **📋 Request/Response Examples**: Copy-paste ready code samples
- **🏷️ Organized by Tags**: Logical grouping of related endpoints  
- **🔍 Search & Filter**: Quick navigation to specific APIs
- **📱 Responsive Design**: Works on desktop and mobile devices

### **🌟 Example Documentation Structure**

```yaml
paths:
  /workspace/v1/modules:
    get:
      tags:
        - "Module Management"
      summary: "Get Workspace Module Overview"
      description: |
        ## 📊 Get Comprehensive Module Information
        
        Retrieves overview of workspace's current module configuration.
        
        ### Use Cases:
        - Dashboard display and health monitoring
        - Quick navigation to module actions
        - Audit trail for module activities
        
      security:
        - BearerAuth: []
        - WorkspaceContext: []
      responses:
        '200':
          description: "✅ Successfully retrieved module overview"
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ApiResponse'
              example:
                success: true
                data:
                  workspaceId: "WS_ABC123_XYZ789"
                  totalModules: 8
                  activeModules: 6
```

---

## 🛠️ **Frontend Integration Guide**

### **📱 Angular Integration Example**

```typescript
// api.service.ts
@Injectable()
export class ApiService {
  private baseUrl = 'http://localhost:8080';
  private token = this.getStoredToken();
  private workspaceId = this.getStoredWorkspaceId();

  constructor(private http: HttpClient) {}

  // Get available modules
  getModules(): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.baseUrl}/workspace/v1/modules`, {
      headers: {
        'Authorization': `Bearer ${this.token}`,
        'X-Workspace-ID': this.workspaceId
      }
    });
  }

  // Perform module action
  performModuleAction(moduleId: string, action: string): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(
      `${this.baseUrl}/workspace/v1/modules/${moduleId}/action?action=${action}`,
      {},
      {
        headers: {
          'Authorization': `Bearer ${this.token}`,
          'X-Workspace-ID': this.workspaceId
        }
      }
    );
  }
}
```

### **⚛️ React Integration Example**

```javascript
// api.js
const API_BASE_URL = 'http://localhost:8080';

class ApiClient {
  constructor() {
    this.token = localStorage.getItem('auth_token');
    this.workspaceId = localStorage.getItem('workspace_id');
  }

  async getModules() {
    const response = await fetch(`${API_BASE_URL}/workspace/v1/modules`, {
      headers: {
        'Authorization': `Bearer ${this.token}`,
        'X-Workspace-ID': this.workspaceId,
        'Content-Type': 'application/json'
      }
    });
    return response.json();
  }

  async performModuleAction(moduleId, action) {
    const response = await fetch(
      `${API_BASE_URL}/workspace/v1/modules/${moduleId}/action?action=${action}`,
      {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${this.token}`,
          'X-Workspace-ID': this.workspaceId,
          'Content-Type': 'application/json'
        }
      }
    );
    return response.json();
  }
}

export default new ApiClient();
```

### **📱 Kotlin Multiplatform Integration**

```kotlin
// ApiService.kt
class ApiService {
    private val baseUrl = "http://localhost:8080"
    private val httpClient = HttpClient()
    
    suspend fun getModules(token: String, workspaceId: String): ApiResponse<ModuleOverview> {
        return httpClient.get("$baseUrl/workspace/v1/modules") {
            headers {
                append("Authorization", "Bearer $token")
                append("X-Workspace-ID", workspaceId)
            }
        }.body()
    }
    
    suspend fun performModuleAction(
        token: String, 
        workspaceId: String, 
        moduleId: String, 
        action: String
    ): ApiResponse<ActionResult> {
        return httpClient.post("$baseUrl/workspace/v1/modules/$moduleId/action") {
            headers {
                append("Authorization", "Bearer $token")
                append("X-Workspace-ID", workspaceId)
            }
            parameter("action", action)
        }.body()
    }
}
```

---

## 🔧 **Configuration Details**

### **SpringDoc Configuration**

The OpenAPI configuration is defined in:
- **Main Config**: `ampairs_service/src/main/kotlin/com/ampairs/config/OpenApiConfiguration.kt`
- **Properties**: `ampairs_service/src/main/resources/application.yml`

### **Key Configuration Options**

```yaml
springdoc:
  api-docs:
    path: /api-docs           # OpenAPI JSON endpoint
    enabled: true
  swagger-ui:
    path: /swagger-ui         # Swagger UI endpoint
    enabled: true
    operationsSorter: alpha   # Sort operations alphabetically
    tagsSorter: alpha         # Sort tags alphabetically
    displayRequestDuration: true
    defaultModelsExpandDepth: 1
    docExpansion: none
  show-actuator: false
  group-configs:              # API groupings
    - group: 'public'
      displayName: 'Public APIs'
    - group: 'workspace'
      displayName: 'Workspace Management'
    - group: 'modules'
      displayName: 'Module Management'
    - group: 'business'
      displayName: 'Business Operations'
```

---

## 🌟 **Best Practices for Frontend Developers**

### ✅ **Do's**

1. **Always Include Required Headers**
   - `Authorization: Bearer {token}` for all authenticated endpoints
   - `X-Workspace-ID: {workspace_id}` for workspace-scoped operations

2. **Handle Error Responses Properly**
   ```javascript
   if (!response.success) {
     console.error('API Error:', response.error.message);
     // Handle specific error codes
     switch (response.error.code) {
       case 'TOKEN_EXPIRED':
         // Redirect to login or refresh token
         break;
       case 'ACCESS_DENIED':
         // Show access denied message
         break;
     }
   }
   ```

3. **Use Standardized Response Structure**
   ```typescript
   interface ApiResponse<T> {
     success: boolean;
     data: T | null;
     error: {
       code: string;
       message: string;
       details?: string;
       module?: string;
     } | null;
     timestamp: string;
     path?: string;
   }
   ```

4. **Implement Proper Loading States**
   ```javascript
   const [loading, setLoading] = useState(false);
   
   const fetchModules = async () => {
     setLoading(true);
     try {
       const result = await apiClient.getModules();
       // Handle success
     } catch (error) {
       // Handle error
     } finally {
       setLoading(false);
     }
   };
   ```

### ❌ **Don'ts**

1. **Don't hardcode authentication tokens**
2. **Don't ignore error responses**
3. **Don't forget workspace context headers for business APIs**
4. **Don't assume API responses without checking success flag**

---

## 🎯 **Quick Start Checklist**

- [ ] **Access Swagger UI**: Visit http://localhost:8080/swagger-ui
- [ ] **Explore API Groups**: Check Public, Workspace, Module, and Business APIs
- [ ] **Test Authentication Flow**: Try `/auth/v1/init` and `/auth/v1/verify`
- [ ] **Get Workspace List**: Call `GET /workspace/v1` with JWT token
- [ ] **Test Module APIs**: Use workspace context header for module operations
- [ ] **Review Response Examples**: Understand the standardized response format
- [ ] **Implement Error Handling**: Handle all documented error codes
- [ ] **Set Up Headers**: Configure JWT and workspace context headers properly

---

## 🆘 **Support & Troubleshooting**

### **Common Issues**

1. **403 Forbidden - Workspace header required**
   - **Solution**: Add `X-Workspace-ID` header to workspace-scoped requests

2. **401 Unauthorized - Token invalid**
   - **Solution**: Check JWT token format and expiration, refresh if needed

3. **API endpoint not found**
   - **Solution**: Verify the correct API version and path in Swagger UI

### **Getting Help**

- **📖 API Documentation**: http://localhost:8080/swagger-ui
- **🔧 Configuration**: Check `application.yml` and `OpenApiConfiguration.kt`
- **🐛 Issues**: Report problems with detailed error messages and request examples

---

**🎉 The Ampairs API documentation is now ready to help frontend developers build amazing applications with our comprehensive business management platform!**