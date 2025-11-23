# Business Images Implementation Guide for KMP App

This document provides instructions for implementing the Business Logo and Gallery Images feature in the Kotlin Multiplatform (KMP) mobile application.

## Overview

The Business Images feature includes:
1. **Business Logo** - Single logo image per business (512x512 max, with 256x256 thumbnail)
2. **Gallery Images** - Multiple images per business (up to 20, with thumbnails)

## API Endpoints

### Base URL
```
{API_BASE_URL}/api/v1/business
```

### Authentication
All endpoints require:
- `Authorization: Bearer {jwt_token}`
- `X-Workspace-ID: {workspace_id}`

---

## 1. Logo Endpoints

### Upload Logo
```http
POST /api/v1/business/logo
Content-Type: multipart/form-data

file: <image_file>  // JPEG, PNG, or WebP (max 10MB)
```

**Response:**
```json
{
  "success": true,
  "data": {
    "uid": "BUS...",
    "name": "My Business",
    "logo_url": "/api/v1/business/logo",
    "logo_thumbnail_url": "/api/v1/business/logo/thumbnail",
    // ... other business fields
  }
}
```

### Get Logo (Full Size)
```http
GET /api/v1/business/logo
```
**Response:** Image bytes (image/jpeg, image/png, or image/webp)

### Get Logo Thumbnail
```http
GET /api/v1/business/logo/thumbnail
```
**Response:** Image bytes (256x256)

### Delete Logo
```http
DELETE /api/v1/business/logo
```

---

## 2. Gallery Image Endpoints

### Upload Image
```http
POST /api/v1/business/images
Content-Type: multipart/form-data

file: <image_file>        // Required: JPEG, PNG, or WebP (max 10MB)
image_type: "GALLERY"     // Optional: GALLERY, STOREFRONT, INTERIOR, PRODUCT_SHOWCASE, TEAM, BANNER, CERTIFICATE, OTHER
title: "Store Front"      // Optional
description: "Main entrance" // Optional
alt_text: "Store front"   // Optional
is_primary: false         // Optional: Set as primary/featured image
```

**Response:**
```json
{
  "success": true,
  "data": {
    "uid": "BIM...",
    "business_id": "BUS...",
    "image_type": "STOREFRONT",
    "image_url": "/api/v1/business/images/BIM.../file",
    "thumbnail_url": "/api/v1/business/images/BIM.../thumbnail",
    "title": "Store Front",
    "description": "Main entrance",
    "alt_text": "Store front",
    "display_order": 1,
    "is_primary": false,
    "active": true,
    "original_filename": "storefront.jpg",
    "file_size": 245678,
    "width": 1920,
    "height": 1080,
    "content_type": "image/jpeg",
    "uploaded_at": "2025-01-15T10:30:00Z",
    "created_at": "2025-01-15T10:30:00Z",
    "updated_at": "2025-01-15T10:30:00Z"
  }
}
```

### Get All Images
```http
GET /api/v1/business/images
GET /api/v1/business/images?image_type=STOREFRONT  // Filter by type
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "uid": "BIM...",
      "image_type": "GALLERY",
      "image_url": "/api/v1/business/images/BIM.../file",
      "thumbnail_url": "/api/v1/business/images/BIM.../thumbnail",
      "title": "Image 1",
      "display_order": 0,
      "is_primary": true
      // ...
    },
    // ... more images
  ]
}
```

### Get Image Details
```http
GET /api/v1/business/images/{imageUid}
```

### Get Image File (Full Size)
```http
GET /api/v1/business/images/{imageUid}/file
```
**Response:** Image bytes

### Get Image Thumbnail
```http
GET /api/v1/business/images/{imageUid}/thumbnail
```
**Response:** Image bytes (400x400)

### Update Image Metadata
```http
PUT /api/v1/business/images/{imageUid}
Content-Type: application/json

{
  "title": "Updated Title",
  "description": "Updated description",
  "alt_text": "Updated alt text",
  "image_type": "INTERIOR"
}
```

### Set Image as Primary
```http
POST /api/v1/business/images/{imageUid}/set-primary
```

### Reorder Images
```http
POST /api/v1/business/images/reorder
Content-Type: application/json

{
  "image_uids": ["BIM001", "BIM003", "BIM002"]  // New order
}
```

### Delete Image
```http
DELETE /api/v1/business/images/{imageUid}
```

---

## 3. KMP Implementation

### 3.1 Data Models

```kotlin
// shared/src/commonMain/kotlin/com/ampairs/app/data/model/BusinessImage.kt

@Serializable
data class BusinessImageResponse(
    val uid: String,
    @SerialName("business_id") val businessId: String,
    @SerialName("image_type") val imageType: String,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("thumbnail_url") val thumbnailUrl: String?,
    val title: String?,
    val description: String?,
    @SerialName("alt_text") val altText: String?,
    @SerialName("display_order") val displayOrder: Int,
    @SerialName("is_primary") val isPrimary: Boolean,
    val active: Boolean,
    @SerialName("original_filename") val originalFilename: String?,
    @SerialName("file_size") val fileSize: Long?,
    val width: Int?,
    val height: Int?,
    @SerialName("content_type") val contentType: String?,
    @SerialName("uploaded_by") val uploadedBy: String?,
    @SerialName("uploaded_at") val uploadedAt: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class UpdateBusinessImageRequest(
    val title: String? = null,
    val description: String? = null,
    @SerialName("alt_text") val altText: String? = null,
    @SerialName("image_type") val imageType: String? = null
)

@Serializable
data class ReorderImagesRequest(
    @SerialName("image_uids") val imageUids: List<String>
)

enum class BusinessImageType {
    GALLERY,
    STOREFRONT,
    INTERIOR,
    PRODUCT_SHOWCASE,
    TEAM,
    BANNER,
    CERTIFICATE,
    OTHER
}
```

### 3.2 API Service

```kotlin
// shared/src/commonMain/kotlin/com/ampairs/app/data/api/BusinessImageApi.kt

interface BusinessImageApi {

    // Logo endpoints
    suspend fun uploadLogo(imageData: ByteArray, fileName: String, contentType: String): ApiResponse<BusinessResponse>
    suspend fun getLogoUrl(): String  // Returns full URL to logo endpoint
    suspend fun getLogoThumbnailUrl(): String
    suspend fun deleteLogo(): ApiResponse<BusinessResponse>

    // Gallery image endpoints
    suspend fun uploadImage(
        imageData: ByteArray,
        fileName: String,
        contentType: String,
        imageType: BusinessImageType = BusinessImageType.GALLERY,
        title: String? = null,
        description: String? = null,
        altText: String? = null,
        isPrimary: Boolean = false
    ): ApiResponse<BusinessImageResponse>

    suspend fun getImages(imageType: BusinessImageType? = null): ApiResponse<List<BusinessImageResponse>>
    suspend fun getImageDetails(imageUid: String): ApiResponse<BusinessImageResponse>
    suspend fun getImageUrl(imageUid: String): String
    suspend fun getImageThumbnailUrl(imageUid: String): String
    suspend fun updateImage(imageUid: String, request: UpdateBusinessImageRequest): ApiResponse<BusinessImageResponse>
    suspend fun setAsPrimary(imageUid: String): ApiResponse<BusinessImageResponse>
    suspend fun reorderImages(imageUids: List<String>): ApiResponse<String>
    suspend fun deleteImage(imageUid: String): ApiResponse<String>
}
```

### 3.3 Ktor Implementation

```kotlin
// shared/src/commonMain/kotlin/com/ampairs/app/data/api/BusinessImageApiImpl.kt

class BusinessImageApiImpl(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val authProvider: AuthProvider
) : BusinessImageApi {

    private val businessUrl = "$baseUrl/api/v1/business"

    override suspend fun uploadLogo(
        imageData: ByteArray,
        fileName: String,
        contentType: String
    ): ApiResponse<BusinessResponse> {
        return httpClient.submitFormWithBinaryData(
            url = "$businessUrl/logo",
            formData = formData {
                append("file", imageData, Headers.build {
                    append(HttpHeaders.ContentType, contentType)
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                })
            }
        ) {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${authProvider.getToken()}")
                append("X-Workspace-ID", authProvider.getWorkspaceId())
            }
        }.body()
    }

    override suspend fun getLogoUrl(): String {
        return "$businessUrl/logo"
    }

    override suspend fun getLogoThumbnailUrl(): String {
        return "$businessUrl/logo/thumbnail"
    }

    override suspend fun deleteLogo(): ApiResponse<BusinessResponse> {
        return httpClient.delete("$businessUrl/logo") {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${authProvider.getToken()}")
                append("X-Workspace-ID", authProvider.getWorkspaceId())
            }
        }.body()
    }

    override suspend fun uploadImage(
        imageData: ByteArray,
        fileName: String,
        contentType: String,
        imageType: BusinessImageType,
        title: String?,
        description: String?,
        altText: String?,
        isPrimary: Boolean
    ): ApiResponse<BusinessImageResponse> {
        return httpClient.submitFormWithBinaryData(
            url = "$businessUrl/images",
            formData = formData {
                append("file", imageData, Headers.build {
                    append(HttpHeaders.ContentType, contentType)
                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                })
                append("image_type", imageType.name)
                title?.let { append("title", it) }
                description?.let { append("description", it) }
                altText?.let { append("alt_text", it) }
                append("is_primary", isPrimary.toString())
            }
        ) {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${authProvider.getToken()}")
                append("X-Workspace-ID", authProvider.getWorkspaceId())
            }
        }.body()
    }

    override suspend fun getImages(imageType: BusinessImageType?): ApiResponse<List<BusinessImageResponse>> {
        return httpClient.get("$businessUrl/images") {
            imageType?.let { parameter("image_type", it.name) }
            headers {
                append(HttpHeaders.Authorization, "Bearer ${authProvider.getToken()}")
                append("X-Workspace-ID", authProvider.getWorkspaceId())
            }
        }.body()
    }

    override suspend fun getImageDetails(imageUid: String): ApiResponse<BusinessImageResponse> {
        return httpClient.get("$businessUrl/images/$imageUid") {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${authProvider.getToken()}")
                append("X-Workspace-ID", authProvider.getWorkspaceId())
            }
        }.body()
    }

    override suspend fun getImageUrl(imageUid: String): String {
        return "$businessUrl/images/$imageUid/file"
    }

    override suspend fun getImageThumbnailUrl(imageUid: String): String {
        return "$businessUrl/images/$imageUid/thumbnail"
    }

    override suspend fun updateImage(
        imageUid: String,
        request: UpdateBusinessImageRequest
    ): ApiResponse<BusinessImageResponse> {
        return httpClient.put("$businessUrl/images/$imageUid") {
            contentType(ContentType.Application.Json)
            setBody(request)
            headers {
                append(HttpHeaders.Authorization, "Bearer ${authProvider.getToken()}")
                append("X-Workspace-ID", authProvider.getWorkspaceId())
            }
        }.body()
    }

    override suspend fun setAsPrimary(imageUid: String): ApiResponse<BusinessImageResponse> {
        return httpClient.post("$businessUrl/images/$imageUid/set-primary") {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${authProvider.getToken()}")
                append("X-Workspace-ID", authProvider.getWorkspaceId())
            }
        }.body()
    }

    override suspend fun reorderImages(imageUids: List<String>): ApiResponse<String> {
        return httpClient.post("$businessUrl/images/reorder") {
            contentType(ContentType.Application.Json)
            setBody(ReorderImagesRequest(imageUids))
            headers {
                append(HttpHeaders.Authorization, "Bearer ${authProvider.getToken()}")
                append("X-Workspace-ID", authProvider.getWorkspaceId())
            }
        }.body()
    }

    override suspend fun deleteImage(imageUid: String): ApiResponse<String> {
        return httpClient.delete("$businessUrl/images/$imageUid") {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${authProvider.getToken()}")
                append("X-Workspace-ID", authProvider.getWorkspaceId())
            }
        }.body()
    }
}
```

### 3.4 Repository

```kotlin
// shared/src/commonMain/kotlin/com/ampairs/app/data/repository/BusinessImageRepository.kt

class BusinessImageRepository(
    private val api: BusinessImageApi
) {
    // Logo operations
    suspend fun uploadLogo(imageData: ByteArray, fileName: String, contentType: String): Result<BusinessResponse> {
        return runCatching { api.uploadLogo(imageData, fileName, contentType).data!! }
    }

    fun getLogoUrl(): String = api.getLogoUrl()
    fun getLogoThumbnailUrl(): String = api.getLogoThumbnailUrl()

    suspend fun deleteLogo(): Result<BusinessResponse> {
        return runCatching { api.deleteLogo().data!! }
    }

    // Gallery operations
    suspend fun uploadImage(
        imageData: ByteArray,
        fileName: String,
        contentType: String,
        imageType: BusinessImageType = BusinessImageType.GALLERY,
        title: String? = null,
        description: String? = null,
        isPrimary: Boolean = false
    ): Result<BusinessImageResponse> {
        return runCatching {
            api.uploadImage(imageData, fileName, contentType, imageType, title, description, title, isPrimary).data!!
        }
    }

    suspend fun getImages(imageType: BusinessImageType? = null): Result<List<BusinessImageResponse>> {
        return runCatching { api.getImages(imageType).data!! }
    }

    suspend fun getImageDetails(imageUid: String): Result<BusinessImageResponse> {
        return runCatching { api.getImageDetails(imageUid).data!! }
    }

    fun getImageUrl(imageUid: String): String = api.getImageUrl(imageUid)
    fun getImageThumbnailUrl(imageUid: String): String = api.getImageThumbnailUrl(imageUid)

    suspend fun updateImage(imageUid: String, request: UpdateBusinessImageRequest): Result<BusinessImageResponse> {
        return runCatching { api.updateImage(imageUid, request).data!! }
    }

    suspend fun setAsPrimary(imageUid: String): Result<BusinessImageResponse> {
        return runCatching { api.setAsPrimary(imageUid).data!! }
    }

    suspend fun reorderImages(imageUids: List<String>): Result<String> {
        return runCatching { api.reorderImages(imageUids).data!! }
    }

    suspend fun deleteImage(imageUid: String): Result<String> {
        return runCatching { api.deleteImage(imageUid).data!! }
    }
}
```

### 3.5 ViewModel

```kotlin
// shared/src/commonMain/kotlin/com/ampairs/app/presentation/business/BusinessImagesViewModel.kt

class BusinessImagesViewModel(
    private val repository: BusinessImageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessImagesUiState())
    val uiState: StateFlow<BusinessImagesUiState> = _uiState.asStateFlow()

    init {
        loadImages()
    }

    fun loadImages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getImages()
                .onSuccess { images ->
                    _uiState.update { it.copy(images = images, isLoading = false, error = null) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun uploadLogo(imageData: ByteArray, fileName: String, contentType: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true) }
            repository.uploadLogo(imageData, fileName, contentType)
                .onSuccess { business ->
                    _uiState.update { it.copy(
                        logoUrl = business.logoUrl,
                        logoThumbnailUrl = business.logoThumbnailUrl,
                        isUploading = false,
                        error = null
                    )}
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isUploading = false, error = error.message) }
                }
        }
    }

    fun uploadImage(
        imageData: ByteArray,
        fileName: String,
        contentType: String,
        imageType: BusinessImageType = BusinessImageType.GALLERY,
        title: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true) }
            repository.uploadImage(imageData, fileName, contentType, imageType, title)
                .onSuccess { newImage ->
                    _uiState.update { state ->
                        state.copy(
                            images = state.images + newImage,
                            isUploading = false,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isUploading = false, error = error.message) }
                }
        }
    }

    fun deleteImage(imageUid: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            repository.deleteImage(imageUid)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            images = state.images.filter { it.uid != imageUid },
                            isDeleting = false,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isDeleting = false, error = error.message) }
                }
        }
    }

    fun setAsPrimary(imageUid: String) {
        viewModelScope.launch {
            repository.setAsPrimary(imageUid)
                .onSuccess { updatedImage ->
                    _uiState.update { state ->
                        state.copy(
                            images = state.images.map { img ->
                                when {
                                    img.uid == imageUid -> img.copy(isPrimary = true)
                                    img.isPrimary -> img.copy(isPrimary = false)
                                    else -> img
                                }
                            }
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }

    fun reorderImages(imageUids: List<String>) {
        viewModelScope.launch {
            repository.reorderImages(imageUids)
                .onSuccess {
                    // Reorder local list to match
                    _uiState.update { state ->
                        val reorderedImages = imageUids.mapNotNull { uid ->
                            state.images.find { it.uid == uid }
                        }
                        state.copy(images = reorderedImages)
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }
}

data class BusinessImagesUiState(
    val logoUrl: String? = null,
    val logoThumbnailUrl: String? = null,
    val images: List<BusinessImageResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null
)
```

---

## 4. Android UI Implementation

### 4.1 Compose UI for Logo

```kotlin
// androidApp/src/main/kotlin/com/ampairs/app/ui/business/BusinessLogoSection.kt

@Composable
fun BusinessLogoSection(
    logoUrl: String?,
    onUploadClick: () -> Unit,
    onDeleteClick: () -> Unit,
    isUploading: Boolean,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Business Logo",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (logoUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(logoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Business Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onUploadClick) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (logoUrl != null) "Change" else "Upload")
                }

                if (logoUrl != null) {
                    OutlinedButton(
                        onClick = onDeleteClick,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Remove")
                    }
                }
            }
        }
    }
}
```

### 4.2 Compose UI for Gallery

```kotlin
// androidApp/src/main/kotlin/com/ampairs/app/ui/business/BusinessGallerySection.kt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BusinessGallerySection(
    images: List<BusinessImageResponse>,
    onAddClick: () -> Unit,
    onImageClick: (BusinessImageResponse) -> Unit,
    onSetPrimaryClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Gallery (${images.size}/20)",
                style = MaterialTheme.typography.titleMedium
            )

            IconButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add Image")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (images.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No images yet")
                    TextButton(onClick = onAddClick) {
                        Text("Add First Image")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.height(300.dp)
            ) {
                items(images, key = { it.uid }) { image ->
                    BusinessImageItem(
                        image = image,
                        onClick = { onImageClick(image) },
                        onSetPrimaryClick = { onSetPrimaryClick(image.uid) },
                        onDeleteClick = { onDeleteClick(image.uid) },
                        modifier = Modifier.animateItemPlacement()
                    )
                }
            }
        }
    }
}

@Composable
fun BusinessImageItem(
    image: BusinessImageResponse,
    onClick: () -> Unit,
    onSetPrimaryClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier.aspectRatio(1f)) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(image.thumbnailUrl ?: image.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = image.altText ?: image.title,
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick),
            contentScale = ContentScale.Crop
        )

        // Primary badge
        if (image.isPrimary) {
            Badge(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
            ) {
                Text("Primary")
            }
        }

        // Menu button
        IconButton(
            onClick = { showMenu = true },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = Color.White
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            if (!image.isPrimary) {
                DropdownMenuItem(
                    text = { Text("Set as Primary") },
                    onClick = {
                        showMenu = false
                        onSetPrimaryClick()
                    },
                    leadingIcon = { Icon(Icons.Default.Star, null) }
                )
            }
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    showMenu = false
                    onDeleteClick()
                },
                leadingIcon = { Icon(Icons.Default.Delete, null) }
            )
        }
    }
}
```

---

## 5. iOS UI Implementation (SwiftUI)

```swift
// iosApp/Sources/Business/BusinessImagesView.swift

import SwiftUI
import shared

struct BusinessLogoView: View {
    let logoUrl: String?
    let isUploading: Bool
    let onUploadTap: () -> Void
    let onDeleteTap: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Text("Business Logo")
                .font(.headline)

            ZStack {
                if let url = logoUrl, let imageUrl = URL(string: url) {
                    AsyncImage(url: imageUrl) { image in
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                    } placeholder: {
                        ProgressView()
                    }
                    .frame(width: 128, height: 128)
                    .clipShape(Circle())
                } else {
                    Circle()
                        .fill(Color.gray.opacity(0.2))
                        .frame(width: 128, height: 128)
                        .overlay(
                            Image(systemName: "building.2")
                                .font(.system(size: 48))
                                .foregroundColor(.gray)
                        )
                }

                if isUploading {
                    ProgressView()
                }
            }

            HStack(spacing: 8) {
                Button(action: onUploadTap) {
                    Label(logoUrl != nil ? "Change" : "Upload", systemImage: "square.and.arrow.up")
                }
                .buttonStyle(.bordered)

                if logoUrl != nil {
                    Button(action: onDeleteTap) {
                        Label("Remove", systemImage: "trash")
                    }
                    .buttonStyle(.bordered)
                    .tint(.red)
                }
            }
        }
        .padding()
    }
}

struct BusinessGalleryView: View {
    let images: [BusinessImageResponse]
    let onAddTap: () -> Void
    let onImageTap: (BusinessImageResponse) -> Void
    let onSetPrimaryTap: (String) -> Void
    let onDeleteTap: (String) -> Void

    private let columns = [
        GridItem(.flexible()),
        GridItem(.flexible()),
        GridItem(.flexible())
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text("Gallery (\(images.count)/20)")
                    .font(.headline)
                Spacer()
                Button(action: onAddTap) {
                    Image(systemName: "plus")
                }
            }

            if images.isEmpty {
                emptyState
            } else {
                LazyVGrid(columns: columns, spacing: 4) {
                    ForEach(images, id: \.uid) { image in
                        ImageGridItem(
                            image: image,
                            onTap: { onImageTap(image) },
                            onSetPrimaryTap: { onSetPrimaryTap(image.uid) },
                            onDeleteTap: { onDeleteTap(image.uid) }
                        )
                    }
                }
            }
        }
        .padding()
    }

    private var emptyState: some View {
        VStack(spacing: 8) {
            Image(systemName: "photo.on.rectangle.angled")
                .font(.system(size: 48))
                .foregroundColor(.gray)
            Text("No images yet")
                .foregroundColor(.secondary)
            Button("Add First Image", action: onAddTap)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 200)
        .background(Color.gray.opacity(0.1))
        .cornerRadius(8)
    }
}
```

---

## 6. Image Types Reference

| Type | Description | Use Case |
|------|-------------|----------|
| `GALLERY` | General gallery image | Default type for misc images |
| `STOREFRONT` | Storefront/exterior | Shop front, building exterior |
| `INTERIOR` | Interior/workspace | Inside shop, office space |
| `PRODUCT_SHOWCASE` | Product showcase | Featured products display |
| `TEAM` | Team/staff | Employee photos, team pictures |
| `BANNER` | Banner/promotional | Marketing banners, promotions |
| `CERTIFICATE` | Certificate/award | Business licenses, awards |
| `OTHER` | Other | Miscellaneous |

---

## 7. Error Handling

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `BUSINESS_NOT_FOUND` | 404 | Business profile doesn't exist |
| `BUSINESS_IMAGE_NOT_FOUND` | 404 | Image not found |
| `IMAGE_VALIDATION_ERROR` | 400 | Invalid file type, size, or corrupted |
| `VALIDATION_ERROR` | 400 | Invalid request data |

---

## 8. Best Practices

1. **Image Loading**: Always use thumbnails for lists/grids, full-size for detail views
2. **Caching**: Images have 1-year cache headers - implement local caching
3. **Upload Progress**: Show upload progress for better UX
4. **Error States**: Handle and display errors gracefully
5. **Optimistic Updates**: Update UI immediately, revert on failure
6. **Image Compression**: Compress images before upload on client-side
7. **Retry Logic**: Implement retry for failed uploads
8. **Offline Support**: Queue uploads when offline, sync when connected
