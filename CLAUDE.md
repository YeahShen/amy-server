# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build and run (dev profile)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Run tests
./mvnw test

# Build JAR
./mvnw package -DskipTests
```

The application starts on port 8080 by default. When running with `dev` profile, it prints the local URL to the log.

## Architecture

Spring Boot 4.1 MVC application (Java 17, Maven) using MyBatis-Flex for database access and Spring Data Redis for caching.

### Technology stack

| Concern | Technology |
|---|---|
| Web framework | Spring Boot MVC (Tomcat) |
| ORM | MyBatis-Flex 1.11.8 (annotation-driven, `BaseMapper`-based) |
| Database | MariaDB |
| Cache | Redis (FastJSON2 serializer, 2-hour default TTL) |
| Auth | Spring Security + JWT (planned, not yet implemented) |
| JSON | FastJSON2 (with Spring HTTP message converter extension) |
| Utilities | Hutool 5.8, Guava 33, Commons Codec |

### Package layout

```
site.ashenstation.amyserver
├── AppRun.java              # Entry point, enables @MapperScan
├── config/
│   ├── security/            # SpringSecurityConfig (stub)
│   └── redis/               # RedisConfiguration, FastJsonRedisSerializer
├── controller/              # IndexController (stub)
├── dto/                     # JwtUserDto (implements UserDetails)
├── entity/                  # User entity → sys_user table
├── enums/                   # LoginPlatform (CLIENT, BROWSER, MOBILE)
├── mapper/                  # UserMapper extends BaseMapper<User>
├── property/                # @ConfigurationProperties classes
│   ├── LoginProperties      # login.* prefix
│   ├── RsaProperties        # rsa.private_key
│   └── SecurityProperties   # jwt.* prefix (token config)
├── service/                 # UserDetailService (stub)
└── utils/
    └── RedisUtils           # Full-featured Redis helper (@Component)
```

### Configuration files

Config YAML files are in `src/main/resources/config/` (not the default `src/main/resources/`). Three files:

- `application.yaml` — Common config (server port, JWT/RSA/login keys)
- `application-dev.yaml` — Dev DB credentials (hardcoded), JWT with token renewal
- `application-prod.yaml` — Prod DB via env vars (`DATASOURCE_HOST`, `DATASOURCE_PORT`, `DATASOURCE_NAME`, `DATASOURCE_USERNAME`, `DATASOURCE_PASSWORD`)

### Database layer

MyBatis-Flex provides `BaseMapper<T>` — entities map to tables via `@Table` annotation. The `User` entity maps to `sys_user` table with auto-increment primary key. Mapper interfaces just extend `BaseMapper` to get CRUD methods without XML.

### Redis / caching

`RedisUtils` is the primary Redis access point — injected as a Spring `@Component`, it wraps `RedisTemplate` with typed get/set, hash, list, set operations, key scanning, and retry logic (3 attempts on `set`). The custom `FastJsonRedisSerializer` handles all value serialization; deserialization auto-detects types via FastJSON's `@type` metadata in serialized payloads. A whitelist limits auto-type deserialization to `site.ashenstation` package.

Spring's `@Cacheable` annotations are wired through `RedisCacheManager` with FastJSON serialization and a custom `KeyGenerator` that hashes (class, method, package, params) with MurmurHash3.

### Auth (WIP)

JWT authentication is configured in YAML but not yet wired. `JwtUserDto` wraps `User` and implements `UserDetails`. `UserDetailService` returns `null` (stub). `SpringSecurityConfig` is empty. RSA private key is Base64-encoded in config for JWT signing.

### Project state

This is early-stage scaffolding. Many classes are empty stubs waiting for implementation: `SpringSecurityConfig`, `IndexController`, `UserDetailService.loadUserByUsername`. The `User` entity is defined but no authentication flow is connected yet.

## Code conventions

- All classes use Lombok (`@Data`, `@Slf4j`, `@RequiredArgsConstructor`). Use `var` or explicit types consistently with existing code.
- Chinese comments are used throughout. New code should follow the same convention.
- MyBatis-Flex uses annotation-driven mapping — no XML mapper files. Add `@Table`, `@Id`, `@Column` annotations to entities rather than writing XML.
- Property classes use `@ConfigurationProperties` with `prefix` for type-safe config binding. Follow this pattern for new config groups.