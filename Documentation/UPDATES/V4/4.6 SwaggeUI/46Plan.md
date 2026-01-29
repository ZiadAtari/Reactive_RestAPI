# v4.6 Implementation Plan: Swagger UI Integration

## Goal
Add interactive API documentation at `/swagger` using Swagger UI 5.x static files.

---

## Phase 1: Directory Setup

### 1.1 Create webroot structure
```
src/main/resources/webroot/swagger/
```

### 1.2 Download Swagger UI files
Download from [Swagger UI Releases](https://github.com/swagger-api/swagger-ui/releases):
- `index.html`
- `swagger-ui.css`
- `swagger-ui-bundle.js`
- `swagger-ui-standalone-preset.js`
- `swagger-initializer.js`

---

## Phase 2: Configure Swagger UI

### 2.1 [MODIFY] `swagger-initializer.js`
Update the spec URL to point to our OpenAPI file:

```javascript
window.onload = function() {
  window.ui = SwaggerUIBundle({
    url: "/openapi.yaml",  // Changed from default petstore URL
    dom_id: '#swagger-ui',
    presets: [
      SwaggerUIBundle.presets.apis,
      SwaggerUIStandalonePreset
    ],
    layout: "StandaloneLayout"
  });
};
```

---

## Phase 3: Route Configuration

### 3.1 [MODIFY] `HttpVerticle.java`
Add static file handlers before mounting the OpenAPI router:

```java
// Swagger UI static files
mainRouter.route("/swagger/*").handler(StaticHandler.create("webroot/swagger"));

// Serve OpenAPI spec for Swagger UI
mainRouter.route("/openapi.yaml").handler(ctx -> {
  ctx.response()
     .putHeader("Content-Type", "application/yaml")
     .sendFile("openapi.yaml");
});
```

**Location**: Add these routes in the `start()` method, before `mainRouter.route("/*").subRouter(apiRouter)`.

---

## Phase 4: Verification

### 4.1 Startup Test
- [ ] Application starts without errors
- [ ] Log shows: `OpenAPI specification loaded successfully`

### 4.2 Swagger UI Access
- [ ] Navigate to `http://localhost:8888/swagger/`
- [ ] UI renders with all 11 endpoints visible
- [ ] Tags display: Auth, V1, V3, Infrastructure

### 4.3 Functional Tests
| Test | Expected Result |
|:--|:--|
| `GET /v1/employees` | Returns employee list (or empty array) |
| `POST /login` | Returns JWT token |
| Authorize with token | V3 endpoints work |
| `GET /metrics` | Returns Prometheus text |

---

## Files Changed

| File | Action | Description |
|:--|:--|:--|
| `src/main/resources/webroot/swagger/` | NEW | Create directory |
| `webroot/swagger/*.js, *.css, *.html` | NEW | Swagger UI dist files |
| `webroot/swagger/swagger-initializer.js` | MODIFY | Point to `/openapi.yaml` |
| `HttpVerticle.java` | MODIFY | Add static handlers |

---

## Rollback Plan
1. Delete `src/main/resources/webroot/` directory
2. Revert `HttpVerticle.java` changes (remove static handlers)
