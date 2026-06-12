# business module

Manages the business profile for a workspace — legal details, address, operating hours, and branding (logo + gallery images). One business profile per workspace. Tax identity/configuration lives in the tax module (`TaxConfiguration` / `/tax/v1/configurations`), not here.

## REST Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/business` | Create business profile (one-time setup) |
| GET | `/api/v1/business` | Get complete business profile |
| PUT | `/api/v1/business` | Update business profile |
| GET | `/api/v1/business/overview` | Dashboard summary |
| GET | `/api/v1/business/exists` | Check if profile exists |
| POST | `/api/v1/business/logo` | Upload logo (multipart) |
| GET | `/api/v1/business/logo` | Stream logo (full size) |
| GET | `/api/v1/business/logo/thumbnail` | Stream logo thumbnail (256×256) |
| DELETE | `/api/v1/business/logo` | Delete logo |
| POST | `/api/v1/business/images` | Upload gallery image |
| GET | `/api/v1/business/images` | List gallery images |
| GET | `/api/v1/business/images/{imageUid}` | Get image metadata |
| GET | `/api/v1/business/images/{imageUid}/file` | Stream image (full size) |
| GET | `/api/v1/business/images/{imageUid}/thumbnail` | Stream image thumbnail |
| PUT | `/api/v1/business/images/{imageUid}` | Update image metadata |
| POST | `/api/v1/business/images/{imageUid}/set-primary` | Set as primary gallery image |
| POST | `/api/v1/business/images/reorder` | Reorder gallery images |
| DELETE | `/api/v1/business/images/{imageUid}` | Delete gallery image |

## Key Entities

### Business

```kotlin
class Business : OwnableBaseDomain() {
    val name: String
    val businessType: BusinessType     // RETAIL, WHOLESALE, SERVICE, MANUFACTURING, ECOMMERCE
    val description: String?
    val ownerName: String?
    // Address
    val addressLine1: String?
    val addressLine2: String?
    val city: String?
    val state: String?
    val postalCode: String?
    val country: String?
    val latitude: Double?
    val longitude: Double?
    // Contact
    val phone: String?
    val email: String?
    val website: String?
    // Branding
    val logoUrl: String?
    val logoThumbnailUrl: String?
    // Tax identity/config moved to the tax module (TaxConfiguration / /tax/v1/configurations)
    val customAttributes: Map<String, Any>?
    // Locale
    val timezone: String            // IANA timezone e.g. "Asia/Kolkata"
    val currency: String            // ISO 4217 e.g. "INR"
    val language: String            // ISO 639-1 e.g. "en"
    val dateFormat: String?
    val timeFormat: String?
    // Hours
    val openingHours: String?       // HH:mm
    val closingHours: String?       // HH:mm
    val operatingDays: List<String>? // ["MON","TUE",...] JSON
}
```

### BusinessImage

```kotlin
class BusinessImage : OwnableBaseDomain() {
    val businessUid: String
    val imageUrl: String            // S3 object key (full size)
    val thumbnailUrl: String?       // S3 object key (256×256)
    val imageType: BusinessImageType  // LOGO, GALLERY, BANNER, STOREFRONT
    val title: String?
    val description: String?
    val altText: String?
    val isPrimary: Boolean
    val displayOrder: Int
    val active: Boolean
    val uploadedBy: String          // user UID
}
```

### BusinessType

| Value | Description |
|-------|-------------|
| `RETAIL` | Retail store / B2C |
| `WHOLESALE` | Wholesale / B2B |
| `SERVICE` | Service-based business |
| `MANUFACTURING` | Manufacturing facility |
| `ECOMMERCE` | Online-first store |

## Business Hours Format

```json
{
  "opening_hours": "09:00",
  "closing_hours": "21:00",
  "operating_days": ["MON", "TUE", "WED", "THU", "FRI", "SAT"]
}
```

## Database Migrations

| File | Description |
|------|-------------|
| `V1.0.12__create_businesses_table.sql` | businesses table |
| `V1.0.14__add_custom_attributes_to_businesses.sql` | JSON custom attributes |
| `V1.0.25__add_business_logo_and_images.sql` | logo + gallery image tables |

## Package Structure

```
com.ampairs.business
├── controller/     — BusinessController
├── exception/      — BusinessExceptionHandler, typed exceptions
├── model/
│   ├── Business.kt, BusinessImage.kt
│   ├── dto/        — BusinessResponse, BusinessCreateRequest, BusinessUpdateRequest,
│   │                  BusinessProfileResponse, BusinessOverviewResponse,
│   │                  BusinessImageDto
│   └── enums/      — BusinessType
├── repository/     — BusinessRepository, BusinessImageRepository
└── service/        — BusinessService, BusinessImageService
```
