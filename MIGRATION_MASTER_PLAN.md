# NEXVIA — MIGRATION MASTER PLAN

> **Status**: PLAN ONLY — No code changes, no Base44 removal, no commits.
> **Date**: 2026-08-09
> **Scope**: Complete migration from Base44 to a self-hosted, secure architecture.

---

## Table of Contents

1. [Current Architecture](#1-current-architecture)
2. [Target Architecture](#2-target-architecture)
3. [Architecture Decisions](#3-architecture-decisions)
4. [Technology Decisions](#4-technology-decisions)
5. [Database Design](#5-database-design)
6. [Authentication Design](#6-authentication-design)
7. [Authorization Design](#7-authorization-design)
8. [API Design](#8-api-design)
9. [Business Logic Design](#9-business-logic-design)
10. [Realtime Design](#10-realtime-design)
11. [Security Model](#11-security-model)
12. [Migration Strategy](#12-migration-strategy)
13. [Data Migration Strategy](#13-data-migration-strategy)
14. [Base44 Removal Strategy](#14-base44-removal-strategy)
15. [Testing Strategy](#15-testing-strategy)
16. [Deployment Strategy](#16-deployment-strategy)
17. [Rollback Strategy](#17-rollback-strategy)
18. [Implementation Phases](#18-implementation-phases)
19. [Acceptance Criteria](#19-acceptance-criteria)
20. [Open Questions](#20-open-questions)

---

## 1. Current Architecture

### 1.1 Stack Overview

| Layer | Technology |
|-------|-----------|
| **Frontend** | React 18, Vite 6, React Router 6, TailwindCSS 3, Radix UI, shadcn/ui, Recharts, Leaflet (react-leaflet) |
| **State** | React Query (TanStack), React Context (AuthContext) |
| **Backend** | Base44 hosted platform (BaaS) — no self-hosted backend code exists |
| **Database** | Base44 entities (hosted, opaque) |
| **Auth** | Base44 SDK (`base44.auth.*`) — email/password, Google OAuth, OTP verification |
| **Realtime** | Base44 `.subscribe()` on entities |
| **Functions** | Base44 serverless functions (Deno runtime) — one function exists: `asignarRol` |
| **Storage/Media** | `media.base44.com` for images (logo) |
| **Build plugin** | `@base44/vite-plugin` — HMR notifier, navigation notifier, analytics tracker, visual edit agent |

### 1.2 Entities (Base44 Data Model)

**User** — Extended with `role` field (enum: `admin`, `chofer`, `usuario`, default `usuario`)

**Viaje** — Trip entity with fields:
- `camion_id`, `chofer_nombre`, `chofer_id`, `usuario_id`, `usuario_nombre`
- `origen_lat`, `origen_lng`, `destino_lat`, `destino_lng`
- `distancia_km`, `toneladas`, `tipo_tarifa` (enum: `por_km`, `por_tonelada`)
- `precio`, `carga`
- `estado` (enum: `solicitado`, `aceptado`, `en_camino`, `completado`, `cancelado`)
- RLS: create=open, read=open, update=owner/chofer/solicitado/admin, delete=admin

**Camion** — Truck entity with fields:
- `transporte_nombre`, `transporte_cuit`, `chofer_nombre`, `chofer_cuit`
- `patente`, `patente_acoplado`, `telefono`, `capacidad_kg`
- `lat`, `lng`, `estado` (enum: `disponible`, `ocupado`, `inactivo`)
- `user_id` — links to the driver
- RLS: create=open, read=open, update=owner/admin, delete=admin

**Configuracion** — System configuration (singleton):
- `tarifa_por_km`, `tarifa_por_tonelada`, `zona_nombre`, `centro_lat`, `centro_lng`
- RLS: CUD=admin, read=open

**LugarGuardado** — Saved locations per user:
- `nombre`, `lat`, `lng`, `tipo` (enum: `pueblo`, `hacienda`, `campo`, `otro`)
- RLS: create=open, read/update/delete=created_by

### 1.3 Authentication Flow (Current)

1. Token stored in `localStorage` as `base44_access_token`
2. Token passed via URL query parameter `access_token` (removed from URL after extraction)
3. `base44.auth.me()` validates token and returns user object
4. Login: `base44.auth.loginViaEmailPassword(email, password)` — page redirect after
5. Register: `base44.auth.register()` → OTP verification → `base44.auth.verifyOtp()` → sets token
6. Google OAuth: `base44.auth.loginWithProvider("google", redirectUrl)`
7. Password reset: `base44.auth.resetPasswordRequest(email)` → token via email → `base44.auth.resetPassword({resetToken, newPassword})`
8. Logout: `base44.auth.logout(redirectUrl)` — clears token
9. Role assignment: serverless function `asignarRol` — authenticated user calls it to self-assign `chofer` or `usuario` (not `admin`)

### 1.4 Critical Security Issues in Current Architecture

1. **Client trusts data**: Viaje creation sends `usuario_id`, `precio`, `distancia_km`, `estado` from frontend — backend has no server-side validation
2. **Price set by client**: `precio` is calculated entirely on frontend and submitted; backend stores it as-is
3. **Distance set by client**: `distancia_km` computed via haversine on frontend, trusted directly
4. **State transitions uncontrolled**: Frontend directly sets `estado: 'aceptado'`, `estado: 'en_camino'`, `estado: 'completado'` via entity update — no state machine enforcement
5. **No transactional trip acceptance**: `handleAceptar` does two separate updates (Viaje + Camion) without atomicity — race conditions possible
6. **GPS auto-completes trips**: Frontend GPS proximity (0.3 km threshold) automatically transitions trips to `en_camino` and `completado` — spoofable
7. **Open entity creation**: Any authenticated user can create Camion entities (RLS create=true)
8. **Admin role change from UI**: `UsuariosSection` directly updates `User.role` via `base44.entities.User.update(userId, { role })` — bypasses the `asignarRol` function's admin-role guard
9. **All viajes readable by all users**: Viaje read RLS is `true` (open) — every user sees every trip
10. **Viaje update too permissive**: The `$or` in update RLS includes `data.estado === 'solicitado'` — meaning ANY user can update ANY trip that is still in `solicitado` state
11. **Media hosted on Base44 CDN**: Logo served from `media.base44.com`
12. **Sensitive data exposed**: CUIT, telefono, patente visible to all users via entity reads

### 1.5 External Services Used

| Service | Usage | Source |
|---------|-------|--------|
| **OpenStreetMap Nominatim** | Geocoding search in `BuscarLugar.jsx` | Frontend `fetch()` |
| **OSRM** | Route line rendering in `MapaCamiones.jsx` | Frontend `fetch()` |
| **Leaflet / OSM tiles** | Map rendering | Frontend library |
| **Stripe** | Payment integration (dependencies present but unused in current code) | `@stripe/stripe-js`, `@stripe/react-stripe-js` in `package.json` |
| **Google OAuth** | Social login via Base44 | `base44.auth.loginWithProvider("google")` |

### 1.6 Roles and Navigation

| Role | Home Page | Access |
|------|-----------|--------|
| `usuario` (productor) | `/` (Home) | Request trips, view map, manage saved locations |
| `chofer` | `/chofer` | View trip requests, accept trips, GPS tracking |
| `admin` | `/admin` | Manage config/tariffs, manage trucks, manage users/roles, invite users |
| `user` (default unassigned) | `/bienvenida` | Choose role (productor or camionero), redirected from Layout |

---

## 2. Target Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                      FRONTEND (React SPA)                     │
│  React 18 · Vite · TailwindCSS · React Router · React Query  │
│  Leaflet · Radix UI · shadcn/ui                              │
└──────────────────────┬───────────────────────────────────────┘
                       │ HTTPS
                       ▼
┌──────────────────────────────────────────────────────────────┐
│                     API BACKEND (Node.js)                      │
│  Express.js · TypeScript                                      │
│  ┌────────────┐  ┌────────────┐  ┌────────────────────────┐  │
│  │  Auth      │  │  API       │  │  WebSocket Server      │  │
│  │  Middleware │  │  Routes    │  │  (ws/socket.io)        │  │
│  └────────────┘  └────────────┘  └────────────────────────┘  │
│  ┌────────────────────────────────────────────────────────┐  │
│  │              Services / Business Logic                  │  │
│  │  AuthService · ViajeService · CamionService            │  │
│  │  ConfigService · LugarService · UserService            │  │
│  └────────────────────────────────────────────────────────┘  │
│  ┌────────────────────────────────────────────────────────┐  │
│  │              Data Access Layer                          │  │
│  │  Repositories with parameterized queries               │  │
│  │  Transaction support via pg pool                       │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────┬───────────────────────────────────────┘
                       │
          ┌────────────┼────────────┐
          ▼            ▼            ▼
┌──────────────┐ ┌──────────┐ ┌──────────────┐
│  PostgreSQL  │ │  Redis   │ │  External    │
│  (primary)   │ │ (sessions│ │  Services    │
│              │ │  rate     │ │  · Google    │
│  · users     │ │  limiting │ │    OAuth     │
│  · viajes    │ │  realtime │ │  · Nominatim │
│  · camiones  │ │  pub/sub) │ │  · OSRM     │
│  · config    │ │          │ │  · SMTP      │
│  · lugares   │ │          │ │  · S3/storage│
│  · audit_log │ └──────────┘ └──────────────┘
└──────────────┘
```

### Key Architectural Principles

1. **Backend is the single source of truth** for all business logic, state transitions, pricing, and authorization
2. **Frontend is a presentation layer** — it can estimate, preview, and suggest, but never decide
3. **Every mutation goes through validated API endpoints** — no direct entity manipulation
4. **Authentication via HttpOnly cookies** — tokens never accessible to JavaScript
5. **Authorization checked on every request** — role + ownership + resource-level
6. **Realtime via authenticated WebSocket** with channel-level authorization
7. **All sensitive operations are transactional** and audit-logged

---

## 3. Architecture Decisions

### AD-1: Monolithic Backend vs. Microservices

**Decision**: Monolithic Node.js/Express backend.

**Rationale**: NEXVIA has ~5 entities, ~3 roles, and a small user base (agro logistics in a specific region). A monolith is simpler to develop, deploy, debug, and maintain. Microservices would add deployment complexity, distributed transaction headaches, and operational overhead with zero benefit at this scale. If scale demands it later, the service layer can be extracted into separate services because the boundary is at the service interface, not the transport layer.

### AD-2: REST API vs. GraphQL

**Decision**: REST API with JSON payloads.

**Rationale**: The data access patterns are simple CRUD with filters. GraphQL would add schema complexity, resolver overhead, and a learning curve without solving an actual problem. The frontend already uses React Query, which works cleanly with REST endpoints. GraphQL's main advantage (avoiding over-fetching across deep entity graphs) is irrelevant — entities are flat and queries are straightforward.

### AD-3: Session Management — HttpOnly Cookie vs. JWT in localStorage

**Decision**: HttpOnly, Secure, SameSite=Lax cookies with server-side session store (Redis).

**Rationale**: The current `localStorage` token approach is vulnerable to XSS — any injected script can steal the token. HttpOnly cookies cannot be read by JavaScript, making them immune to XSS token theft. Server-side sessions in Redis enable instant revocation (logout, password change, admin ban) without waiting for token expiry. The tradeoff is that the backend must maintain session state, but Redis makes this trivial and the session store doubles as the rate-limiting store.

### AD-4: ORM vs. Raw SQL vs. Query Builder

**Decision**: No ORM. Use `pg` (node-postgres) directly with parameterized queries, wrapped in a thin repository layer.

**Rationale**: The schema is simple (5 tables). An ORM (Prisma, TypeORM, Sequelize) would add significant dependency weight, migration complexity, and abstraction leakage for a schema this small. Raw parameterized SQL is explicit, auditable, and avoids the N+1 / eager-loading pitfalls of ORMs. The repository pattern provides the same testability benefits without the abstraction cost. Migrations will be managed via versioned `.sql` files executed in order.

### AD-5: WebSocket Library

**Decision**: `ws` (lightweight WebSocket library) with a thin pub/sub layer backed by Redis.

**Rationale**: `socket.io` adds significant overhead (engine.io fallback, rooms abstraction, binary protocol) that isn't needed. The realtime requirements are simple: push entity change events to authorized subscribers. `ws` is the most widely used, standards-compliant WebSocket library for Node.js. Redis pub/sub provides horizontal scalability if needed later.

### AD-6: Hosting/Deployment Target

**Decision**: Deferred — see [Open Questions](#20-open-questions). The architecture is designed to run on any Node.js-capable host (VPS, Docker container, Railway, Render, Fly.io, AWS EC2/ECS).

**Requirements**: Node.js 20+, PostgreSQL 15+, Redis 7+, static file serving (or CDN) for frontend assets.

---

## 4. Technology Decisions

### 4.1 What We Keep (Frontend)

| Technology | Reason |
|-----------|--------|
| React 18 | Current framework, all UI code written in it |
| Vite 6 | Current build tool, fast, well-configured |
| React Router 6 | Current routing, all routes defined |
| TailwindCSS 3 | Current styling, extensive usage across all components |
| Radix UI + shadcn/ui | Current component library, ~40 UI components |
| React Query (TanStack) | Current data-fetching layer, already configured |
| Leaflet (react-leaflet) | Map rendering, already integrated |
| Recharts | Charts (in dependencies) |
| Zod | Validation (already a dependency, will be used more extensively) |
| Lucide React | Icons (already used everywhere) |
| react-hook-form | Forms (already a dependency) |
| date-fns | Date handling (already a dependency) |

### 4.2 What We Add (Backend)

| Technology | Purpose | Rationale |
|-----------|---------|-----------|
| **Node.js 20 LTS** | Runtime | Same language as frontend (JavaScript/TypeScript), team familiarity, NPM ecosystem |
| **Express.js 4** | HTTP framework | Minimal, battle-tested, massive middleware ecosystem |
| **TypeScript 5** | Type safety | Already in `devDependencies`, one `.ts` file exists (`base44/functions/asignarRol/entry.ts`), provides compile-time safety for backend |
| **pg (node-postgres)** | PostgreSQL client | Zero-dependency, parameterized queries, pool management, transaction support |
| **Redis (ioredis)** | Session store, rate limiting, pub/sub | Single dependency covers three needs |
| **ws** | WebSocket server | Lightweight, standards-compliant |
| **bcrypt** | Password hashing | Industry standard, adaptive cost factor |
| **helmet** | Security headers | Minimal config, comprehensive headers |
| **cors** | CORS handling | Express middleware |
| **express-rate-limit + rate-limit-redis** | Rate limiting | Configurable per-route limits with Redis store |
| **nodemailer** | Email sending | Password reset, OTP, notifications |
| **passport + passport-google-oauth20** | Google OAuth | Standard OIDC implementation |
| **cookie-parser** | Cookie parsing | Required for session cookies |
| **zod** | Input validation (shared) | Already in frontend deps, shared schemas possible |
| **nanoid** | Token generation | Cryptographically secure, URL-safe |
| **dotenv** | Environment config | Standard `.env` loading |

### 4.3 What We Remove

| Technology | Replacement |
|-----------|------------|
| `@base44/sdk` | Own API client (`fetch` + React Query) |
| `@base44/vite-plugin` | Remove entirely (HMR notifier, analytics, visual editor are Base44-specific) |
| `base44Client.js` | Own API service module |
| `app-params.js` | Own config module (environment variables only) |
| Base44 entity methods (`.create`, `.filter`, `.update`, `.delete`, `.subscribe`, `.list`, `.get`) | REST API calls via React Query + WebSocket subscriptions |
| Base44 auth methods (`.loginViaEmailPassword`, `.register`, `.verifyOtp`, etc.) | Own auth API endpoints |
| Base44 functions (`.invoke`) | Own backend endpoints |
| Base44 user management (`.inviteUser`) | Own user management API |
| `media.base44.com` | Self-hosted static assets or S3-compatible storage |

### 4.4 What We DON'T Add

| Technology | Why Not |
|-----------|---------|
| Next.js | Would require rewriting the entire frontend for SSR/RSC. Current SPA architecture is fine. |
| Prisma / TypeORM | Overkill for 5 tables (see AD-4) |
| GraphQL | Unnecessary complexity (see AD-2) |
| Firebase / Supabase / another BaaS | Defeats the purpose of owning the stack |
| Docker (initially) | Not required for development or simple VPS deployment; can be added later |
| Kubernetes | Completely unnecessary at this scale |

---

## 5. Database Design

### 5.1 Schema

```sql
-- ============================================================
-- ENUM TYPES
-- ============================================================

CREATE TYPE user_role AS ENUM ('usuario', 'chofer', 'admin');
CREATE TYPE viaje_estado AS ENUM ('solicitado', 'aceptado', 'en_camino', 'completado', 'cancelado');
CREATE TYPE tipo_tarifa AS ENUM ('por_km', 'por_tonelada');
CREATE TYPE camion_estado AS ENUM ('disponible', 'ocupado', 'inactivo');
CREATE TYPE lugar_tipo AS ENUM ('pueblo', 'hacienda', 'campo', 'otro');

-- ============================================================
-- USERS
-- ============================================================

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           TEXT NOT NULL,
    email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    password_hash   TEXT,                          -- NULL for Google OAuth-only users
    full_name       TEXT,
    role            user_role NOT NULL DEFAULT 'usuario',
    google_id       TEXT,                          -- Google OAuth subject ID
    avatar_url      TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT users_email_unique UNIQUE (email),
    CONSTRAINT users_google_id_unique UNIQUE (google_id),
    CONSTRAINT users_email_format CHECK (email ~* '^[^@\s]+@[^@\s]+\.[^@\s]+$')
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_users_google_id ON users (google_id) WHERE google_id IS NOT NULL;

-- ============================================================
-- SESSIONS
-- ============================================================
-- Primary session store is Redis; this table is for audit/backup only.
-- Sessions in Redis: key = "session:{sessionId}", value = JSON {userId, role, createdAt, expiresAt, ip, userAgent}

-- ============================================================
-- EMAIL VERIFICATION TOKENS
-- ============================================================

CREATE TABLE email_verification_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code        TEXT NOT NULL,                    -- 6-digit OTP
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT evt_not_expired CHECK (expires_at > created_at)
);

CREATE INDEX idx_evt_user_id ON email_verification_tokens (user_id);

-- ============================================================
-- PASSWORD RESET TOKENS
-- ============================================================

CREATE TABLE password_reset_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  TEXT NOT NULL,                    -- hashed random token
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT prt_not_expired CHECK (expires_at > created_at)
);

CREATE INDEX idx_prt_user_id ON password_reset_tokens (user_id);

-- ============================================================
-- CAMIONES (TRUCKS)
-- ============================================================

CREATE TABLE camiones (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID REFERENCES users(id) ON DELETE SET NULL, -- assigned driver
    transporte_nombre   TEXT,
    transporte_cuit     TEXT,
    chofer_nombre       TEXT NOT NULL,
    chofer_cuit         TEXT,
    patente             TEXT NOT NULL,
    patente_acoplado    TEXT,
    telefono            TEXT,
    capacidad_kg        INTEGER DEFAULT 0,
    lat                 DOUBLE PRECISION,
    lng                 DOUBLE PRECISION,
    estado              camion_estado NOT NULL DEFAULT 'disponible',
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT camiones_patente_unique UNIQUE (patente) WHERE NOT is_deleted,
    CONSTRAINT camiones_capacidad_positive CHECK (capacidad_kg >= 0)
);

CREATE INDEX idx_camiones_user_id ON camiones (user_id) WHERE NOT is_deleted;
CREATE INDEX idx_camiones_estado ON camiones (estado) WHERE NOT is_deleted;

-- ============================================================
-- VIAJES (TRIPS)
-- ============================================================

CREATE TABLE viajes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id      UUID NOT NULL REFERENCES users(id),      -- requester
    camion_id       UUID REFERENCES camiones(id),             -- assigned truck (NULL until accepted)
    chofer_id       UUID REFERENCES users(id),                -- assigned driver (NULL until accepted)

    origen_lat      DOUBLE PRECISION NOT NULL,
    origen_lng      DOUBLE PRECISION NOT NULL,
    destino_lat     DOUBLE PRECISION NOT NULL,
    destino_lng     DOUBLE PRECISION NOT NULL,
    origen_nombre   TEXT,
    destino_nombre  TEXT,

    distancia_km    DOUBLE PRECISION NOT NULL,                -- server-calculated
    toneladas       DOUBLE PRECISION DEFAULT 0,
    tipo_tarifa     tipo_tarifa NOT NULL DEFAULT 'por_km',
    tarifa_unitaria DOUBLE PRECISION NOT NULL,                -- rate used at time of calculation
    precio          DOUBLE PRECISION NOT NULL,                -- server-calculated final price
    carga           TEXT NOT NULL,

    estado          viaje_estado NOT NULL DEFAULT 'solicitado',

    solicitado_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    aceptado_at     TIMESTAMPTZ,
    en_camino_at    TIMESTAMPTZ,
    completado_at   TIMESTAMPTZ,
    cancelado_at    TIMESTAMPTZ,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         INTEGER NOT NULL DEFAULT 1,               -- optimistic locking

    CONSTRAINT viajes_distancia_positive CHECK (distancia_km > 0),
    CONSTRAINT viajes_precio_positive CHECK (precio > 0),
    CONSTRAINT viajes_coords_valid CHECK (
        origen_lat BETWEEN -90 AND 90 AND
        origen_lng BETWEEN -180 AND 180 AND
        destino_lat BETWEEN -90 AND 90 AND
        destino_lng BETWEEN -180 AND 180
    )
);

CREATE INDEX idx_viajes_usuario_id ON viajes (usuario_id);
CREATE INDEX idx_viajes_chofer_id ON viajes (chofer_id) WHERE chofer_id IS NOT NULL;
CREATE INDEX idx_viajes_camion_id ON viajes (camion_id) WHERE camion_id IS NOT NULL;
CREATE INDEX idx_viajes_estado ON viajes (estado);
CREATE INDEX idx_viajes_solicitado_at ON viajes (solicitado_at DESC);

-- ============================================================
-- CONFIGURACION (SYSTEM CONFIG — SINGLETON)
-- ============================================================

CREATE TABLE configuracion (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tarifa_por_km       DOUBLE PRECISION NOT NULL,
    tarifa_por_tonelada DOUBLE PRECISION DEFAULT 0,
    zona_nombre         TEXT,
    centro_lat          DOUBLE PRECISION NOT NULL DEFAULT -32.4341,
    centro_lng          DOUBLE PRECISION NOT NULL DEFAULT -63.2433,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by          UUID REFERENCES users(id),

    CONSTRAINT config_tarifa_positive CHECK (tarifa_por_km > 0),
    CONSTRAINT config_tarifa_ton_non_negative CHECK (tarifa_por_tonelada >= 0)
);

-- ============================================================
-- LUGARES GUARDADOS (SAVED LOCATIONS)
-- ============================================================

CREATE TABLE lugares_guardados (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    nombre      TEXT NOT NULL,
    lat         DOUBLE PRECISION NOT NULL,
    lng         DOUBLE PRECISION NOT NULL,
    tipo        lugar_tipo NOT NULL DEFAULT 'otro',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT lg_coords_valid CHECK (
        lat BETWEEN -90 AND 90 AND
        lng BETWEEN -180 AND 180
    )
);

CREATE INDEX idx_lugares_user_id ON lugares_guardados (user_id);

-- ============================================================
-- AUDIT LOG
-- ============================================================

CREATE TABLE audit_log (
    id          BIGSERIAL PRIMARY KEY,
    actor_id    UUID REFERENCES users(id),        -- NULL for system actions
    action      TEXT NOT NULL,                     -- e.g., 'viaje.create', 'user.role_change'
    resource    TEXT NOT NULL,                     -- e.g., 'viaje', 'user', 'camion'
    resource_id TEXT,                              -- UUID of affected resource
    details     JSONB,                             -- action-specific metadata
    ip_address  INET,
    user_agent  TEXT,
    result      TEXT NOT NULL DEFAULT 'success',   -- 'success' or 'failure'
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_actor ON audit_log (actor_id);
CREATE INDEX idx_audit_resource ON audit_log (resource, resource_id);
CREATE INDEX idx_audit_action ON audit_log (action);
CREATE INDEX idx_audit_created_at ON audit_log (created_at DESC);

-- ============================================================
-- UPDATED_AT TRIGGER
-- ============================================================

CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_camiones_updated_at BEFORE UPDATE ON camiones
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_viajes_updated_at BEFORE UPDATE ON viajes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_lugares_updated_at BEFORE UPDATE ON lugares_guardados
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
```

### 5.2 Migration File Strategy

Migrations are versioned SQL files in `server/migrations/`:

```
server/migrations/
  001_initial_schema.sql
  002_seed_configuracion.sql
  ...
```

A simple migration runner reads `.sql` files in order, tracks applied migrations in a `_migrations` table, and runs unapplied ones inside a transaction.

**No auto-DDL**: Migrations are explicit, reviewed, and version-controlled. Production databases are never modified by application code outside of migrations.

### 5.3 Key Design Notes

- **UUIDs as PKs**: Avoids sequential ID enumeration attacks; safe for client-side generation; no collision concerns across environments.
- **Optimistic locking on viajes**: The `version` column prevents lost-update problems. Every update must include `WHERE version = :expected_version` and increment it. If the row was modified between read and write, the update affects 0 rows and the operation is rejected.
- **Soft delete on camiones**: `is_deleted` flag preserves historical trip references. Unique constraint on `patente` only applies to non-deleted trucks.
- **Denormalized names on viajes**: `origen_nombre`, `destino_nombre` are stored at trip creation time so they survive location renames.
- **Timestamp tracking on state transitions**: Each estado has its own `*_at` column for analytics and dispute resolution.
- **Audit log is append-only**: No UPDATE or DELETE allowed. Never log passwords, tokens, or secrets in `details`.

---

## 6. Authentication Design

### 6.1 Registration

```
POST /api/auth/register
Body: { email, password }
```

Server-side flow:
1. Validate email format (RFC 5322 basic)
2. Validate password policy: minimum 8 characters, at least 1 uppercase, 1 lowercase, 1 digit
3. Check email not already registered (case-insensitive)
4. Hash password with bcrypt (cost factor 12)
5. Create user with `role = 'usuario'`, `email_verified = false`
6. Generate 6-digit OTP, store hashed in `email_verification_tokens` with 15-minute expiry
7. Send OTP via email (SMTP)
8. Return `{ message: "Verification code sent" }` — never indicate if email was already taken
9. Audit log: `user.register`

**Password policy**: 8+ chars, 1 uppercase, 1 lowercase, 1 digit. No dictionary check initially (can add later).

**Anti-enumeration**: Registration always returns the same response regardless of whether the email exists. If the email is already verified, silently do nothing. If unverified, regenerate OTP.

### 6.2 Email Verification (OTP)

```
POST /api/auth/verify-email
Body: { email, code }
```

Server-side flow:
1. Find latest unused token for email where `expires_at > now()`
2. Compare submitted code against stored (hashed) code
3. If match: mark `email_verified = true`, mark token `used_at = now()`
4. Create session (see 6.4), set HttpOnly cookie
5. Return `{ user: { id, email, role, full_name } }`
6. If no match: increment attempt counter (rate limit per email: 5 attempts per 15 minutes)
7. Audit log: `user.verify_email` (success/failure)

### 6.3 Login

```
POST /api/auth/login
Body: { email, password }
```

Server-side flow:
1. Find user by email (case-insensitive)
2. If not found OR password doesn't match: return `401 { error: "Invalid credentials" }` — generic message
3. If user is not active (`is_active = false`): return `401 { error: "Invalid credentials" }` — same generic message
4. If email not verified: return `403 { error: "email_not_verified" }` — frontend can offer to resend OTP
5. Compare password with bcrypt
6. Create session, set HttpOnly cookie
7. Return `{ user: { id, email, role, full_name, avatar_url } }`
8. Audit log: `user.login` (success/failure, IP)

**Rate limiting**: 5 failed attempts per email per 15 minutes. After 5 failures, return `429 Too Many Requests` with `Retry-After` header. Counter stored in Redis with TTL.

**Brute force protection**: After 10 consecutive failed attempts (tracked per email in Redis), require CAPTCHA solution or impose a 30-minute lockout. The user account is NOT locked (preventing denial-of-service via lockout), but the IP+email combination is throttled.

### 6.4 Session Management

**Session creation**:
1. Generate cryptographically random session ID (nanoid, 32 chars)
2. Store in Redis: key `session:{sessionId}` → value `{ userId, role, createdAt, expiresAt, ip, userAgent }`
3. Set cookie: `session_id={sessionId}; HttpOnly; Secure; SameSite=Lax; Path=/; Max-Age=604800` (7 days)

**Session validation** (on every authenticated request):
1. Read `session_id` from cookie
2. Look up in Redis
3. If not found or expired: return `401`
4. If found: extend TTL by 7 days (sliding expiration)
5. Attach `req.user = { id, role }` from session data

**Session revocation**:
- Logout: delete Redis key
- Password change: delete ALL sessions for that user (prefix scan `session:*` filtered by userId)
- Admin deactivation: delete all sessions for target user

**Cookie settings**:
| Attribute | Value | Why |
|-----------|-------|-----|
| `HttpOnly` | `true` | Prevents XSS from reading the cookie |
| `Secure` | `true` (production) | Prevents transmission over HTTP |
| `SameSite` | `Lax` | Prevents CSRF on state-changing requests while allowing top-level navigation |
| `Path` | `/` | Available to all API routes |
| `Max-Age` | `604800` (7 days) | Reasonable session duration |

### 6.5 Logout

```
POST /api/auth/logout
```

1. Read session ID from cookie
2. Delete Redis key
3. Clear cookie (set `Max-Age=0`)
4. Return `204 No Content`
5. Audit log: `user.logout`

### 6.6 Password Reset

**Request**:
```
POST /api/auth/forgot-password
Body: { email }
```

1. Always return `{ message: "If an account exists, a reset link has been sent" }` — anti-enumeration
2. If user exists: generate random token (nanoid, 48 chars), hash it, store in `password_reset_tokens` with 1-hour expiry
3. Send email with link: `{FRONTEND_URL}/reset-password?token={raw_token}`
4. Invalidate any previous unused reset tokens for this user
5. Rate limit: 3 requests per email per hour

**Reset**:
```
POST /api/auth/reset-password
Body: { token, newPassword }
```

1. Hash the submitted token, find matching unexpired, unused row
2. If not found: return `400 { error: "Invalid or expired token" }`
3. Validate new password against policy
4. Hash new password with bcrypt
5. Update user's `password_hash`
6. Mark token as `used_at = now()`
7. Delete ALL sessions for this user (force re-login everywhere)
8. Audit log: `user.password_reset`

### 6.7 Google OAuth

```
GET /api/auth/google
→ Redirect to Google OAuth consent screen

GET /api/auth/google/callback?code=...
→ Exchange code for tokens, create/link user, create session, redirect to frontend
```

Server-side flow (using Passport):
1. `GET /api/auth/google`: redirect to Google with `client_id`, `redirect_uri`, `scope=openid email profile`
2. Google redirects back to `GET /api/auth/google/callback` with authorization code
3. Server exchanges code for tokens using `client_secret` (NEVER sent to frontend)
4. Extract `sub` (Google ID), `email`, `name`, `picture` from ID token
5. Find user by `google_id = sub`:
   - If found: update `full_name`, `avatar_url` if changed
   - If not found: find by email
     - If email exists without google_id: link Google account (`google_id = sub`)
     - If email not found: create new user with `email_verified = true`, `role = 'usuario'`
6. Create session, set cookie
7. Redirect to `{FRONTEND_URL}/` (or `/bienvenida` if role is unassigned)
8. Audit log: `user.google_login`

**Security**: `client_secret` is a server-side environment variable, never exposed to frontend. The `state` parameter is used to prevent CSRF on the OAuth flow.

### 6.8 Role Assignment

```
POST /api/auth/choose-role
Body: { role: "usuario" | "chofer" }
```

1. Requires authenticated session
2. Only allowed if user's current role is `'usuario'` AND they haven't completed onboarding (no active trips, no assigned truck)
3. Validates `role` is one of `['usuario', 'chofer']` — never `'admin'`
4. Updates user role
5. Audit log: `user.role_change`

Admin role assignment:
```
PUT /api/admin/users/:userId/role
Body: { role: "usuario" | "chofer" | "admin" }
```
- Requires `admin` role
- Can set any role including `admin`
- Audit log with admin actor ID

---

## 7. Authorization Design

### 7.1 Middleware Stack

Every request passes through:
1. **Cookie parser** — extracts session cookie
2. **Session middleware** — validates session, attaches `req.user = { id, role }`
3. **Rate limiter** — per-route limits
4. **Route handler** — business logic with authorization checks

### 7.2 RBAC (Role-Based Access Control)

```
requireAuth()        → 401 if not authenticated
requireRole('admin') → 403 if role !== 'admin'
requireRole('chofer') → 403 if role !== 'chofer'
requireRoles(['admin', 'chofer']) → 403 if role not in list
```

### 7.3 Resource Authorization Matrix

| Resource | Action | usuario | chofer | admin |
|----------|--------|---------|--------|-------|
| **Viaje** | Create (request trip) | Own only | No | No |
| **Viaje** | Read own | Yes | — | — |
| **Viaje** | Read assigned | — | Yes (as chofer) | — |
| **Viaje** | Read all | No | No | Yes |
| **Viaje** | List solicitado | No | Yes (to see available) | Yes |
| **Viaje** | Cancel own (solicitado) | Yes | No | Yes |
| **Viaje** | Accept (solicitado → aceptado) | No | Yes (transactional) | No |
| **Viaje** | State transitions | No | Restricted (see §9) | Yes |
| **Camion** | Create | No | No | Yes |
| **Camion** | Read available | Yes | Yes | Yes |
| **Camion** | Read all details | No | Own only | Yes |
| **Camion** | Update location | No | Own only | No |
| **Camion** | Update details | No | No | Yes |
| **Camion** | Delete | No | No | Yes |
| **Config** | Read | Yes | Yes | Yes |
| **Config** | Update | No | No | Yes |
| **LugarGuardado** | CRUD | Own only | Own only | Own only |
| **User** | Read profile | Own only | Own only | All |
| **User** | Update role | No | No | Yes |
| **User** | Invite | No | No | Yes |
| **User** | Deactivate | No | No | Yes |
| **Audit Log** | Read | No | No | Yes |

### 7.4 Ownership Enforcement

Every query that involves user-specific data includes the authenticated user's ID from the session (NEVER from the request body):

```typescript
// Correct — userId from session
const viajes = await db.query(
  'SELECT * FROM viajes WHERE usuario_id = $1',
  [req.user.id]
);

// WRONG — userId from request body (vulnerable to IDOR)
const viajes = await db.query(
  'SELECT * FROM viajes WHERE usuario_id = $1',
  [req.body.usuario_id]  // NEVER
);
```

### 7.5 Data Visibility Rules

**What a `usuario` (productor) can see about a trip:**
- All trip details for their own trips
- Chofer name, truck info (patente, capacidad) only after trip is accepted
- NO chofer CUIT, phone, or personal data unless explicitly needed for carta de porte

**What a `chofer` can see:**
- All `solicitado` trips (to decide which to accept) — only: coordinates, distance, price, cargo type
- Full details of trips they accepted/completed
- Their own truck info
- NO other chofers' data, NO other users' personal data

**What an `admin` can see:**
- Everything, including all users, all trips, all trucks, audit log
- Users' email and role
- NO password hashes (never returned from any endpoint)

---

## 8. API Design

### 8.1 Base URL and Versioning

```
/api/v1/...
```

Versioning via URL path. All endpoints return JSON. Errors follow a consistent format:

```json
{
  "error": "Human-readable message",
  "code": "MACHINE_READABLE_CODE"
}
```

### 8.2 Endpoint Catalog

#### Auth
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/auth/register` | No | Register with email/password |
| POST | `/api/v1/auth/verify-email` | No | Verify OTP code |
| POST | `/api/v1/auth/resend-otp` | No | Resend verification OTP |
| POST | `/api/v1/auth/login` | No | Login with email/password |
| POST | `/api/v1/auth/logout` | Yes | Destroy session |
| POST | `/api/v1/auth/forgot-password` | No | Request password reset |
| POST | `/api/v1/auth/reset-password` | No | Reset password with token |
| GET | `/api/v1/auth/google` | No | Initiate Google OAuth |
| GET | `/api/v1/auth/google/callback` | No | Google OAuth callback |
| GET | `/api/v1/auth/me` | Yes | Get current user profile |
| POST | `/api/v1/auth/choose-role` | Yes | Self-assign role (onboarding) |

#### Viajes
| Method | Path | Auth | Role | Description |
|--------|------|------|------|-------------|
| POST | `/api/v1/viajes` | Yes | usuario | Request a new trip |
| GET | `/api/v1/viajes/mis-viajes` | Yes | usuario | List own trips |
| GET | `/api/v1/viajes/activo` | Yes | usuario | Get active trip (if any) |
| GET | `/api/v1/viajes/disponibles` | Yes | chofer | List available (solicitado) trips |
| POST | `/api/v1/viajes/:id/aceptar` | Yes | chofer | Accept a trip (transactional) |
| POST | `/api/v1/viajes/:id/cancelar` | Yes | usuario/admin | Cancel a trip |
| POST | `/api/v1/viajes/:id/en-camino` | Yes | chofer | Mark trip as en_camino |
| POST | `/api/v1/viajes/:id/completar` | Yes | chofer | Mark trip as completed |
| GET | `/api/v1/viajes/:id` | Yes | owner/chofer/admin | Get trip details |
| GET | `/api/v1/viajes` | Yes | admin | List all trips (with filters) |

#### Camiones
| Method | Path | Auth | Role | Description |
|--------|------|------|------|-------------|
| GET | `/api/v1/camiones/disponibles` | Yes | any | List available trucks (public data only) |
| GET | `/api/v1/camiones/mi-camion` | Yes | chofer | Get own assigned truck |
| POST | `/api/v1/camiones` | Yes | admin | Create a truck |
| PUT | `/api/v1/camiones/:id` | Yes | admin | Update truck details |
| DELETE | `/api/v1/camiones/:id` | Yes | admin | Soft-delete a truck |
| PATCH | `/api/v1/camiones/:id/ubicacion` | Yes | chofer | Update own truck GPS position |
| GET | `/api/v1/camiones` | Yes | admin | List all trucks |

#### Configuracion
| Method | Path | Auth | Role | Description |
|--------|------|------|------|-------------|
| GET | `/api/v1/configuracion` | Yes | any | Get current config |
| PUT | `/api/v1/configuracion` | Yes | admin | Update config |

#### Lugares Guardados
| Method | Path | Auth | Role | Description |
|--------|------|------|------|-------------|
| GET | `/api/v1/lugares` | Yes | any | List own saved locations |
| POST | `/api/v1/lugares` | Yes | any | Create a saved location |
| PUT | `/api/v1/lugares/:id` | Yes | any | Update own saved location |
| DELETE | `/api/v1/lugares/:id` | Yes | any | Delete own saved location |

#### Admin — Users
| Method | Path | Auth | Role | Description |
|--------|------|------|------|-------------|
| GET | `/api/v1/admin/users` | Yes | admin | List all users |
| PUT | `/api/v1/admin/users/:id/role` | Yes | admin | Change user role |
| POST | `/api/v1/admin/users/invite` | Yes | admin | Invite user by email |
| PUT | `/api/v1/admin/users/:id/deactivate` | Yes | admin | Deactivate user |
| POST | `/api/v1/admin/camiones/:camionId/asignar` | Yes | admin | Assign truck to driver |

#### Pricing
| Method | Path | Auth | Role | Description |
|--------|------|------|------|-------------|
| POST | `/api/v1/precio/estimar` | Yes | any | Estimate price for a route |

### 8.3 Request/Response DTOs

**Create Viaje (request body)**:
```json
{
  "origen_lat": -32.4341,
  "origen_lng": -63.2433,
  "origen_nombre": "Campo Los Álamos",
  "destino_lat": -32.5012,
  "destino_lng": -63.1987,
  "destino_nombre": "Acopio Villa María",
  "tipo_tarifa": "por_km",
  "toneladas": 0,
  "tarifa_ofrecida": 500,
  "carga": "Soja"
}
```

Note what is NOT in the request:
- `usuario_id` — derived from session
- `precio` — calculated by server
- `distancia_km` — calculated by server (with routing API or haversine fallback)
- `estado` — always starts as `solicitado`
- `camion_id`, `chofer_id` — null until accepted

**Viaje response (for usuario)**:
```json
{
  "id": "uuid",
  "origen_lat": -32.4341,
  "origen_lng": -63.2433,
  "origen_nombre": "Campo Los Álamos",
  "destino_lat": -32.5012,
  "destino_lng": -63.1987,
  "destino_nombre": "Acopio Villa María",
  "distancia_km": 12.4,
  "tipo_tarifa": "por_km",
  "tarifa_unitaria": 500,
  "precio": 6200,
  "carga": "Soja",
  "estado": "aceptado",
  "chofer_nombre": "Juan Pérez",
  "camion_patente": "AB123CD",
  "solicitado_at": "2026-08-09T...",
  "aceptado_at": "2026-08-09T..."
}
```

Note: sensitive fields (chofer CUIT, telefono, camion full details) are omitted. A separate endpoint or query param can expose carta de porte data when the trip is in the right state.

### 8.4 Input Validation (Zod Schemas)

All request bodies are validated using Zod schemas before reaching business logic. Invalid input returns `400` with field-level errors. Schemas are defined once and shared between frontend (for client-side preview validation) and backend (for authoritative validation).

---

## 9. Business Logic Design

### 9.1 Viaje State Machine

```
                    ┌──────────────┐
                    │  SOLICITADO  │
                    └──────┬───────┘
                           │
              ┌────────────┼────────────┐
              ▼                         ▼
      ┌──────────────┐         ┌──────────────┐
      │   ACEPTADO   │         │  CANCELADO   │
      └──────┬───────┘         └──────────────┘
             │                         ▲
             ▼                         │
      ┌──────────────┐                 │
      │  EN_CAMINO   │─────────────────┘
      └──────┬───────┘
             │
             ▼
      ┌──────────────┐
      │  COMPLETADO  │
      └──────────────┘
```

**Allowed transitions**:

| From | To | Who | Conditions |
|------|----|-----|------------|
| `solicitado` | `aceptado` | chofer | Transactional (see §9.2) |
| `solicitado` | `cancelado` | usuario (owner) or admin | — |
| `aceptado` | `en_camino` | chofer (assigned) | Chofer must be assigned to this trip |
| `aceptado` | `cancelado` | admin only | Chofer cannot cancel after accepting (can be revised) |
| `en_camino` | `completado` | chofer (assigned) | See §9.4 for completion rules |
| `en_camino` | `cancelado` | admin only | Emergency cancellation |

**Every other transition is rejected** with `400 { error: "Invalid state transition", code: "INVALID_TRANSITION" }`.

The state machine is enforced by the backend service. The transition function:
1. Reads the viaje with `FOR UPDATE` (row lock)
2. Validates the transition is allowed from current state
3. Validates the actor has permission for this transition
4. Applies the transition
5. Updates the corresponding timestamp
6. Increments the `version` column
7. Audit logs the transition

### 9.2 Trip Acceptance (Transactional)

This is the most critical operation. It MUST be atomic.

```typescript
async acceptTrip(viajeId: string, choferId: string, camionId: string): Promise<Viaje> {
  return db.transaction(async (tx) => {
    // 1. Lock the viaje row
    const viaje = await tx.query(
      'SELECT * FROM viajes WHERE id = $1 FOR UPDATE',
      [viajeId]
    );
    if (!viaje) throw new NotFoundError('Trip not found');
    if (viaje.estado !== 'solicitado') throw new ConflictError('Trip is no longer available');

    // 2. Verify chofer has role 'chofer'
    const chofer = await tx.query('SELECT * FROM users WHERE id = $1', [choferId]);
    if (chofer.role !== 'chofer') throw new ForbiddenError('Not a driver');

    // 3. Lock and verify camion
    const camion = await tx.query(
      'SELECT * FROM camiones WHERE id = $1 AND NOT is_deleted FOR UPDATE',
      [camionId]
    );
    if (!camion) throw new NotFoundError('Truck not found');
    if (camion.user_id !== choferId) throw new ForbiddenError('Not your truck');
    if (camion.estado !== 'disponible') throw new ConflictError('Truck is not available');

    // 4. Check chofer doesn't have another active trip
    const activeTrip = await tx.query(
      "SELECT id FROM viajes WHERE chofer_id = $1 AND estado IN ('aceptado', 'en_camino')",
      [choferId]
    );
    if (activeTrip) throw new ConflictError('You already have an active trip');

    // 5. Update viaje
    await tx.query(
      `UPDATE viajes SET
        camion_id = $1, chofer_id = $2, estado = 'aceptado',
        aceptado_at = now(), version = version + 1, updated_at = now()
       WHERE id = $3 AND estado = 'solicitado'`,
      [camionId, choferId, viajeId]
    );

    // 6. Update camion estado
    await tx.query(
      "UPDATE camiones SET estado = 'ocupado', updated_at = now() WHERE id = $1",
      [camionId]
    );

    // 7. Return updated viaje
    return tx.query('SELECT * FROM viajes WHERE id = $1', [viajeId]);
  });
}
```

**Race condition protection**: `SELECT ... FOR UPDATE` acquires a row-level lock. If CHOFER A and CHOFER B try to accept simultaneously, one will block until the other's transaction completes. The second transaction will then see `estado !== 'solicitado'` and fail with `ConflictError`.

### 9.3 Pricing

**Server-side price calculation**:

```
POST /api/v1/viajes
```

1. Receive: `origen_lat/lng`, `destino_lat/lng`, `tipo_tarifa`, `toneladas`, `tarifa_ofrecida`, `carga`
2. Server calculates `distancia_km`:
   - Primary: OSRM routing API → actual driving distance
   - Fallback: Haversine formula × 1.3 (road factor)
   - Error: reject the request, do not accept client-provided distance
3. Server fetches current `tarifa_por_km` or `tarifa_por_tonelada` from `configuracion`
4. Server determines `tarifa_unitaria`:
   - User-offered rate (`tarifa_ofrecida`) is accepted if within sanity bounds (50%–500% of system rate)
   - If outside bounds: reject with `400`
5. Server calculates `precio`:
   - `por_km`: `distancia_km × tarifa_unitaria`
   - `por_tonelada`: `toneladas × tarifa_unitaria`
6. Server stores: `distancia_km`, `tarifa_unitaria`, `precio`

**Estimation endpoint** (for frontend preview):
```
POST /api/v1/precio/estimar
Body: { origen_lat, origen_lng, destino_lat, destino_lng, tipo_tarifa, toneladas, tarifa_ofrecida }
Response: { distancia_km, tarifa_unitaria, precio_estimado }
```

This endpoint is idempotent, read-only, and returns a preview. The actual price is recalculated at trip creation time.

### 9.4 Trip Completion

**Rules for marking a trip as `completado`**:

1. Only the assigned chofer can trigger completion
2. Trip must be in `en_camino` state
3. Server records `completado_at` timestamp
4. Server updates camion estado back to `disponible` (within the same transaction)

**GPS and completion** — Design decision:

The current frontend auto-completes trips based on GPS proximity (0.3 km from destination). This is **insecure** because GPS is client-provided and trivially spoofable.

New approach:
- GPS proximity from frontend is treated as a **suggestion**, not a command
- The chofer presses a "Completar viaje" button in the UI (explicit action)
- The backend validates:
  - Trip is in `en_camino` state
  - Actor is the assigned chofer
  - At least 5 minutes have elapsed since `en_camino_at` (prevents instant completion)
  - Optionally: last known GPS position is within reasonable range of destination (sanity check, not proof)
- The backend logs the completion with the chofer's last reported GPS position for potential dispute resolution

**Future enhancement** (not in scope for migration): dual confirmation (chofer + usuario both confirm delivery).

### 9.5 GPS / Location Updates

```
PATCH /api/v1/camiones/:id/ubicacion
Body: { lat, lng }
```

Server-side validation:
1. Authenticated chofer, truck belongs to them
2. Coordinates are valid (-90 ≤ lat ≤ 90, -180 ≤ lng ≤ 180)
3. Rate limit: max 1 update per 5 seconds per truck
4. Sanity check: if last known position exists, distance from last update should be < 200 km (prevent spoofing jumps)
5. Store position and `updated_at`
6. Publish to realtime channel (see §10)

The backend does NOT automatically transition trip states based on GPS. This is a frontend UX convenience that can be implemented client-side as a suggestion (show a "I've arrived" button when close), not as an automatic state change.

### 9.6 Mass Assignment Protection

Every endpoint has a specific input DTO. Only whitelisted fields are read from the request body.

**Example — Create Camion (admin)**:
Accepted fields: `transporte_nombre`, `transporte_cuit`, `chofer_nombre`, `chofer_cuit`, `patente`, `patente_acoplado`, `telefono`, `capacidad_kg`

Ignored/rejected: `id`, `user_id`, `lat`, `lng`, `estado`, `is_deleted`, `created_at`, `updated_at`

**Example — Update Viaje state**:
Each transition has its own endpoint (`/aceptar`, `/cancelar`, `/en-camino`, `/completar`). There is NO generic `PATCH /viajes/:id` that accepts arbitrary fields.

---

## 10. Realtime Design

### 10.1 Architecture

```
Frontend                          Backend
  │                                 │
  │── WS connect ──────────────────>│  (with session cookie)
  │<── auth ok ─────────────────────│
  │                                 │
  │── subscribe:camiones:disponibles│
  │<── ack ─────────────────────────│
  │                                 │
  │<── event:camion:update {data} ──│  (when a camion position changes)
  │<── event:viaje:create {data} ───│  (when a new trip is requested)
  │                                 │
```

### 10.2 WebSocket Server

- Runs on the same Node.js process as the HTTP server (upgrade from HTTP)
- Authentication: on connection, validate session cookie (same as HTTP auth)
- If session is invalid or missing: close with `4401` code

### 10.3 Channels and Authorization

| Channel | Who can subscribe | Events |
|---------|-------------------|--------|
| `camiones:disponibles` | Any authenticated user | Truck position updates for trucks with `estado = 'disponible'` |
| `viajes:solicitados` | Any authenticated chofer | New trip requests |
| `viaje:{viajeId}` | Owner (usuario), assigned chofer, admin | State changes, chofer assignment |
| `camion:{camionId}` | Owner chofer, admin | Position updates, state changes |
| `user:{userId}:viajes` | Owner only | Updates to any of their trips |

### 10.4 Event Flow

When a backend service modifies data (e.g., trip accepted), it:
1. Commits the database transaction
2. Publishes an event to Redis pub/sub: `{ channel, event_type, data }`
3. The WebSocket server receives the Redis message
4. For each connected client subscribed to that channel: send the event

### 10.5 Reconnection

- Client uses exponential backoff: 1s, 2s, 4s, 8s, max 30s
- On reconnect, client re-subscribes to channels
- Server can send a `snapshot` event with current state to avoid stale data

### 10.6 Event Payload Filtering

Events are filtered before sending to ensure clients only receive data they're authorized to see. For example, a `camiones:disponibles` event strips sensitive fields (CUIT, telefono) before broadcasting.

---

## 11. Security Model

### 11.1 Rate Limiting

| Endpoint Group | Limit | Window | Store |
|---------------|-------|--------|-------|
| `POST /auth/login` | 5 per email | 15 min | Redis |
| `POST /auth/register` | 3 per IP | 1 hour | Redis |
| `POST /auth/verify-email` | 5 per email | 15 min | Redis |
| `POST /auth/resend-otp` | 3 per email | 15 min | Redis |
| `POST /auth/forgot-password` | 3 per email | 1 hour | Redis |
| `POST /auth/reset-password` | 5 per IP | 1 hour | Redis |
| API (general) | 100 per user | 1 min | Redis |
| `POST /viajes` | 5 per user | 5 min | Redis |
| `POST /viajes/:id/aceptar` | 10 per user | 1 min | Redis |
| `PATCH /camiones/:id/ubicacion` | 12 per truck | 1 min | Redis |
| WebSocket messages | 60 per connection | 1 min | In-memory |

### 11.2 CORS

```javascript
{
  origin: process.env.FRONTEND_URL,      // e.g., 'https://nexvia.com.ar'
  credentials: true,                      // allow cookies
  methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'],
  allowedHeaders: ['Content-Type'],
  maxAge: 86400                           // preflight cache 24h
}
```

**NEVER** `Access-Control-Allow-Origin: *` with `credentials: true`.

### 11.3 Security Headers (via Helmet)

```
Strict-Transport-Security: max-age=63072000; includeSubDomains
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 0
Referrer-Policy: strict-origin-when-cross-origin
Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https://*.tile.openstreetmap.org; connect-src 'self' wss://{domain} https://nominatim.openstreetmap.org https://router.project-osrm.org
```

### 11.4 Secret Management

**Environment variables** (`.env` file, never committed):

```env
# Database
DATABASE_URL=postgresql://user:pass@host:5432/nexvia
DATABASE_POOL_MIN=2
DATABASE_POOL_MAX=10

# Redis
REDIS_URL=redis://host:6379

# Session
SESSION_SECRET=<random-64-char-hex>

# Google OAuth
GOOGLE_CLIENT_ID=<from-google-console>
GOOGLE_CLIENT_SECRET=<from-google-console>

# Email
SMTP_HOST=<smtp-host>
SMTP_PORT=587
SMTP_USER=<email>
SMTP_PASS=<password>
SMTP_FROM=noreply@nexvia.com.ar

# App
FRONTEND_URL=https://nexvia.com.ar
NODE_ENV=production
PORT=3000
```

**`.env.example`** committed with placeholder values and descriptions, no secrets.

**Frontend environment variables**: Only `VITE_API_URL` (the backend URL). NO secrets, NO database URLs, NO API keys. Everything `VITE_*` is public.

**Production secrets**: Managed via hosting platform's secret management (Railway secrets, Render env vars, AWS SSM, etc.). Never in `.env` files in production.

**Rotation strategy**: 
- `SESSION_SECRET`: rotate by adding new secret to an array, accepting sessions signed with any recent secret, and removing old ones after session TTL (7 days)
- `GOOGLE_CLIENT_SECRET`: rotate via Google Console, deploy new value, restart
- Database credentials: rotate via hosting platform, update connection string, restart

### 11.5 Input Sanitization

- All user input is validated via Zod schemas (type, format, length limits)
- SQL injection prevented by parameterized queries (NEVER string concatenation)
- XSS prevented by React's default escaping + CSP headers
- No `dangerouslySetInnerHTML` usage
- File uploads (if added later): validated MIME type, size limit, stored with random names

### 11.6 HTTPS

- All traffic over HTTPS in production (enforced by hosting platform or reverse proxy)
- HTTP → HTTPS redirect at infrastructure level
- Secure cookie flag prevents cookie transmission over HTTP

---

## 12. Migration Strategy

### 12.1 Overall Approach: Parallel Build + Cutover

The migration follows a "build new alongside old, then switch" strategy rather than incremental replacement. This is because Base44 is deeply integrated (auth, database, realtime, functions) and cannot be replaced piecemeal.

```
Phase 1: Build backend (independent of Base44)
Phase 2: Build new API client layer (replaces base44Client.js)
Phase 3: Migrate frontend components one-by-one
Phase 4: Migrate data
Phase 5: Validate in staging
Phase 6: Cutover
Phase 7: Remove Base44 dependencies
Phase 8: Post-migration cleanup
```

### 12.2 Development Environment

```
nexvia/
├── server/                  ← NEW: backend
│   ├── src/
│   │   ├── routes/
│   │   ├── services/
│   │   ├── repositories/
│   │   ├── middleware/
│   │   ├── validators/
│   │   ├── websocket/
│   │   └── index.ts
│   ├── migrations/
│   ├── tests/
│   ├── package.json
│   └── tsconfig.json
├── src/                     ← EXISTING: frontend (modified in place)
│   ├── api/
│   │   ├── base44Client.js  ← REMOVED after migration
│   │   └── client.ts        ← NEW: API client
│   ├── ...
├── base44/                  ← REMOVED after migration
├── package.json             ← EXISTING: frontend deps
└── vite.config.js           ← MODIFIED: remove Base44 plugin
```

The backend is a separate Node.js project in `server/` with its own `package.json` and `tsconfig.json`. During development, Vite proxies `/api` requests to the backend server.

---

## 13. Data Migration Strategy

### 13.1 Entity Mapping

| Base44 Entity | PostgreSQL Table | Notes |
|--------------|-----------------|-------|
| User | users | Password hashes likely NOT exportable from Base44 |
| Viaje | viajes | Direct field mapping with adjustments |
| Camion | camiones | Direct field mapping |
| Configuracion | configuracion | Direct field mapping |
| LugarGuardado | lugares_guardados | Direct field mapping |

### 13.2 User Migration

**Critical issue**: Base44 almost certainly does NOT expose password hashes for export. This means:

**Strategy: Forced password reset for all users**

1. Export user data from Base44 (email, full_name, role, google_id if available)
2. Import into `users` table with `password_hash = NULL` and `email_verified = TRUE` (they were already verified on Base44)
3. Users who registered with Google can log in immediately via Google OAuth (their `google_id` links)
4. Users who registered with email/password:
   - On first login attempt, they get a message: "We've upgraded our security. Please reset your password."
   - This redirects them to the password reset flow
   - They receive a reset email and set a new password
5. Admin users are pre-configured in the migration seed

**Communication plan**: Email all users before cutover explaining the security upgrade and the one-time password reset requirement.

### 13.3 Data Export

```
1. Export Base44 entities via SDK or API:
   - base44.entities.User.list()
   - base44.entities.Viaje.list()
   - base44.entities.Camion.list()
   - base44.entities.Configuracion.list()
   - base44.entities.LugarGuardado.list()

2. Write to JSON files: users.json, viajes.json, etc.

3. Transform:
   - Map Base44 IDs to UUIDs (maintain a mapping table for FK resolution)
   - Convert timestamps to TIMESTAMPTZ
   - Validate all data against Zod schemas
   - Flag invalid records for manual review

4. Import via SQL:
   - INSERT into PostgreSQL tables
   - Resolve foreign keys using ID mapping
   - Verify referential integrity
   - Verify record counts match
```

### 13.4 Media Migration

The logo at `media.base44.com/images/public/...` must be:
1. Downloaded
2. Stored in self-hosted static storage (or S3)
3. URL references in `Layout.jsx` and `Bienvenida.jsx` updated to new location

### 13.5 Data Validation

After import:
- Count records per table: must match Base44 counts
- Verify FK integrity: no orphaned viajes, no missing user references
- Verify enum values: all estados, roles, tipos are valid
- Run a sample of 10 viajes through the price calculation to verify consistency
- Verify all users have valid email format
- Verify all coordinates are within Argentina bounds (roughly lat: -55 to -21, lng: -74 to -53)

---

## 14. Base44 Removal Strategy

### 14.1 Items to Remove

| Item | File(s) | Replacement |
|------|---------|-------------|
| `@base44/sdk` | `package.json` | Own API client |
| `@base44/vite-plugin` | `package.json`, `vite.config.js` | Remove entirely |
| `base44Client.js` | `src/api/base44Client.js` | `src/api/client.ts` |
| `app-params.js` | `src/lib/app-params.js` | `src/lib/config.ts` (simple env var reading) |
| `base44.auth.*` calls | `AuthContext.jsx`, `Login.jsx`, `Register.jsx`, `ForgotPassword.jsx`, `ResetPassword.jsx`, `Bienvenida.jsx` | Own auth API calls |
| `base44.entities.*` calls | `Home.jsx`, `Chofer.jsx`, `Admin.jsx`, `BuscarLugar.jsx`, `UsuariosSection.jsx`, `Bienvenida.jsx` | React Query hooks calling own REST API |
| `base44.entities.*.subscribe()` | `Home.jsx`, `Chofer.jsx` | WebSocket subscription hooks |
| `base44.functions.invoke()` | `Bienvenida.jsx` | `POST /api/v1/auth/choose-role` |
| `base44.users.inviteUser()` | `UsuariosSection.jsx` | `POST /api/v1/admin/users/invite` |
| `base44/` directory | `base44/config.jsonc`, `base44/entities/*.jsonc`, `base44/functions/asignarRol/entry.ts` | PostgreSQL schema + backend services |
| `media.base44.com` URLs | `Layout.jsx`, `Bienvenida.jsx` | Self-hosted static assets |
| `localStorage base44_*` keys | `app-params.js`, `AuthContext.jsx` | HttpOnly cookies (no client-side token storage) |

### 14.2 Removal Rules

1. **No item is removed until its replacement is verified working**
2. Removal is done per-file, one component at a time
3. After each file is migrated, grep for remaining Base44 references
4. Final verification (§14.3) must pass before considering migration complete

### 14.3 "Base44 = 0" Verification

```bash
# Must all return 0 matches:
grep -ri "base44" src/ --include="*.jsx" --include="*.js" --include="*.ts" --include="*.tsx"
grep -ri "base44" package.json package-lock.json
grep -ri "base44" vite.config.js
grep -ri "base44" .env .env.*
grep -ri "media\.base44\.com" src/
ls base44/  # directory must not exist

# Network verification (in browser devtools):
# No requests to *.base44.com or *.base44.app domains
```

---

## 15. Testing Strategy

### 15.1 Backend Tests

**Unit tests** (per service):
- ViajeService: state machine transitions (valid + invalid)
- PricingService: price calculation for all tariff types
- AuthService: password hashing, session management

**Integration tests** (with real PostgreSQL):
- Full trip lifecycle: create → accept → en_camino → complete
- Transaction isolation: concurrent acceptance
- Auth flow: register → verify → login → authenticated request

**Security tests** (negative tests):

| Test | Description |
|------|-------------|
| `usuario` cannot set `role` to `admin` | POST /auth/choose-role with role=admin → 400 |
| `usuario` cannot access others' trips | GET /viajes/:otherId → 403 |
| `usuario` cannot modify price | POST /viajes with manipulated price → price is recalculated |
| `usuario` cannot set arbitrary estado | PATCH with estado=completado → 400 |
| `chofer` cannot accept two trips | Second accept → 409 Conflict |
| `chofer` cannot accept already-taken trip | Concurrent accept → 409 Conflict |
| `chofer` cannot modify price | Any price-modifying request → 400 |
| `chofer` cannot change trip owner | Update with different usuario_id → ignored/400 |
| `chofer` cannot complete trip instantly | Complete immediately after en_camino → 400 (time check) |
| Unauthenticated access | Any API without cookie → 401 |
| Expired session | Request with expired session → 401 |
| Rate limit enforcement | 6th login attempt → 429 |
| SQL injection | `' OR 1=1 --` in fields → no effect (parameterized queries) |
| Mass assignment | Extra fields in request body → ignored |
| IDOR | Access camion/lugar belonging to another user → 403 |

### 15.2 Frontend Tests

- Verify API client correctly sends requests and handles responses
- Verify auth state management (login/logout/session expiry)
- Verify WebSocket reconnection behavior
- Verify error handling for all API error codes

### 15.3 End-to-End Tests

Manual testing checklist per phase:
1. Register → verify email → choose role → see correct home page
2. Login with email/password → see correct role-based content
3. Login with Google → linked account or new account
4. Request a trip → see it in chofer panel → accept → track → complete
5. Admin: manage config, trucks, users, roles
6. Password reset flow
7. Concurrent acceptance attempt
8. Session expiry / logout

---

## 16. Deployment Strategy

### 16.1 Infrastructure Requirements

| Component | Requirement |
|-----------|------------|
| Node.js server | Node 20 LTS, 512MB+ RAM |
| PostgreSQL | v15+, 1GB+ storage |
| Redis | v7+, 128MB+ RAM |
| Frontend static files | CDN or static hosting |
| SMTP | Transactional email service |
| Domain | DNS configuration for API + frontend |
| SSL | TLS certificate (Let's Encrypt or hosting-provided) |

### 16.2 Deployment Architecture

**Option A — Single-server (VPS)**:
```
[Nginx reverse proxy]
  ├── / → static files (frontend dist/)
  ├── /api → Node.js backend (PM2)
  └── /ws → WebSocket upgrade → Node.js
[PostgreSQL on same or separate server]
[Redis on same server]
```

**Option B — PaaS (Railway, Render, Fly.io)**:
```
[Frontend] → Static hosting / CDN
[Backend]  → PaaS container (auto-scaled)
[PostgreSQL] → Managed database (hosting-provided)
[Redis] → Managed Redis (hosting-provided)
```

Decision on hosting platform is deferred — see [Open Questions](#20-open-questions).

### 16.3 Vite Configuration (Post-Migration)

```javascript
// vite.config.js (after Base44 removal)
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:3000',
        changeOrigin: true,
      },
      '/ws': {
        target: 'ws://localhost:3000',
        ws: true,
      },
    },
  },
});
```

---

## 17. Rollback Strategy

### 17.1 Per-Phase Rollback

Each implementation phase has a rollback plan:

| Phase | Rollback |
|-------|----------|
| Backend built | No rollback needed — it's additive, doesn't touch frontend |
| API client created | Revert to `base44Client.js` imports |
| Component X migrated | Revert component file to pre-migration version (git revert) |
| Data migrated | Restore Base44 as primary, discard new DB |
| Cutover | Switch DNS/config back to Base44, restore frontend from git |
| Base44 removed | Revert removal commits (git revert) |

### 17.2 Data Rollback

During the cutover period:
1. Base44 is set to read-only (if possible) or documented as "no longer primary"
2. New PostgreSQL is primary
3. If critical issues found within 48 hours: switch back to Base44, accept data loss since cutover
4. After 48 hours with no issues: Base44 data is archived and access is removed

### 17.3 Git Strategy

- All migration work on a `migration` branch
- Each phase is a series of commits on this branch
- Main branch remains on Base44 until cutover is validated
- After cutover: merge `migration` into `main`
- Tag `v1.0.0-pre-migration` on the last Base44 commit for easy rollback reference

---

## 18. Implementation Phases

### Phase 0: Preparation
**Objective**: Set up development infrastructure.
**Changes**:
- Initialize `server/` directory with `package.json`, `tsconfig.json`
- Install backend dependencies
- Set up PostgreSQL locally (or Docker)
- Set up Redis locally (or Docker)
- Create `.env.example`
- Create migration runner utility
- Run migration `001_initial_schema.sql`
**Dependencies**: None
**Tests**: Migration runs successfully, tables exist
**Criteria**: Backend project scaffolded, database schema applied
**Rollback**: Delete `server/` directory

### Phase 1: Authentication Backend
**Objective**: Complete auth system without touching frontend.
**Changes**:
- Auth routes: register, verify-email, login, logout, forgot-password, reset-password, me
- Session middleware (Redis)
- Password hashing (bcrypt)
- Rate limiting (Redis)
- Email sending (nodemailer)
- Google OAuth (passport)
**Files**: `server/src/routes/auth.ts`, `server/src/services/auth.ts`, `server/src/middleware/session.ts`, etc.
**Dependencies**: Phase 0
**Tests**: All auth flows tested via HTTP client (curl/httpie/Postman)
**Criteria**: Can register, verify, login, get /me, logout, reset password — all via API
**Rollback**: Backend is independent — no frontend impact

### Phase 2: Core API Backend
**Objective**: All CRUD and business logic endpoints.
**Changes**:
- Viaje routes + service (create, list, accept, cancel, state transitions)
- Camion routes + service
- Configuracion routes + service
- LugarGuardado routes + service
- Admin routes (user management)
- Pricing service
- Input validation (Zod)
- Authorization middleware
- Audit logging
**Dependencies**: Phase 1
**Tests**: Full API test suite including security negative tests
**Criteria**: All endpoints work, authorization enforced, state machine validated
**Rollback**: Backend is independent

### Phase 3: Realtime Backend
**Objective**: WebSocket server with authenticated pub/sub.
**Changes**:
- WebSocket upgrade handler
- Channel subscription with authorization
- Redis pub/sub integration
- Event publishing from services
**Dependencies**: Phase 2
**Tests**: WebSocket connection, subscription, event receipt
**Criteria**: Real-time events flow from API mutations to subscribed clients
**Rollback**: Backend is independent

### Phase 4: Frontend API Client
**Objective**: Create new API client module alongside (not replacing) Base44 client.
**Changes**:
- `src/api/client.ts` — fetch-based API client with typed methods
- `src/hooks/useViajes.ts`, `src/hooks/useCamiones.ts`, etc. — React Query hooks
- `src/hooks/useWebSocket.ts` — WebSocket subscription hook
- `src/lib/config.ts` — environment config (replaces `app-params.js`)
**Files affected**: New files only, Base44 client untouched
**Dependencies**: Phase 3
**Tests**: Frontend can call new backend (manual test with Vite proxy)
**Criteria**: API client works, hooks work, WebSocket subscription works
**Rollback**: Delete new files

### Phase 5: Migrate Auth UI
**Objective**: Switch auth pages from Base44 to own backend.
**Changes**:
- `AuthContext.jsx` → uses own `/api/v1/auth/me` instead of `base44.auth.me()`
- `Login.jsx` → calls own `/api/v1/auth/login`
- `Register.jsx` → calls own `/api/v1/auth/register` + verify
- `ForgotPassword.jsx` → calls own `/api/v1/auth/forgot-password`
- `ResetPassword.jsx` → calls own `/api/v1/auth/reset-password`
- `Bienvenida.jsx` → calls own `/api/v1/auth/choose-role` + camion creation
- Google OAuth button → redirects to `/api/v1/auth/google`
**Dependencies**: Phase 4
**Tests**: All auth flows in browser
**Criteria**: Can register, login, logout, reset password through new backend
**Rollback**: Revert modified files to Base44 versions

### Phase 6: Migrate Home (Usuario)
**Objective**: Switch Home page from Base44 entities to own API.
**Changes**:
- `Home.jsx` → uses React Query hooks instead of `base44.entities.*`
- Replace `base44.entities.Viaje.create()` with `POST /api/v1/viajes`
- Replace `base44.entities.Camion.filter()` with `GET /api/v1/camiones/disponibles`
- Replace `base44.entities.*.subscribe()` with WebSocket hooks
- Remove client-side price/distance as authoritative (show as estimate, server calculates)
**Dependencies**: Phase 5
**Tests**: Full trip request flow in browser
**Criteria**: Usuario can request a trip, see trucks, track trip status
**Rollback**: Revert `Home.jsx`

### Phase 7: Migrate Chofer
**Objective**: Switch Chofer page from Base44 to own API.
**Changes**:
- `Chofer.jsx` → uses React Query hooks
- Trip acceptance via `POST /api/v1/viajes/:id/aceptar`
- GPS tracking via `PATCH /api/v1/camiones/:id/ubicacion`
- State transitions via dedicated endpoints
- Remove auto-complete on GPS proximity (replace with explicit button)
**Dependencies**: Phase 6
**Tests**: Full chofer flow: see requests, accept, navigate, complete
**Criteria**: Chofer can accept and complete trips via new backend
**Rollback**: Revert `Chofer.jsx`

### Phase 8: Migrate Admin
**Objective**: Switch Admin page from Base44 to own API.
**Changes**:
- `Admin.jsx` → uses React Query hooks
- `UsuariosSection.jsx` → uses admin API endpoints
- Config management via `GET/PUT /api/v1/configuracion`
- Truck management via admin camiones endpoints
- User/role management via admin users endpoints
- Invite via `POST /api/v1/admin/users/invite`
**Dependencies**: Phase 7
**Tests**: Full admin flow in browser
**Criteria**: Admin can manage config, trucks, users, roles
**Rollback**: Revert admin files

### Phase 9: Migrate Remaining Components
**Objective**: Migrate BuscarLugar (saved locations) and static assets.
**Changes**:
- `BuscarLugar.jsx` → uses own API for saved locations
- `Layout.jsx`, `Bienvenida.jsx` → replace `media.base44.com` logo URL
- Any remaining Base44 references
**Dependencies**: Phase 8
**Tests**: Search, save locations, UI loads correctly
**Criteria**: No Base44 references remain in source code
**Rollback**: Revert modified files

### Phase 10: Data Migration
**Objective**: Export data from Base44 and import into PostgreSQL.
**Changes**:
- Write migration script (in `server/scripts/`)
- Export all Base44 entities
- Transform and import into PostgreSQL
- Verify data integrity
**Dependencies**: Phases 0–9
**Tests**: Data validation checks (counts, FK integrity, etc.)
**Criteria**: All data migrated, verified, and consistent
**Rollback**: Drop and recreate PostgreSQL tables

### Phase 11: Base44 Removal
**Objective**: Remove all Base44 dependencies.
**Changes**:
- Remove `@base44/sdk` and `@base44/vite-plugin` from `package.json`
- Remove `base44Client.js` and `app-params.js`
- Remove `base44/` directory
- Update `vite.config.js` (remove Base44 plugin)
- Remove any remaining Base44 env vars
- Run `npm install` to update lockfile
**Dependencies**: Phase 10
**Tests**: Build succeeds, app runs without Base44
**Criteria**: "Base44 = 0" verification passes (§14.3)
**Rollback**: Revert removals from git

### Phase 12: Staging Validation & Cutover
**Objective**: Deploy to staging, validate, then deploy to production.
**Changes**:
- Deploy backend + frontend to staging environment
- Run full test suite
- Communication to users about password reset
- Deploy to production
- Monitor for 48 hours
**Dependencies**: Phase 11
**Tests**: E2E testing in staging
**Criteria**: All functionality works, no Base44 dependencies, all tests pass
**Rollback**: Switch back to Base44 (see §17)

---

## 19. Acceptance Criteria

### Global Criteria

- [ ] Zero Base44 runtime dependencies (§14.3 verification passes)
- [ ] All existing functionality preserved (trip request, acceptance, tracking, completion, admin management)
- [ ] All three roles (usuario, chofer, admin) work correctly
- [ ] Authentication works (email/password, Google OAuth, OTP, password reset)
- [ ] Real-time updates work (truck positions, trip status changes)
- [ ] Build succeeds without warnings related to missing dependencies
- [ ] No `base44` references in source, package.json, lockfile, or env files
- [ ] No network requests to `*.base44.com` or `*.base44.app`

### Security Criteria

- [ ] Passwords hashed with bcrypt (cost 12+)
- [ ] Sessions stored in Redis, cookie is HttpOnly/Secure/SameSite
- [ ] No tokens in localStorage
- [ ] Rate limiting on all auth endpoints
- [ ] Price calculated server-side only
- [ ] Distance calculated server-side (or validated)
- [ ] State transitions enforced by backend state machine
- [ ] Trip acceptance is transactional with row locking
- [ ] Authorization checked on every endpoint (role + ownership)
- [ ] No mass assignment vulnerabilities
- [ ] Audit log records all critical operations
- [ ] Sensitive data (CUIT, telefono) not exposed to unauthorized roles
- [ ] All security negative tests pass
- [ ] CORS restricted to frontend origin only
- [ ] Security headers set (via Helmet)
- [ ] No secrets in frontend code or `VITE_*` variables

### Performance Criteria

- [ ] API response time < 200ms for CRUD operations
- [ ] WebSocket events delivered within 500ms of database commit
- [ ] GPS position updates processed at ≤ 1/5s per truck
- [ ] Frontend bundle size not significantly larger than current

---

## 20. Open Questions

### Q1: Hosting Platform
**Question**: Where will the backend, database, and Redis be hosted?
**Options**: Railway, Render, Fly.io, DigitalOcean VPS, AWS (EC2+RDS+ElastiCache)
**Impact**: Affects deployment scripts, secret management, CI/CD, cost
**Recommendation**: Railway or Render for simplicity; can migrate to AWS later if needed
**Decision needed before**: Phase 12 (staging deployment)

### Q2: Email Provider
**Question**: What SMTP service will be used for transactional emails?
**Options**: Resend, SendGrid, AWS SES, Mailgun, self-hosted
**Impact**: Affects email deliverability, cost, setup complexity
**Recommendation**: Resend (simple API, good deliverability, free tier for low volume)
**Decision needed before**: Phase 1 (auth backend)

### Q3: Base44 Data Export
**Question**: Does Base44 provide an export API for all entity data? Can we export user records including google_id?
**Impact**: Affects data migration strategy (Phase 10)
**Action**: Test the export capabilities before Phase 10
**Decision needed before**: Phase 10

### Q4: Stripe Integration
**Question**: Stripe dependencies exist in `package.json` but are unused in current code. Is payment processing planned?
**Impact**: May need payment endpoints in the API
**Decision needed before**: Phase 2 (or deferred to post-migration)

### Q5: Domain and SSL
**Question**: What domain will NEXVIA use? Is DNS already configured?
**Impact**: CORS configuration, cookie domain, SSL certificate, Google OAuth redirect URI
**Decision needed before**: Phase 12

### Q6: Concurrent Trip Acceptance UX
**Question**: When a chofer tries to accept a trip that was already taken, what UX should the frontend show?
**Current behavior**: Unknown (race condition exists in Base44 version)
**Recommendation**: Show a toast "This trip was already taken by another driver" and auto-refresh the list
**Decision needed before**: Phase 7

### Q7: Trip Cancellation by Chofer
**Question**: Can a chofer cancel a trip after accepting it? Under what conditions?
**Current behavior**: No cancellation path exists for chofer after acceptance
**Recommendation**: Allow cancellation within 5 minutes of acceptance; after that, admin-only
**Decision needed before**: Phase 7

### Q8: Historical Data Retention
**Question**: How long should completed/cancelled trip data be retained?
**Impact**: Database size, compliance, analytics
**Recommendation**: Keep all data; add archival later if needed
**Decision needed before**: Phase 5 (database schema)

---

## Appendix A: Base44 SDK Methods Used

Complete inventory of `base44.*` calls in the codebase:

| Method | Used in | Replacement |
|--------|---------|-------------|
| `base44.auth.me()` | `AuthContext.jsx` | `GET /api/v1/auth/me` |
| `base44.auth.loginViaEmailPassword()` | `Login.jsx` | `POST /api/v1/auth/login` |
| `base44.auth.register()` | `Register.jsx` | `POST /api/v1/auth/register` |
| `base44.auth.verifyOtp()` | `Register.jsx` | `POST /api/v1/auth/verify-email` |
| `base44.auth.resendOtp()` | `Register.jsx` | `POST /api/v1/auth/resend-otp` |
| `base44.auth.setToken()` | `Register.jsx` | Not needed (HttpOnly cookie set by server) |
| `base44.auth.loginWithProvider()` | `Login.jsx`, `Register.jsx` | `GET /api/v1/auth/google` redirect |
| `base44.auth.logout()` | `AuthContext.jsx` | `POST /api/v1/auth/logout` |
| `base44.auth.redirectToLogin()` | `AuthContext.jsx` | `navigate('/login')` |
| `base44.auth.resetPasswordRequest()` | `ForgotPassword.jsx` | `POST /api/v1/auth/forgot-password` |
| `base44.auth.resetPassword()` | `ResetPassword.jsx` | `POST /api/v1/auth/reset-password` |
| `base44.entities.Viaje.create()` | `Home.jsx` | `POST /api/v1/viajes` |
| `base44.entities.Viaje.filter()` | `Home.jsx`, `Chofer.jsx` | `GET /api/v1/viajes/*` |
| `base44.entities.Viaje.update()` | `Home.jsx`, `Chofer.jsx` | `POST /api/v1/viajes/:id/{action}` |
| `base44.entities.Viaje.subscribe()` | `Home.jsx`, `Chofer.jsx` | WebSocket subscription |
| `base44.entities.Camion.filter()` | `Home.jsx`, `Chofer.jsx` | `GET /api/v1/camiones/*` |
| `base44.entities.Camion.get()` | `Home.jsx` | `GET /api/v1/camiones/:id` |
| `base44.entities.Camion.create()` | `Admin.jsx`, `Bienvenida.jsx` | `POST /api/v1/camiones` |
| `base44.entities.Camion.update()` | `Chofer.jsx`, `UsuariosSection.jsx` | `PATCH /api/v1/camiones/:id/*` |
| `base44.entities.Camion.delete()` | `Admin.jsx` | `DELETE /api/v1/camiones/:id` |
| `base44.entities.Camion.list()` | `Admin.jsx` | `GET /api/v1/camiones` |
| `base44.entities.Camion.subscribe()` | `Home.jsx`, `Chofer.jsx` | WebSocket subscription |
| `base44.entities.Configuracion.list()` | `Home.jsx`, `Chofer.jsx`, `Admin.jsx` | `GET /api/v1/configuracion` |
| `base44.entities.Configuracion.create()` | `Admin.jsx` | `PUT /api/v1/configuracion` (upsert) |
| `base44.entities.Configuracion.update()` | `Admin.jsx` | `PUT /api/v1/configuracion` |
| `base44.entities.LugarGuardado.list()` | `BuscarLugar.jsx` | `GET /api/v1/lugares` |
| `base44.entities.LugarGuardado.create()` | `BuscarLugar.jsx` | `POST /api/v1/lugares` |
| `base44.entities.User.list()` | `UsuariosSection.jsx` | `GET /api/v1/admin/users` |
| `base44.entities.User.update()` | `UsuariosSection.jsx` | `PUT /api/v1/admin/users/:id/role` |
| `base44.functions.invoke('asignarRol')` | `Bienvenida.jsx` | `POST /api/v1/auth/choose-role` |
| `base44.users.inviteUser()` | `UsuariosSection.jsx` | `POST /api/v1/admin/users/invite` |

## Appendix B: File-by-File Migration Impact

| File | Base44 Dependencies | Migration Effort |
|------|---------------------|-----------------|
| `src/api/base44Client.js` | Core dependency | Replace with `client.ts` |
| `src/lib/app-params.js` | Base44 token/config | Replace with `config.ts` |
| `src/lib/AuthContext.jsx` | `base44.auth.me()`, `base44.auth.logout()`, `createAxiosClient` | High — rewrite auth state management |
| `src/pages/Login.jsx` | `base44.auth.loginViaEmailPassword()`, `base44.auth.loginWithProvider()` | Medium |
| `src/pages/Register.jsx` | `base44.auth.register()`, `base44.auth.verifyOtp()`, `base44.auth.resendOtp()`, `base44.auth.setToken()`, `base44.auth.loginWithProvider()` | Medium |
| `src/pages/ForgotPassword.jsx` | `base44.auth.resetPasswordRequest()` | Low |
| `src/pages/ResetPassword.jsx` | `base44.auth.resetPassword()` | Low |
| `src/pages/Bienvenida.jsx` | `base44.functions.invoke('asignarRol')`, `base44.entities.Camion.create()` | Medium |
| `src/pages/Home.jsx` | `base44.entities.Viaje.*`, `base44.entities.Camion.*`, `base44.entities.Configuracion.*`, `.subscribe()` | High |
| `src/pages/Chofer.jsx` | `base44.entities.Viaje.*`, `base44.entities.Camion.*`, `base44.entities.Configuracion.*`, `.subscribe()` | High |
| `src/pages/Admin.jsx` | `base44.entities.Configuracion.*`, `base44.entities.Camion.*` | Medium |
| `src/components/admin/UsuariosSection.jsx` | `base44.entities.User.*`, `base44.users.inviteUser()`, `base44.entities.Camion.update()` | Medium |
| `src/components/mapa/BuscarLugar.jsx` | `base44.entities.LugarGuardado.*` | Low |
| `src/components/Layout.jsx` | `media.base44.com` URL | Low (URL change only) |
| `vite.config.js` | `@base44/vite-plugin` | Low (remove plugin) |
| `base44/` directory | Entire directory | Delete after migration complete |

---

*This document is the blueprint for NEXVIA's migration from Base44 to a self-controlled, secure architecture. No implementation should begin without reviewing and approving this plan. Every decision above is informed by direct inspection of the current codebase.*
