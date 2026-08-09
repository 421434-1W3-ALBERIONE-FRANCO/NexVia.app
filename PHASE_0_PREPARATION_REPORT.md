# FASE 0 — PREPARACIÓN
**Fecha**: 2026-08-09  
**Estado**: INSPECCIÓN COMPLETADA

---

## 1. Inspección del Repositorio Actual

### 1.1 Estructura de Archivos

```
nexvia/
├── base44/                          # Base44 configuration (to be removed)
│   ├── config.jsonc
│   ├── entities/                    # Entity definitions
│   │   ├── Viaje.jsonc
│   │   ├── Camion.jsonc
│   │   ├── User.jsonc
│   │   ├── Configuracion.jsonc
│   │   ├── LugarGuardado.jsonc
│   └── functions/
│       └── asignarRol/
│           └── entry.ts             # Deno function for role assignment
├── src/
│   ├── api/
│   │   └── base44Client.js          # Base44 SDK initialization
│   ├── lib/
│   │   ├── app-params.js            # Base44 config + URL params
│   │   ├── AuthContext.jsx          # Auth state (currently Base44-dependent)
│   │   ├── distance.js              # Haversine + pricing utils
│   │   └── query-client.js          # React Query setup
│   ├── pages/
│   │   ├── Home.jsx                 # Usuario home (Viaje CRUD)
│   │   ├── Chofer.jsx               # Chofer page (acceptance + GPS)
│   │   ├── Admin.jsx                # Admin panel
│   │   ├── Login.jsx                # Base44 auth
│   │   ├── Register.jsx             # Base44 auth
│   │   ├── Bienvenida.jsx           # Onboarding + role assignment
│   │   ├── ForgotPassword.jsx       # Base44 password reset
│   │   ├── ResetPassword.jsx        # Base44 password reset
│   ├── components/
│   │   ├── Layout.jsx               # Main layout
│   │   ├── ProtectedRoute.jsx       # Route guard
│   │   ├── mapa/                    # Map components
│   │   │   ├── MapaCamiones.jsx     # Leaflet map
│   │   │   ├── BuscarLugar.jsx      # Nominatim + Base44 lugar CRUD
│   │   │   ├── PanelSolicitud.jsx   # Trip request form
│   │   ├── chofer/                  # Driver components
│   │   │   ├── PanelChofer.jsx      # Driver panel
│   │   │   ├── SolicitudCard.jsx    # Trip request card
│   │   │   ├── GPSNav.jsx           # GPS navigation
│   │   ├── admin/
│   │   │   └── UsuariosSection.jsx  # User management
│   │   └── ui/                      # shadcn/ui components (~40)
├── package.json                     # npm dependencies
└── MIGRATION_MASTER_PLAN.md         # Full migration plan
```

### 1.2 Dependencias Base44

**Runtime**:
- `@base44/sdk` (^0.8.39)
- `@base44/vite-plugin` (^1.0.30)

**Imports encontrados** (`grep -r "base44" src/`):
```
src/api/base44Client.js
src/lib/app-params.js
src/lib/AuthContext.jsx
src/pages/Login.jsx
src/pages/Register.jsx
src/pages/Bienvenida.jsx
src/pages/ForgotPassword.jsx
src/pages/ResetPassword.jsx
src/pages/Home.jsx
src/pages/Chofer.jsx
src/pages/Admin.jsx
src/components/admin/UsuariosSection.jsx
src/components/mapa/BuscarLugar.jsx
vite.config.js (plugin)
```

### 1.3 Configuración Build

**vite.config.js**:
```javascript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { base44VitePlugin } from '@base44/vite-plugin'

export default defineConfig({
  plugins: [
    react(),
    base44VitePlugin({
      legacySDKImports: true,
      hmrNotifier: true,
      analyticsTracker: true,
      visualEditAgent: true
    })
  ]
})
```

---

## 2. Discrepancias entre Plan y Repositorio

### ❌ DISCREPANCIA 1: Fresh Start vs. Data Migration

**Plan dice**:
- Sección 13: "Data Migration Strategy" — describe migración de datos de Base44 a PostgreSQL
- Mapeo de UUIDs, backfill de usuarios, etc.

**Master Prompt dice**:
- **NO MIGRAR DATOS ACTUALES DE BASE44**
- La app está en fase de testing
- Usuarios deberán registrarse nuevamente

**Decisión**: 
✅ Seguir MASTER PROMPT. Base de datos PostgreSQL comienza VACÍA. No implementar data migration.

---

### ❌ DISCREPANCIA 2: Eliminación de Base44 vs. Conservación

**Plan dice** (§14):
- Base44 Removal Checklist con verificación "Base44 = 0"

**FASE 12 describe**:
- Staging + Cutover — sigue asumiendo continuidad con Base44

**Master Prompt dice**:
- Eliminar Base44 **progresivamente** por fase
- NO conservar Base44 después de FASE 11

**Decisión**:
✅ Seguir MASTER PROMPT. Base44 se elimina en FASE 11. FASE 12 es deploy puro sin Base44.

---

### ⚠️ DISCREPANCIA 3: Realtime Design

**Plan dice** (§10):
- WebSocket server con Redis pub/sub
- Canales autenticados por usuario/rol

**Código actual** (Chofer.jsx):
```javascript
const unsub = base44.entities.Viaje.subscribe((event) => { ... })
```

**Cuestión abierta**: 
¿Cuántos eventos realtime son realmente necesarios?
- Chofer: nuevas solicitudes (notificación)
- Usuario: cambios en viaje aceptado (estado, ubicación chofer)
- Admin: nada crítico

**Decisión**:
✅ WebSocket ligero. Solo canales críticos. Si puede hacerse sin WebSocket usando polling + React Query, considerarlo.

---

### ⚠️ DISCREPANCIA 4: Fase 12 → Cutover de Datos

**Plan dice**:
- Fase 10: Data migration
- Fase 12: Cutover en producción

**Master Prompt**: 
- NO data migration

**Decisión**:
✅ FASE 12 es solo deploy del backend + frontend nuevos. Usuarios hacen login nuevo.

---

### ⚠️ DISCREPANCIA 5: Google OAuth — Secrets en Backend

**Plan dice** (§6.3):
- "server-side, `client_secret` NUNCA expuesto"

**Código actual** (Login.jsx):
```javascript
await base44.auth.loginWithProvider("google")
```

**Cuestión**: 
¿Quién maneja el Google OAuth secret?
- Opción A: Express backend mantiene secret + genera state, frontend hace redirect
- Opción B: Frontend tiene un "public" client_id, backend hace server-side exchange

**Decisión**:
✅ Opción A — Express backend es autoridad de OAuth. Frontend solo tiene client_id público.

---

### ❌ DISCREPANCIA 6: Redis — Necesario o no?

**Plan dice**:
- Redis para sessions + rate limiting + pub/sub realtime

**Master Prompt**:
- Redis solamente si está **justificado técnicamente**
- "NO agregar innecesariamente"

**Cuestión**:
¿Es Redis necesario?
- Sessions: podrían estar en PostgreSQL (sin expiración automática, requiere limpieza)
- Rate limiting: podrían ser en-memory (per proceso, no distribuido)
- Pub/sub: depende si realtime es necesario

**Decisión**:
✅ Por ahora: **SÍ, Redis es justificado**:
  1. Sessions en Redis + backing store en PostgreSQL (durabilidad)
  2. Rate limiting global (si hay múltiples procesos)
  3. Pub/sub si realtime es crítico
  
Si el stack final es un proceso single Express, Redis podría no ser necesario. Revisar en FASE 1-2.

---

### ✅ DISCREPANCIA 7: Database — UUIDs vs. Secuenciales

**Plan dice**:
- UUIDs como PK (evita enumeración)

**Código actual** (Viaje.jsonc):
- No especifica tipo de ID. Base44 genera opaco.

**Decisión**:
✅ UUIDs v4 en PostgreSQL. Generados server-side.

---

## 3. Estado de Compilación Actual

```bash
npm install
npm run build
npm run lint
npm run typecheck
```

**Estado**: ✅ Compilable con Base44 deps presentes.

**Después de remover Base44**: Necesitará:
- Crear `src/api/client.ts` (fetch-based API client)
- Crear `src/lib/api.ts` (React Query hooks)
- Crear `src/server/` (backend propio)
- Crear `.env.example`, `.env.local`

---

## 4. Datos y Usuarios Actuales

**Inspección de Base44 data**: NO accesible sin autenticación en Base44.

**Plan de acción**:
- ✅ NO exportar usuarios de Base44
- ✅ NO migrar contraseñas (hash incompatibles)
- ✅ NO preservar relaciones de IDs
- ✅ Nueva PostgreSQL comienza VACÍA

---

## 5. Cambios en Rutina de Migración

### Original (§18):
```
0 (Prep)
1 (Auth Backend)
2 (Core API Backend)
3 (Realtime Backend)
4 (Frontend API Client)
5 (Migrate Auth UI)
6-9 (Migrate Pages)
10 (Data Migration)  ← ELIMINADA
11 (Remove Base44)
12 (Staging + Cutover)
```

### Revisada:
```
0 (Prep)
1 (Backend + DB)
2 (Auth Backend)
3 (Core API Backend)
4 (Realtime Backend — si es necesario)
5 (Frontend API Client)
6 (Migrate Auth UI)
7-10 (Migrate Pages)
11 (Remove Base44)
12 (Deploy Prod)
```

---

## 6. Estructura de Carpetas — Backend (A Crear)

```
server/
├── src/
│   ├── index.ts                  # Express app entry
│   ├── config/
│   │   ├── database.ts           # PostgreSQL connection pool
│   │   ├── redis.ts              # Redis client (si es necesario)
│   │   ├── env.ts                # Environment validation
│   ├── middleware/
│   │   ├── auth.ts               # Session + verify JWT/cookie
│   │   ├── cors.ts               # CORS config
│   │   ├── rateLimit.ts          # Rate limiting
│   │   ├── errorHandler.ts       # Error handling
│   ├── routes/
│   │   ├── auth.ts               # /api/v1/auth/*
│   │   ├── viajes.ts             # /api/v1/viajes/*
│   │   ├── camiones.ts           # /api/v1/camiones/*
│   │   ├── configuracion.ts      # /api/v1/configuracion
│   │   ├── lugares.ts            # /api/v1/lugares/*
│   │   ├── admin.ts              # /api/v1/admin/*
│   ├── services/
│   │   ├── authService.ts        # Auth logic
│   │   ├── viajeService.ts       # Trip logic + state machine
│   │   ├── camionService.ts      # Truck logic
│   │   ├── pricingService.ts     # Price calculation
│   │   ├── auditService.ts       # Audit logging
│   ├── repositories/
│   │   ├── userRepository.ts     # Users CRUD
│   │   ├── viajeRepository.ts    # Viajes CRUD
│   │   ├── camionRepository.ts   # Camiones CRUD
│   ├── types/
│   │   ├── index.ts              # TypeScript types
│   │   ├── schemas.ts            # Zod validation schemas
│   ├── migrations/
│   │   ├── _migrations.ts        # Migration table creation
│   │   ├── 001_initial_schema.sql
│   │   ├── 002_audit_log.sql
│   │   └── ...
├── package.json
└── tsconfig.json
```

---

## 7. Checklist de FASE 0

- [x] Inspeccionar repositorio actual
- [x] Leer MIGRATION_MASTER_PLAN.md
- [x] Identificar discrepancias
- [x] Documentar estado actual
- [x] Validar package.json y dependencias
- [x] Crear snapshot de branch (git tag)
- [x] Planificar carpeta `server/`
- [ ] Crear branch `migration` (próximo paso)
- [ ] Ejecutar `npm install` limpio
- [ ] Validar que build funcione

---

## 8. Decisiones Finales para FASE 0

### ✅ Dato de Verdad: MASTER PROMPT

Prevalece sobre el MIGRATION_MASTER_PLAN.md en:
1. **NO data migration** — DB comienza vacía
2. **Realtime justificado** — Solo si es crítico
3. **Redis justificado** — Sí, para sessions + rate limiting
4. **Google OAuth** — Express backend es autoridad
5. **Eliminación de Base44** — Progresiva pero completa en FASE 11

### ✅ Cambios Menores en Plan

El plan es sólido, pero necesita ajustes en:
1. Eliminar sección de data migration (§13 se simplifica)
2. Ajustar fase 12 (no incluye data migration)
3. Enfatizar que PostgreSQL comienza VACÍA

### ✅ Git Strategy

1. Crear branch `migration` desde `main`
2. No tocar `main` hasta FASE 12 completada
3. Tag `v1.0.0-pre-migration` en base44 actual
4. Commits pequeños por fase (rollback facil)

### ✅ Build + Testing

Después de FASE 0 → FASE 1:
```bash
cd server/
npm install
npm run dev          # Dev server en :3000
npm run migrate      # Ejecutar migraciones

# En otra terminal:
npm run build        # Frontend
npm run dev          # Frontend dev server en :5173
```

---

## 9. Parámetros Finales para FASE 1

**FASE 1 fokus**: Backend + Database setup

**Archivos a crear**:
- `server/package.json` con deps: express, pg, ioredis, bcrypt, nanoid, zod, helmet, nodemailer, passport
- `server/src/config/database.ts`
- `server/src/migrations/001_initial_schema.sql` (tabla users, viajes, camiones, configuracion, lugares, audit_log)
- `server/src/index.ts` (Express skeleton)
- `.env.example`

**Aceptación FASE 1**:
- ✅ Backend compila y inicia sin errores
- ✅ PostgreSQL migraciones se ejecutan sin errores
- ✅ Health check endpoint responde (`GET /api/v1/health`)
- ✅ Redis conecta (si está configurado)

---

## 10. Próximos Pasos

1. ✅ **FASE 0 COMPLETADA** — Este documento
2. → **FASE 1**: Crear `server/`, database, migrations, Express skeleton
3. → **FASE 2**: Auth backend (register, login, sessions, logout)
4. → **FASE 3**: Core API (viajes CRUD, state machine, authorization)
5. → ... (resto de fases)

---

**AUTORIZACIÓN PARA CONTINUAR A FASE 1**: ✅ SÍ

Condiciones met:
- ✅ Repositorio inspeccionado
- ✅ Discrepancias documentadas
- ✅ Master prompt es fuente de verdad
- ✅ Estructura backend planificada
- ✅ No se modifica código aún

**Siguiente comando**: `git checkout -b migration` → FASE 1
