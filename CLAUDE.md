# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **modular monolith** logistics platform built with Spring Boot 4.0.1 and Java 21. The application follows
Domain-Driven Design (DDD) principles with strict bounded context isolation and layered architecture enforcement via
ArchUnit tests.

## Build and Development Commands

### Environment Setup

```bash
# Start PostgreSQL database
docker-compose up -d

# Stop database
docker-compose down
```

### Build and Run

```bash
# Build the project
./gradlew build

# Run the application (automatically uses 'local' profile)
./gradlew bootRun

# Run all tests
./gradlew test

# Run only architecture tests
./gradlew test --tests "*ArchitectureTest"

# Clean build
./gradlew clean build
```

### Database Migrations

- Migrations are managed by Flyway and located in `src/main/resources/db/migration/`
- Migrations run automatically on application startup
- Database URL: `jdbc:postgresql://localhost:5434/logistics`
- Credentials: `transport_user` / `transport_password`

## Architecture

### Modular Monolith Structure

The codebase is organized into **bounded contexts** (modules), each completely isolated from others:

```
tech.bytesmind.logistics/
├── shared/           # Cross-cutting concerns (security, tenancy, events, exceptions)
├── agency/           # Agency management bounded context
├── parcel/           # Parcel management bounded context (planned)
└── auth/             # Authentication bounded context (planned)
```

**Critical Rule**: Bounded contexts MUST NOT depend on each other. Only `shared` can be referenced across contexts. This
is enforced by ArchUnit tests in `src/test/java/tech/bytesmind/logistics/shared/architecture/ArchitectureTest.java`.

### DDD Layered Architecture

Each bounded context follows strict 4-layer DDD architecture:

```
<context>/
├── api/              # REST controllers, DTOs, HTTP layer
├── application/      # Application services, mappers, orchestration
├── domain/           # Business logic, domain models, domain services, policies, events
└── infrastructure/   # Repositories, external integrations
```

**Layer Dependencies** (enforced by ArchUnit):

- **API** → Application (no other layer can access API)
- **Application** → Domain + Infrastructure
- **Domain** → No dependencies on other layers (pure business logic)
- **Infrastructure** → Domain (for repository interfaces)

**Important Patterns**:

- Domain models are **anemic** (no business logic in entities)
- Business logic resides in **Domain Services** (e.g., `AgencyDomainService`)
- Application services orchestrate domain services and repositories
- Use **MapStruct** for entity/DTO mapping

### Multi-Tenancy Architecture

The application implements **tenant isolation via `agency_id`**:

- All tenant-aware entities implement `TenantAware` interface
- `TenantContextFilter` extracts `agency_id` from JWT and stores in `TenantContext` (ThreadLocal)
- Security model supports multiple actor types: `PLATFORM_ADMIN`, `AGENCY_EMPLOYEE`, `DRIVER`, `CUSTOMER`
- JWT claims used:
    - `sub`: User ID (UUID)
    - `actor_type`: Type of actor
    - `agency_id`: Agency UUID (required for AGENCY_EMPLOYEE, null for others)
    - `roles`: List of role strings

### Security Architecture

Authentication is handled via **OAuth2 JWT Resource Server**:

- JWT issuer/JWK configuration in `application.yml` points to Keycloak
- `SecurityContextService` extracts security context from JWT
- Access control annotations: `@RequireRole`, `@RequireActor`
- Security policies: `AgencyAccessPolicy`, `LocationAccessPolicy`

### Event-Driven Architecture

Domain events enable loose coupling:

- All domain events implement `DomainEvent` interface
- Events published via `TransactionalEventPublisher`
- Events are published **after transaction commit** (Spring's `@TransactionalEventListener`)
- Examples: `AgencyCreatedEvent`, `AgencySuspendedEvent`, `AgencyLocationAddedEvent`

## Key Constants and Configuration

### Architecture Constants

See `tech.bytesmind.logistics.architecture.ArchitectureConstants`:

- Bounded context packages
- DDD layer names
- JWT claim names

### Application Configuration

- Database: PostgreSQL 16 on port 5434
- Application port: 8080
- JWT issuer: `http://localhost:8080/realms/transport` (configurable via env vars)
- Hibernate: `ddl-auto=validate` (schema managed by Flyway only)

## Development Guidelines

### When Adding New Features

1. **Identify the bounded context** - Does it belong to existing context or need a new one?
2. **Respect layer boundaries** - Place code in correct layer (api/application/domain/infrastructure)
3. **Follow anemic domain pattern** - Business logic in domain services, not entities
4. **Implement tenant awareness** - All entities must implement `TenantAware` if they belong to an agency
5. **Publish domain events** - Use `TransactionalEventPublisher` for important state changes
6. **Run architecture tests** - Ensure `./gradlew test --tests "*ArchitectureTest"` passes

### When Working with Domain Models

- Entities are JPA entities with Lombok `@Getter`/`@Setter`
- Use `@PrePersist` and `@PreUpdate` for timestamp management
- Soft delete via `@SQLDelete` annotation
- Business rules enforced in domain services, not entity setters

### When Creating Endpoints

- Controllers in `api` package only
- Use DTOs (request/response records) for API contracts
- Map DTOs to entities using MapStruct mappers in `application.mapper` package
- Annotate with security requirements (`@RequireRole`, `@RequireActor`)

### MapStruct Configuration

- MapStruct version: 1.6.3
- Lombok integration enabled via `lombok-mapstruct-binding:0.2.0`
- Mappers are Spring components (`@Mapper(componentModel = "spring")`)

## Testing

- Unit tests for domain services
- Architecture tests enforce all architectural rules
- Use `@SpringBootTest` for integration tests
- Test database configuration in test resources

## Common Patterns

### Creating an Entity with Domain Logic

```java
// 1. Create anemic entity in domain.model
// 2. Create domain service in domain.service
// 3. Implement business logic in domain service
// 4. Create application service to orchestrate
// 5. Publish domain events for state changes
```

### Adding Multi-Tenant Entity

```java
// 1. Implement TenantAware interface
// 2. Add agency_id column to database migration
// 3. Use TenantService.validateAccess() in access policies
```

### Publishing Domain Events

```java
// In application service:
eventPublisher.publish(new MyDomainEvent(...));
```

## Technology Stack

- **Framework**: Spring Boot 4.0.1
- **Language**: Java 21
- **Database**: PostgreSQL 16
- **Migration**: Flyway
- **Security**: Spring Security OAuth2 Resource Server (JWT)
- **Mapping**: MapStruct 1.6.3
- **Testing**: JUnit 5, ArchUnit 1.3.0
- **Build Tool**: Gradle (Kotlin DSL)