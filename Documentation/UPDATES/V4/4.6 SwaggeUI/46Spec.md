# v4.6 Specification: Swagger UI Integration

## Overview
This update adds an interactive Swagger UI interface at `/swagger`, allowing developers to browse, test, and authenticate against the API directly from a web browser. The UI will consume the `openapi.yaml` specification created in v4.5.

## Requirements

### 1. Functional Requirements
- **Interactive Documentation**: Swagger UI must render all 11 endpoints from `openapi.yaml`
- **Try-it-out**: Execute API calls directly from the browser
- **Authentication Flow**: "Authorize" button accepts JWT token for V3 endpoints
- **Accessibility**: UI accessible at `http://localhost:8888/swagger/`

### 2. Technical Requirements
- **Swagger UI Version**: 5.x (latest stable)
- **No Build Tooling**: Use pre-built dist files (no npm/webpack)
- **Static Handler**: Vert.x `StaticHandler` serves files from `webroot/swagger`

## Directory Structure

```
src/main/resources/
├── logback.xml
├── openapi.yaml              # API specification (served at /openapi.yaml)
├── schemas/
└── webroot/                  # NEW: Static web assets
    └── swagger/              # Swagger UI files
        ├── index.html
        ├── swagger-ui.css
        ├── swagger-ui-bundle.js
        ├── swagger-ui-standalone-preset.js
        └── swagger-initializer.js   # Points to /openapi.yaml
```

## Route Configuration

| Path | Handler | Purpose |
|:--|:--|:--|
| `/swagger/*` | `StaticHandler.create("webroot/swagger")` | Serve Swagger UI assets |
| `/openapi.yaml` | Inline handler | Serve OpenAPI spec for Swagger UI |

## API Coverage
All 11 endpoints will be documented in Swagger UI:

| Tag | Endpoints |
|:--|:--|
| Auth | `POST /login` |
| V1 (Legacy) | `GET/POST /v1/employees`, `PUT/DELETE /v1/employees/{id}` |
| V3 (Authenticated) | `GET/POST /v3/employees`, `PUT/DELETE /v3/employees/{id}` |
| Infrastructure | `GET /health/live`, `GET /metrics` |

## Scope

### In Scope
- Download Swagger UI 5.x distribution files
- Create `webroot/swagger/` directory structure
- Configure `swagger-initializer.js` with `/openapi.yaml` URL
- Add routes in `HttpVerticle.java`
- Verify UI loads and API calls work

### Out of Scope
- Custom Swagger UI theming/branding
- Swagger Codegen / SDK generation

## Dependencies
- v4.5 (OpenAPI spec and RouterBuilder integration)

## Risk Assessment
- **Bundle Size**: ~3MB (acceptable for dev tooling)
- **Security**: Consider disabling in production or protecting behind auth
