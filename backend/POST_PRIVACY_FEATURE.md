# Post Privacy Feature Implementation

## Overview
Added public/private post functionality with shareable UUID codes to the FCFS application.

## Database Changes
- **New Migration**: `V2__add_post_privacy.sql`
  - Added `visibility` enum field (PUBLIC/PRIVATE) with default PUBLIC
  - Added `share_code` UUID field with unique constraint
  - Added performance indexes

## API Changes

### Post Creation/Update
- Posts can now be created with `visibility` field (PUBLIC/PRIVATE)
- If not specified, defaults to PUBLIC for backward compatibility
- Each post gets a unique `share_code` UUID
- Visibility can be changed after creation

### Endpoints

#### Existing (Enhanced)
- `GET /api/posts` - Now returns only visible posts (public + user's private)
- `GET /api/posts/{id}` - Enhanced with privacy check
- `PUT /api/posts/{id}` - Now supports updating visibility

#### New
- `GET /api/posts/public` - Public posts only (anonymous access)
- `GET /api/posts/share/{shareCode}` - Access any post via share code

### Privacy Logic
- **Public posts**: Visible to everyone
- **Private posts**: Only visible to:
  - Post owner
  - Anyone with the share code

## Technical Details

### N+1 Prevention
All queries use `JOIN FETCH` to avoid N+1 problems:
- `findAllPublicWithOwner()`
- `findAllVisibleToUserWithOwner()`
- `findByShareCodeWithOwner()`

### Data Models
- **PostDto**: Added `visibility` and `shareCode` fields
- **PostAdminDto**: Added `visibility` and `shareCode` fields
- **Post**: Added `visibility` and `shareCode` fields

### Key Features
1. **Backward Compatibility**: Existing endpoints work, defaulting to PUBLIC
2. **Share Codes**: UUID-based sharing for private posts
3. **Privacy-Aware Queries**: Repository methods filter based on visibility
4. **Performance**: All queries avoid N+1 problems with JOIN FETCH

## Usage Examples

### Create Private Post
```json
POST /api/posts
{
  "title": "My Private Post",
  "description": "Only for invited people",
  "visibility": "PRIVATE",
  ...
}
```

### Share Private Post
Use the `shareCode` from the response:
`GET /api/posts/share/550e8400-e29b-41d4-a716-446655440000`

### Update Visibility
```json
PUT /api/posts/123
{
  "title": "My Post",
  "visibility": "PUBLIC",
  ...
}
```
