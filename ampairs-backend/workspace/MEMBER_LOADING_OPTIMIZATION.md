# Workspace Member Loading Optimization

## Overview

The workspace member loading has been optimized to efficiently load member details along with user information, avoiding N+1 query problems and providing better performance for web applications.

## Implementation

### 1. Custom Repository Query

Added `findActiveMembers()` method in `WorkspaceMemberRepository` that uses a custom JPQL query with projection to load all member fields in a single database query:

```kotlin
@Query("""
    SELECT new map(
        wm.uid as memberId,
        wm.userId as userId,
        wm.memberName as memberName,
        wm.memberEmail as memberEmail,
        wm.role as role,
        wm.isActive as isActive,
        wm.joinedAt as joinedAt,
        wm.lastActiveAt as lastActivityAt,
        wm.department as department,
        wm.jobTitle as jobTitle,
        wm.phone as phone
    )
    FROM com.ampairs.workspace.model.WorkspaceMember wm
    WHERE wm.workspaceId = :workspaceId 
    AND wm.isActive = true
    ORDER BY wm.joinedAt DESC
""")
fun findActiveMembers(workspaceId: String, pageable: Pageable): Page<Map<String, Any>>
```

### 2. UserDetailProvider Interface

Created a flexible interface for integrating with external user services:

```kotlin
interface UserDetailProvider {
    fun getUserDetail(userId: String): UserDetail?
    fun getUserDetails(userIds: List<String>): Map<String, UserDetail>
    fun isUserServiceAvailable(): Boolean
}
```

### 3. Optimized Service Method

Implemented `getWorkspaceMembersOptimized()` that:
- Uses the custom repository query (single DB query)
- Batch loads user details when user service is available
- Falls back to local member data when user service is unavailable
- Provides enhanced user information (firstName, lastName, avatarUrl)

### 4. Configuration

Added `UserDetailConfiguration` to provide default implementation and allow external modules to override:

```kotlin
@Bean
@ConditionalOnMissingBean(UserDetailProvider::class)
fun localUserDetailProvider(): UserDetailProvider {
    return LocalUserDetailProvider()
}
```

## Benefits

### Performance Improvements
- **Single Query**: Instead of N+1 queries, uses one optimized query with projection
- **Batch Loading**: When user service is available, loads all user details in one batch call
- **Reduced Network Overhead**: Minimizes database roundtrips

### Scalability
- **Large Teams**: Efficient even with hundreds of workspace members
- **Pagination Friendly**: Works seamlessly with Spring Data pagination
- **Memory Efficient**: Uses projections instead of full entity loading

### Extensibility
- **User Service Integration**: Ready for integration with external user service
- **Fallback Strategy**: Gracefully handles missing user service
- **Future-Proof**: Easy to extend with additional user fields

## Usage

### Controller Integration
```kotlin
val members = memberService.getWorkspaceMembersOptimized(workspaceId, pageable)
```

### Future User Service Integration
To integrate with a user service, simply implement `UserDetailProvider`:

```kotlin
@Component
class AuthUserDetailProvider(
    private val authService: AuthService
) : UserDetailProvider {
    
    override fun getUserDetails(userIds: List<String>): Map<String, UserDetail> {
        return authService.getUsersByIds(userIds).associate { user ->
            user.id to UserDetail(
                userId = user.id,
                firstName = user.firstName,
                lastName = user.lastName,
                email = user.email,
                avatarUrl = user.profilePicture,
                isActive = user.isActive
            )
        }
    }
    
    override fun isUserServiceAvailable(): Boolean = true
}
```

## Performance Comparison

### Before Optimization
- 1 query to load members
- N additional queries to load user details (N+1 problem)
- Total: N+1 database queries for N members

### After Optimization
- 1 optimized query with projection to load all member data
- 1 optional batch call to user service (when available)
- Total: 1 database query + 1 optional service call

## Database Query Example

The optimized query generates SQL similar to:
```sql
SELECT 
    wm.uid as memberId,
    wm.user_id as userId,
    wm.member_name as memberName,
    wm.member_email as memberEmail,
    wm.role as role,
    wm.is_active as isActive,
    wm.joined_at as joinedAt,
    wm.last_active_at as lastActivityAt,
    wm.department as department,
    wm.job_title as jobTitle,
    wm.phone as phone
FROM workspace_members wm 
WHERE wm.workspace_id = ? 
AND wm.is_active = true 
ORDER BY wm.joined_at DESC 
LIMIT ? OFFSET ?
```

This approach provides significant performance improvements while maintaining clean architecture and extensibility for future enhancements.