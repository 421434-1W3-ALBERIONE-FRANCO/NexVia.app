# FASE 3 — ARCHITECTURE & DATA FLOWS

Diagramas y flujos de datos para FASE 3 (Core API Backend).

---

## SYSTEM ARCHITECTURE

```
┌─────────────────────────────────────────────────────────────────┐
│                        FRONTEND (React)                         │
│                      (Base44 SSR + Client)                      │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTP/HTTPS
                             │ XHR/Fetch + Cookies
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                    EXPRESS SERVER (Node.js)                     │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │               MIDDLEWARE STACK                           │  │
│  │ ┌────────────────────────────────────────────────────┐  │  │
│  │ │ 1. Helmet (security headers)                       │  │  │
│  │ │ 2. CORS (whitelist localhost:5173)                 │  │  │
│  │ │ 3. Body Parser (JSON/URL-encoded)                  │  │  │
│  │ │ 4. authMiddleware (verify session cookie)          │  │  │
│  │ │ 5. Rate Limiting (Redis-backed)                    │  │  │
│  │ │ 6. Error Handler (global error catcher)            │  │  │
│  │ └────────────────────────────────────────────────────┘  │  │
│  │                                                          │  │
│  │  ┌────────────────┐  ┌────────────────┐  ┌──────────┐  │  │
│  │  │  ROUTES        │  │ SERVICES       │  │REPOSITORIES│  │  │
│  │  │                │  │                │  │           │  │  │
│  │  │ /viajes        │→ │TripService     │→ │TripRepo  │  │  │
│  │  │ /camiones      │→ │TruckService    │→ │TruckRepo │  │  │
│  │  │ /admin         │→ │AdminService    │→ │ConfigRepo│  │  │
│  │  │                │  │AuditService    │  │UserRepo  │  │  │
│  │  │ (auth)         │  │(inherited)     │  │(inherited)   │  │
│  │  └────────────────┘  └────────────────┘  └──────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │               DATA ACCESS LAYER                          │  │
│  │ ┌────────────────────────────────────────────────────┐  │  │
│  │ │ PostgreSQL Connection Pool (max 20)                │  │  │
│  │ │ • viajes (trips)                                   │  │  │
│  │ │ • camiones (trucks)                                │  │  │
│  │ │ • users (inherited from FASE 2)                    │  │  │
│  │ │ • configuracion (singleton)                        │  │  │
│  │ │ • audit_log (append-only)                          │  │  │
│  │ └────────────────────────────────────────────────────┘  │  │
│  │ ┌────────────────────────────────────────────────────┐  │  │
│  │ │ Redis Cache (optional)                             │  │  │
│  │ │ • Sessions (primary storage, TTL 24h)              │  │  │
│  │ │ • Config (cached, TTL 5min)                        │  │  │
│  │ │ • Rate limiter counters                            │  │  │
│  │ └────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## LAYERED ARCHITECTURE

```
┌─────────────────────────────────────────────┐
│        HTTP Layer (Express Routes)           │
│    (Input validation, response formatting)   │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│        Service Layer (Business Logic)        │
│    • Trip calculations (distance, pricing)   │
│    • State transitions (viaje states)        │
│    • Truck availability checks               │
│    • Transactional operations                │
│    • Audit logging                           │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│     Repository Layer (Data Access)           │
│    • Query builders                          │
│    • SQL execution                           │
│    • Transaction management                  │
│    • Optimistic locking                      │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│       Data Layer (PostgreSQL/Redis)          │
│    • Persistence                             │
│    • Indexes                                 │
│    • Transactions                            │
└─────────────────────────────────────────────┘
```

---

## ROLE-BASED ACCESS CONTROL (RBAC)

```
┌──────────────────────────────────────────────────────────┐
│                USER ROLES                                │
└──────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ ADMIN                                                   │
│ • View all users                                        │
│ • Update system configuration (tariffs)                 │
│ • View audit logs                                       │
│ • Manage trucks (soft delete)                           │
│ • View analytics                                        │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ CHOFER (Driver)                                         │
│ • Accept/reject available trips                         │
│ • Update own truck location (GPS)                       │
│ • Mark trip as en_camino / completado                   │
│ • Cancel own trips (if not started)                     │
│ • View assigned truck info                              │
│ • View own trip history                                 │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ USUARIO (End User)                                      │
│ • Create trip requests                                  │
│ • View own trips                                        │
│ • Cancel own trips (if not accepted)                    │
│ • View available trucks/drivers                         │
│ • Update saved locations                                │
│ • View pricing estimates                                │
└─────────────────────────────────────────────────────────┘
```

---

## TRIP STATE MACHINE

```
                    ┌─────────────┐
                    │  solicitado  │ ← START (usuario creates trip)
                    └──────┬──────┘
                           │
                    ┌──────▼─────────┐
                    │ [chofer accepts]│
                    └──────┬──────────┘
                           ▼
                    ┌─────────────┐
                    │  aceptado    │
                    └──────┬──────┘
                           │
                  ┌─────────┴──────────┐
                  │                    │
    ┌─────────────▼────────┐  ┌───────▼──────────┐
    │   [chofer marks en   │  │ [usuario/chofer  │
    │      route]          │  │    cancels]      │
    └─────────────┬────────┘  └───────┬──────────┘
                  │                   │
    ┌─────────────▼────────┐  ┌───────▼──────────┐
    │   en_camino          │  │   cancelado      │
    └─────────────┬────────┘  └──────────────────┘
                  │                   ▲
    ┌─────────────▼────────┐          │
    │ [chofer marks        │          │
    │  completed]          │          │
    └─────────────┬────────┘   [usuario/chofer
                  │             cancels before
    ┌─────────────▼────────┐    accepted]
    │   completado         │
    └──────────────────────┘


STATE RULES:
- solicitado  → aceptado  (chofer accepts)
- solicitado  → cancelado (usuario cancels)
- aceptado    → en_camino (chofer marks en route)
- aceptado    → cancelado (usuario OR chofer cancels)
- en_camino   → completado (chofer marks done)
- (terminal)  → no more transitions

TRUCK SYNC:
- viaje.estado='aceptado'   → truck.estado='ocupado'
- viaje.estado='cancelado'  → truck.estado='disponible'
- viaje.estado='completado' → truck.estado='disponible'
- viaje.estado='en_camino'  → truck.estado='ocupado' (already set)
```

---

## TRUCK AVAILABILITY FLOW

```
                    ┌──────────────────┐
                    │  TRUCK CREATED   │
                    │  estado='inactivo'│
                    └────────┬──────────┘
                             │
                    ┌────────▼─────────┐
                    │ [admin activates]│
                    └────────┬─────────┘
                             │
                    ┌────────▼──────────┐
                    │  disponible       │
                    │ (ready for trips) │
                    └────────┬──────────┘
                             │
                    ┌────────▼────────────────┐
                    │[chofer accepts trip]    │
                    │UPDATE viajes (accept)   │
                    │UPDATE camiones (ocupado)│
                    └────────┬────────────────┘
                             │
                    ┌────────▼──────────┐
                    │  ocupado          │
                    │(on active trip)   │
                    └────────┬──────────┘
                             │
                    ┌────────▼──────────┐
                    │[chofer completes/ │
                    │ cancels trip]     │
                    │UPDATE viajes      │
                    │UPDATE camiones    │
                    └────────┬──────────┘
                             │
                    ┌────────▼──────────┐
                    │  disponible       │
                    │(ready again)      │
                    └──────────────────┘


AVAILABILITY RULES:
- disponible: Can accept new trips
- ocupado: No new trips, active trip in progress
- inactivo: No trips, maintenance or offline
```

---

## TRIP CREATION FLOW

```
┌─────────────────────────────────────────────────────────────┐
│ CLIENT: POST /api/v1/viajes                                 │
│ Body: {                                                     │
│   origen: { lat: -34.6037, lng: -58.3816 },                │
│   destino: { lat: -34.7037, lng: -58.4816 },               │
│   toneladas: 5.5,                                           │
│   carga: "Soja"                                             │
│ }                                                           │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 1. ROUTE HANDLER                                            │
│    - Extract userId from req.user                           │
│    - Validate role = 'usuario'                              │
│    - Parse & validate body with Zod schema                  │
│    - Check coords in range [-90..90] / [-180..180]         │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 2. TripService.createTrip()                                 │
│    - Validate usuarioId exists & is_active                  │
│    - Call calculateDistance(lat1, lng1, lat2, lng2)         │
│      (Haversine formula) → distancia_km                     │
│    - Get tariffs from ConfigService.getConfig()             │
│    - Call calculatePrice(distancia, toneladas, tarifas)     │
│      → precio, tarifa_unitaria, tipo_tarifa                 │
│    - Validate precio > 0                                    │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 3. TripRepository.create()                                  │
│    INSERT INTO viajes (                                     │
│      id: uuid(),                                            │
│      usuario_id: ?,                                         │
│      origen_lat, origen_lng,                                │
│      destino_lat, destino_lng,                              │
│      distancia_km,                                          │
│      toneladas,                                             │
│      precio, tarifa_unitaria, tipo_tarifa,                  │
│      carga,                                                 │
│      estado: 'solicitado',                                  │
│      version: 1,                                            │
│      solicitado_at: NOW(),                                  │
│      created_at: NOW(),                                     │
│      updated_at: NOW()                                      │
│    );                                                       │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 4. AuditService.log()                                       │
│    INSERT INTO audit_log (                                  │
│      id, user_id: userId, action: 'create',                │
│      entity_type: 'viaje', entity_id: tripId,              │
│      details: {email, distancia, precio, ...},              │
│      ip_address, user_agent, timestamp                      │
│    );                                                       │
└──────────────────────┬──────────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────────┐
│ 5. RESPONSE                                                 │
│ 201 Created                                                 │
│ {                                                           │
│   success: true,                                            │
│   data: {                                                   │
│     id: "550e8400-e29b...",                                │
│     usuario_id: "...",                                      │
│     origen_lat, origen_lng,                                 │
│     destino_lat, destino_lng,                               │
│     distancia_km: 11.25,                                    │
│     precio: 1125.00,                                        │
│     tarifa_unitaria: 100,                                   │
│     tipo_tarifa: "por_km",                                  │
│     estado: "solicitado",                                   │
│     solicitado_at: "2026-08-09T12:34:56.789Z",             │
│     created_at: "2026-08-09T12:34:56.789Z"                 │
│   },                                                        │
│   message: "Trip created successfully"                      │
│ }                                                           │
└─────────────────────────────────────────────────────────────┘
```

---

## TRIP ACCEPTANCE FLOW (TRANSACTIONAL)

```
┌──────────────────────────────────────────────────────────┐
│ CLIENT: POST /api/v1/viajes/:id/aceptar                  │
│ Chofer attempts to accept trip                            │
└────────────────────────┬─────────────────────────────────┘
                         │
┌────────────────────────▼─────────────────────────────────┐
│ 1. ROUTE HANDLER                                         │
│    - Extract tripId from params                          │
│    - Extract choferId from req.user                      │
│    - Validate role = 'chofer'                            │
└────────────────────────┬─────────────────────────────────┘
                         │
┌────────────────────────▼─────────────────────────────────┐
│ 2. TripService.acceptTrip(tripId, choferId)             │
│    - Get viaje = TripRepository.findById(tripId)         │
│    - Validate viaje.estado = 'solicitado'                │
│    - Get truck = TruckRepository.findByUserId(choferId)  │
│    - Validate truck exists                               │
│    - Validate truck.estado = 'disponible'                │
│    - Validate truck.is_deleted = false                   │
└────────────────────────┬─────────────────────────────────┘
                         │
┌────────────────────────▼─────────────────────────────────┐
│ 3. TripRepository.acceptTrip(tripId, choferId, camionId, │
│                              currentVersion)             │
│                                                          │
│    BEGIN TRANSACTION;                                    │
│                                                          │
│    UPDATE viajes SET                                     │
│      estado='aceptado',                                  │
│      chofer_id=?, camion_id=?,                           │
│      version=version+1,                                  │
│      aceptado_at=NOW()                                   │
│    WHERE id=? AND version=? AND estado='solicitado'      │
│    RETURNING *;                                          │
│                                                          │
│    ┌─ IF no rows affected:                              │
│    │  ROLLBACK;                                          │
│    │  THROW: 409 Conflict (version mismatch)            │
│    │                                                    │
│    └─ IF rows affected:                                │
│       UPDATE camiones SET                               │
│         estado='ocupado'                                │
│       WHERE id=?;                                       │
│                                                        │
│       COMMIT;                                          │
│       RETURN viaje;                                    │
│                                                        │
│    EXCEPTION handler:                                 │
│      ROLLBACK;                                         │
│      THROW error;                                      │
│                                                        │
│    END TRANSACTION;                                    │
└────────────────────────┬─────────────────────────────────┘
                         │
┌────────────────────────▼─────────────────────────────────┐
│ 4. AuditService.log()                                   │
│    INSERT INTO audit_log (                              │
│      action: 'trip_accepted',                           │
│      details: {chofer_id, camion_id, ...}               │
│    );                                                   │
└────────────────────────┬─────────────────────────────────┘
                         │
┌────────────────────────▼─────────────────────────────────┐
│ POSSIBLE OUTCOMES:                                       │
│                                                          │
│ SUCCESS (200):                                           │
│ {                                                        │
│   success: true,                                         │
│   data: { ...viaje, estado='aceptado', chofer_id, ... }  │
│ }                                                        │
│                                                          │
│ CONFLICT (409):                                          │
│ {                                                        │
│   success: false,                                        │
│   error: {                                               │
│     code: 'CONFLICT',                                    │
│     message: 'Trip was accepted by another chofer'       │
│   }                                                      │
│ }                                                        │
│                                                          │
│ INVALID STATE (422):                                     │
│ {                                                        │
│   success: false,                                        │
│   error: {                                               │
│     code: 'INVALID_STATE',                               │
│     message: 'Trip not in solicitado state'              │
│   }                                                      │
│ }                                                        │
│                                                          │
│ TRUCK NOT AVAILABLE (409):                               │
│ {                                                        │
│   success: false,                                        │
│   error: {                                               │
│     code: 'TRUCK_UNAVAILABLE',                           │
│     message: 'Truck is not in disponible state'          │
│   }                                                      │
│ }                                                        │
└──────────────────────────────────────────────────────────┘
```

---

## CONCURRENT REQUEST SCENARIO

```
Timeline:

T0:00  Chofer A: GET /viajes/disponibles
       → Sees Trip X (estado='solicitado', version=1)
       
       Chofer B: GET /viajes/disponibles
       → Sees Trip X (estado='solicitado', version=1)

T0:05  Chofer A: POST /viajes/X/aceptar
       → acceptTrip(X, choferA, version=1)
       → ✅ SUCCESS: UPDATE viaje SET version=2
       → Truck A marked as 'ocupado'
       → Response 200

T0:10  Chofer B: POST /viajes/X/aceptar
       → acceptTrip(X, choferB, version=1)
       → ❌ FAIL: UPDATE viaje WHERE version=1
       → No rows affected (current version is 2)
       → Response 409 Conflict
       → Chofer B's app shows: "Trip no longer available"

RESULT: Trip X accepted by Chofer A only, no race condition
        Chofer B must refresh list and choose another trip
```

---

## AUDIT LOG STRUCTURE

```
audit_log table:
┌───────────────────────────────────────────────────────────┐
│ id (uuid)                                                 │
│ user_id (uuid, FK→users)                                  │
│ action ('create' | 'update' | 'delete' | ...)            │
│ entity_type ('viaje' | 'camion' | 'user' | 'config' ...) │
│ entity_id (uuid)                                          │
│ details (JSONB) {                                         │
│   ip_address: "192.168.1.1",                              │
│   user_agent: "Mozilla/5.0...",                           │
│   changes: { field: [old_value, new_value] },             │
│   metadata: {...}                                         │
│ }                                                         │
│ created_at (timestamp)                                    │
│ updated_at (timestamp)                                    │
└───────────────────────────────────────────────────────────┘

Example log entry for trip acceptance:
{
  id: "uuid...",
  user_id: "chofer-uuid",
  action: "trip_accepted",
  entity_type: "viaje",
  entity_id: "trip-uuid",
  details: {
    ip_address: "192.168.1.100",
    user_agent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
    chofer_id: "chofer-uuid",
    camion_id: "truck-uuid",
    previous_estado: "solicitado",
    new_estado: "aceptado",
    version_before: 1,
    version_after: 2,
    timestamp_ms: 1234567890
  },
  created_at: "2026-08-09T12:34:56.789Z"
}
```

---

## API RESPONSE FORMAT (Standard)

### Success Response (2xx)
```json
{
  "success": true,
  "data": { /* entity or array */ },
  "pagination": { "total": 42, "limit": 20, "offset": 0 },
  "message": "Operation successful",
  "timestamp": "2026-08-09T12:34:56.789Z"
}
```

### Error Response (4xx, 5xx)
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message",
    "details": { /* optional, for validation errors */ }
  },
  "timestamp": "2026-08-09T12:34:56.789Z"
}
```

### Validation Error Response (400)
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid input data",
    "details": {
      "origen.lat": ["Must be between -90 and 90"],
      "toneladas": ["Must be greater than 0"]
    }
  },
  "timestamp": "2026-08-09T12:34:56.789Z"
}
```

---

## SECURITY LAYERS

```
┌─────────────────────────────────────────────────┐
│ Layer 1: TRANSPORT SECURITY                     │
│ • HTTPS/TLS (production)                        │
│ • Helmet security headers                       │
│ • CORS whitelist (only localhost:5173)          │
└─────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────┐
│ Layer 2: AUTHENTICATION                         │
│ • Session verification (HttpOnly cookie)        │
│ • User attached to req.user                     │
│ • 401 Unauthorized if not authenticated         │
└─────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────┐
│ Layer 3: AUTHORIZATION (RBAC)                   │
│ • requireRole('admin'|'chofer'|'usuario')      │
│ • Owner checks (user_id == viaje.usuario_id)   │
│ • 403 Forbidden if insufficient permissions    │
└─────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────┐
│ Layer 4: INPUT VALIDATION                       │
│ • Zod schemas on all inputs                     │
│ • Type checking (coords range, etc)             │
│ • 400 Bad Request if invalid                    │
└─────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────┐
│ Layer 5: BUSINESS LOGIC VALIDATION              │
│ • Trip state machine (solicitado → aceptado)   │
│ • Truck availability (disponible only)          │
│ • Version checks (optimistic locking)           │
│ • 409 Conflict, 422 Invalid State               │
└─────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────┐
│ Layer 6: DATA PERSISTENCE & AUDIT               │
│ • PostgreSQL transactions (ACID)                │
│ • Audit logging (immutable)                     │
│ • Soft deletes (data preservation)              │
└─────────────────────────────────────────────────┘
                      ↓
┌─────────────────────────────────────────────────┐
│ Layer 7: RATE LIMITING                          │
│ • Per-IP limiting (HTTP 429 Too Many Requests)  │
│ • Per-endpoint limits (auth stricter)           │
│ • Redis-backed counters                         │
└─────────────────────────────────────────────────┘
```

---

## ERROR HANDLING FLOW

```
┌─────────────────────────────────┐
│ Request arrives at route handler│
└────────────┬────────────────────┘
             │
    ┌────────▼────────┐
    │ Try:            │
    │ • Validate      │
    │ • Call service  │
    │ • Return 200    │
    └────────┬────────┘
             │
             ├─→ Validation fails (Zod)
             │   └─→ throw AppError(400, message)
             │
             ├─→ Service throws AppError
             │   └─→ (various status codes)
             │
             ├─→ Unexpected error
             │   └─→ throw new Error(...)
             │
             └─→ Success
                 └─→ res.json(200, data)
    
    ┌────────▼────────────────────┐
    │ Catch: Error Handler Middleware
    │ • Check if AppError or generic Error
    │ • Extract status, code, message
    │ • Log error (audit trail)
    │ • Return JSON response
    └────────────────────────────┘
    
    Response format:
    {
      success: false,
      error: {
        code: "VALIDATION_ERROR|NOT_FOUND|CONFLICT|...",
        message: "Human readable message"
      },
      timestamp: "2026-08-09T..."
    }
```

---

## DEPLOYMENT ARCHITECTURE (Future - FASE 8)

```
┌─────────────────────────────────────────────┐
│           PRODUCTION ENVIRONMENT             │
├─────────────────────────────────────────────┤
│                                             │
│  ┌───────────────────────────────────────┐  │
│  │  Load Balancer (ALB / NGINX)          │  │
│  │  • SSL/TLS termination                │  │
│  │  • Route to container replicas        │  │
│  └───────────────────────────────────────┘  │
│              ↓                              │
│  ┌───────────────────────────────────────┐  │
│  │  Kubernetes Pod Replicas (N=3)        │  │
│  │  ┌──────────────────────────────────┐ │  │
│  │  │ Node.js/Express Container        │ │  │
│  │  │ • API server                     │ │  │
│  │  │ • Sessions (from Redis)          │ │  │
│  │  │ • Audit logging                  │ │  │
│  │  └──────────────────────────────────┘ │  │
│  └───────────────────────────────────────┘  │
│              ↓                              │
│  ┌───────────────────────────────────────┐  │
│  │  PostgreSQL (Managed / RDS)           │  │
│  │  • Master + Read replicas             │  │
│  │  • Automated backups (daily)          │  │
│  │  • viajes, camiones, users, etc.      │  │
│  └───────────────────────────────────────┘  │
│              ↓                              │
│  ┌───────────────────────────────────────┐  │
│  │  Redis Cache (Managed / ElastiCache) │  │
│  │  • Sessions                           │  │
│  │  • Config cache                       │  │
│  │  • Rate limiter counters              │  │
│  └───────────────────────────────────────┘  │
│                                             │
└─────────────────────────────────────────────┘

High Availability Features:
✅ Pod replicas (3)
✅ Database replication + backups
✅ Redis HA (cluster mode)
✅ Auto-scaling (based on CPU/memory)
✅ Health checks (liveness/readiness probes)
✅ Logging aggregation (ELK stack)
✅ Monitoring & alerting (Prometheus/Grafana)
```

---

## NEXT PHASES INTEGRATION

```
FASE 3 (Current)
├─ Core APIs ✅
├─ Database schema ✅
└─ RBAC + Audit ✅
    │
    ▼
FASE 4: Payment Integration
├─ Stripe/MercadoPago SDK
├─ Webhook handling
├─ Invoice generation
└─ Payment status tracking
    │
    ▼
FASE 5: Real-time Notifications
├─ WebSocket connections
├─ Trip status updates
├─ Email/SMS alerts
└─ Push notifications
    │
    ▼
FASE 6: Maps & Navigation
├─ Google Maps API
├─ Route optimization
├─ ETA calculation
└─ Live tracking
    │
    ▼
FASE 7: Frontend Integration
├─ React components
├─ API client (fetch)
├─ State management
└─ User interface
    │
    ▼
FASE 8: DevOps & Deployment
├─ Docker containerization
├─ Kubernetes orchestration
├─ CI/CD pipeline
└─ Production monitoring
```

---

## File Structure Reference

```
server/
├── src/
│   ├── index.ts                          (Entry point)
│   │
│   ├── config/
│   │   ├── env.ts                        (Environment validation)
│   │   ├── database.ts                   (PostgreSQL pool)
│   │   └── redis.ts                      (Redis client)
│   │
│   ├── middleware/
│   │   ├── auth.ts                       (authMiddleware, requireAuth, requireRole)
│   │   ├── cors.ts                       (CORS configuration)
│   │   ├── errorHandler.ts               (AppError, global error handler)
│   │   └── rateLimit.ts                  (Rate limiting)
│   │
│   ├── types/
│   │   ├── index.ts                      (TypeScript interfaces)
│   │   └── schemas.ts                    (Zod validation schemas)
│   │
│   ├── services/
│   │   ├── authService.ts                (FASE 2 - inherited)
│   │   ├── sessionService.ts             (FASE 2 - inherited)
│   │   ├── passwordService.ts            (FASE 2 - inherited)
│   │   ├── tokenService.ts               (FASE 2 - inherited)
│   │   ├── emailService.ts               (FASE 2 - inherited)
│   │   ├── auditService.ts               (FASE 2 - inherited)
│   │   ├── tripService.ts                (FASE 3 - NEW)
│   │   ├── truckService.ts               (FASE 3 - NEW)
│   │   ├── adminService.ts               (FASE 3 - NEW)
│   │   └── configService.ts              (FASE 3 - NEW)
│   │
│   ├── repositories/
│   │   ├── userRepository.ts             (FASE 2 - inherited)
│   │   ├── tokenRepository.ts            (FASE 2 - inherited)
│   │   ├── tripRepository.ts             (FASE 3 - NEW)
│   │   ├── truckRepository.ts            (FASE 3 - NEW)
│   │   └── configRepository.ts           (FASE 3 - NEW)
│   │
│   ├── routes/
│   │   ├── auth.ts                       (FASE 2 - inherited)
│   │   ├── trips.ts                      (FASE 3 - NEW)
│   │   ├── trucks.ts                     (FASE 3 - NEW)
│   │   └── admin.ts                      (FASE 3 - NEW)
│   │
│   ├── migrations/
│   │   ├── 001_initial_schema.sql        (FASE 1 - inherited)
│   │   ├── 002_fase3_indexes.sql         (FASE 3 - NEW, optional)
│   │   └── runner.ts                     (FASE 1 - inherited)
│   │
│   └── __tests__/
│       ├── unit/
│       │   ├── services/
│       │   │   ├── tripService.test.ts
│       │   │   ├── truckService.test.ts
│       │   │   └── adminService.test.ts
│       │   └── utils/
│       │       └── haversine.test.ts
│       │
│       └── integration/
│           ├── routes/
│           │   ├── trips.test.ts
│           │   ├── trucks.test.ts
│           │   └── admin.test.ts
│           └── setup.ts
│
├── package.json
├── tsconfig.json
├── .env.example
├── .env.local
└── README.md
```
