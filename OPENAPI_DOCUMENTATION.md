# Rapla REST API - OpenAPI Documentation

## Overview

The Rapla REST API is now fully documented using **OpenAPI 3.0** specifications (formerly known as Swagger). This provides comprehensive API documentation and an interactive UI for testing endpoints.

## Features

- **Interactive API Documentation**: Swagger UI for testing endpoints directly in your browser
- **OpenAPI Specification**: Full OpenAPI 3.0 compatible specification
- **Authentication Support**: Bearer token authentication with JWT
- **Comprehensive Endpoint Documentation**: All REST endpoints with request/response schemas

## Accessing the Documentation

### Swagger UI
Once Rapla is running, you can access the interactive Swagger UI at:
```
http://localhost:8051/rapla/swagger-ui.html
```

### OpenAPI JSON Specification
The raw OpenAPI specification is available at:
```
http://localhost:8051/rapla/api-docs
```

## API Endpoints

### Authentication
- **POST** `/login` - User login with credentials, returns access token

### Dynamic Types
- **GET** `/dynamictypes` - Get available classification types (resource, person, reservation)

### Resources
- **GET** `/resources` - List all resources and persons
- **GET** `/resources/{id}` - Get specific resource
- **POST** `/resources` - Create new resource
- **PUT** `/resources/{id}` - Update resource
- **DELETE** `/resources/{id}` - Delete resource

### Events (Reservations)
- **GET** `/events` - List events with filtering options
- **GET** `/events/{id}` - Get specific event
- **POST** `/events` - Create new event
- **PUT** `/events/{id}` - Update event
- **PATCH** `/events/{id}` - Partially update event
- **DELETE** `/events/{id}` - Delete event

## Authentication

All API endpoints (except `/login`) require Bearer token authentication.

1. **Login** to get access token:
   ```bash
   POST /rapla/login
   Content-Type: application/json
   
   {
     "username": "admin",
     "password": ""
   }
   ```

2. **Response** includes access token:
   ```json
   {
     "accessToken": "eyJ...",
     "validUntil": "2025-02-05T10:30:00Z"
   }
   ```

3. **Use token** in subsequent requests:
   ```bash
   GET /rapla/events
   Authorization: Bearer eyJ...
   ```

## Request/Response Format

The API supports both **JSON** and **XML** formats:

### JSON Request Example (Create Resource)
```json
{
  "classification": {
    "type": "room",
    "data": {
      "name": ["Test Room"]
    }
  }
}
```

### Query Parameters

**Events Filtering:**
- `start` - Start date (ISO 8601, e.g., "2025-01-01")
- `end` - End date (ISO 8601, e.g., "2025-12-31T23:59:59Z")
- `resources` - List of resource IDs
- `owners` - List of owner user IDs
- `eventTypes` - List of event type keys
- `attributeFilter` - JSON filter map

**Resources Filtering:**
- `resourceTypes` - List of resource type keys
- `attributeFilter` - JSON filter map

## Implementation Details

The OpenAPI documentation is configured in the following files:

### Main Configuration
- **`OpenAPIConfiguration.java`** - Centralized OpenAPI configuration with security schemes

### Endpoint Classes with Annotations
- **`RaplaAuthRestPage.java`** - Authentication endpoints
- **`RaplaResourcesRestPage.java`** - Resource management endpoints
- **`RaplaEventsRestPage.java`** - Event/reservation management endpoints
- **`RaplaDynamicTypesRestPage.java`** - Dynamic type endpoints

## Technologies

- **OpenAPI Specification 3.0** - Industry standard for REST API documentation
- **Swagger UI** - Interactive web interface for API exploration
- **JWT Bearer Tokens** - Secure token-based authentication
- **JAX-RS / RESTEasy** - Java REST implementation

## Development

To generate or update OpenAPI documentation:

1. Annotations are already added to all REST endpoint methods
2. The OpenAPI spec is automatically generated at application startup
3. Access Swagger UI to verify the documentation

### Adding Documentation to New Endpoints

1. Add annotations to the REST method:
   ```java
   @Operation(summary = "Brief description", description = "Detailed description")
   @ApiResponse(responseCode = "200", description = "Success")
   @ApiResponse(responseCode = "404", description = "Not found")
   public void myEndpoint() { ... }
   ```

2. Add parameter documentation:
   ```java
   @Parameter(description = "Parameter description", required = true) 
   @PathParam("id") String id
   ```

3. The OpenAPI spec will be automatically updated

## References

- [OpenAPI 3.0 Specification](https://spec.openapis.org/oas/v3.0.3)
- [Swagger Documentation](https://swagger.io/resources/articles/best-practices-in-api-documentation/)
- [Springdoc OpenAPI](https://springdoc.org/)

## Testing

You can test the API using:

1. **Swagger UI** - Interactive testing in browser
2. **cURL** - Command line
3. **Postman** - API client
4. **RestAPIExample.java** - Embedded test client
