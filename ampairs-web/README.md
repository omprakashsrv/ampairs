# Ampairs Web

This is the Angular web application for Ampairs, featuring secure mobile-based authentication and a modern Material Design interface.

## Features

- **Mobile-based Authentication**: Login using mobile number with OTP verification
- **Secure Token Management**: JWT tokens stored in secure HTTP-only cookies
- **Responsive Design**: Mobile-first design using Angular Material
- **Auto-refresh**: Stay logged in across page refreshes
- **Modern UI**: Clean and intuitive user interface

## Getting Started

### Prerequisites
- Node.js (v18 or higher)
- npm or yarn
- Angular CLI

### Installation

1. Install dependencies:
```bash
npm install
```

2. Start the development server:
```bash
npm start
```

3. Open your browser and navigate to `http://localhost:4200`

### Build

To build the project for production:
```bash
npm run build
```

## Project Structure

```
src/
├── app/
│   ├── auth/
│   │   ├── login/                 # Login page component
│   │   └── verify-otp/            # OTP verification component
│   ├── core/
│   │   ├── guards/
│   │   │   └── auth.guard.ts      # Route protection guard
│   │   └── services/
│   │       └── auth.service.ts    # Authentication service
│   ├── home/
│   │   └── home.component.ts      # Home page component
│   ├── app.component.ts           # Root component
│   ├── app.config.ts             # App configuration
│   └── app.routes.ts             # Route definitions
├── styles.scss                   # Global styles
└── index.html                   # Main HTML file
```

## Authentication Flow

1. **Login**: User enters mobile number (+91 only)
2. **OTP Generation**: System sends 6-digit OTP via SMS
3. **Verification**: User enters OTP for verification
4. **Token Storage**: Access and refresh tokens stored in secure cookies
5. **Auto-login**: User stays logged in across sessions

## API Integration

The application integrates with the Spring Boot auth service endpoints:

- `POST /api/v1/auth/init` - Initialize authentication
- `POST /api/v1/auth/verify` - Verify OTP
- `POST /api/v1/auth/refresh` - Refresh access token
- `POST /api/v1/auth/logout` - Logout user

## Environment Configuration

Update the API base URL in `src/app/core/services/auth.service.ts`:

```typescript
private readonly API_BASE_URL = 'http://localhost:8081/api/v1/auth';
```

## Security Features

- Secure cookie storage for tokens
- HTTP-only cookies prevent XSS attacks
- SameSite cookie attribute for CSRF protection
- Automatic token refresh
- Route guards for protected pages

## Development

### Code Scaffolding

Run `ng generate component component-name` to generate a new component.

### Running Tests

Run `ng test` to execute unit tests via [Karma](https://karma-runner.github.io).

### Linting

Run `ng lint` to lint the project using ESLint.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## License

This project is licensed under the MIT License.