# API Contracts

No API contract changes. This is a pure structural refactor.

All existing endpoints remain identical:
- `POST /api/auth/v1/init`
- `POST /api/auth/v1/verify-otp`
- `POST /api/auth/v1/refresh`
- `POST /api/auth/v1/logout`
- `GET  /api/user/v1/profile`
- `POST /api/user/v1/update`
- `POST /api/user/v1/profile-picture`
- `GET  /api/user/v1/profile-picture`

Request/response shapes, HTTP status codes, and header requirements are unchanged.
