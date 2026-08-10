# FASE 3 — QUICK REFERENCE

Guía rápida de implementación para FASE 3 (Core API Backend).

---

## ENDPOINTS SUMMARY (17 total)

### Viajes (8)
```
POST   /api/v1/viajes                    → createTrip (usuario)
GET    /api/v1/viajes/mis-viajes          → getMyTrips (any)
GET    /api/v1/viajes/disponibles         → getAvailableTrips (chofer)
POST   /api/v1/viajes/:id/aceptar         → acceptTrip (chofer, TRANSACTIONAL)
POST   /api/v1/viajes/:id/cancelar        → cancelTrip (owner)
POST   /api/v1/viajes/:id/en-camino       → markEnRoute (chofer)
POST   /api/v1/viajes/:id/completar       → markCompleted (chofer)
```

### Camiones (5)
```
GET    /api/v1/camiones/disponibles       → getAvailableTrucks (any auth)
GET    /api/v1/camiones/mi-camion         → getMyTruck (chofer)
POST   /api/v1/camiones/:id/ubicacion     → updateLocation (chofer)
PUT    /api/v1/camiones/:id               → updateTruck (admin)
DELETE /api/v1/camiones/:id               → deleteTruck (admin, soft delete)
```

### Admin (4+)
```
GET    /api/v1/admin/usuarios             → listUsers (admin)
GET    /api/v1/admin/usuarios/:id         → getUserDetails (admin)
PUT    /api/v1/admin/configuracion        → updateConfiguration (admin)
GET    /api/v1/admin/audit-log            → getAuditLog (admin)
```

---

## FILES TO CREATE

```
src/
├── services/
│   ├── tripService.ts          (150-200 lines)
│   ├── truckService.ts         (100-150 lines)
│   └── adminService.ts         (100-150 lines)
├── repositories/
│   ├── tripRepository.ts       (80-120 lines)
│   ├── truckRepository.ts      (80-120 lines)
│   └── configRepository.ts     (30-50 lines)
├── routes/
│   ├── trips.ts                (120-180 lines)
│   ├── trucks.ts               (100-150 lines)
│   └── admin.ts                (100-150 lines)
└── types/
    └── schemas.ts              (ADD 15-20 schemas)
```

---

## KEY ALGORITHMS

### Haversine Distance Formula
```typescript
const calculateDistance = (lat1, lng1, lat2, lng2) => {
  const R = 6371; // Earth radius in km
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLng = (lng2 - lng1) * Math.PI / 180;
  const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
            Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
            Math.sin(dLng/2) * Math.sin(dLng/2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
  return R * c;
};
```

### Pricing Logic
```typescript
const calculatePrice = (distancia, toneladas, tarifaPorKm, tarifaPorTonelada) => {
  // OPCIÓN 1: Suma ambas tarifas
  if (toneladas) {
    return {
      precio: distancia * tarifaPorKm + toneladas * tarifaPorTonelada,
      tarifaUnitaria: (distancia * tarifaPorKm + toneladas * tarifaPorTonelada) / (distancia + toneladas),
      tipoTarifa: 'mixta'
    };
  }
  return {
    precio: distancia * tarifaPorKm,
    tarifaUnitaria: tarifaPorKm,
    tipoTarifa: 'por_km'
  };
};
```

### Optimistic Locking (acceptTrip)
```sql
BEGIN TRANSACTION;

-- Update solo si version coincide
UPDATE viajes 
SET estado='aceptado', chofer_id=$1, camion_id=$2, version=version+1, aceptado_at=NOW()
WHERE id=$3 AND version=$4 AND estado='solicitado';

-- Si no hay rows afectadas: version mismatch, throw 409 Conflict
IF ROW_COUNT = 0 THEN ROLLBACK; RETURN NULL; END IF;

-- Update truck
UPDATE camiones SET estado='ocupado' WHERE id=$2;

COMMIT;
```

---

## VALIDATION SCHEMAS

### Core Schemas
```typescript
// Location validation
{ lat: number [-90..90], lng: number [-180..180] }

// Pagination
{ limit: number [1..100], offset: number [0..] }

// Filters
{ estado?: 'solicitado' | 'aceptado' | 'en_camino' | 'completado' | 'cancelado' }

// CUIT format
{ cuit: /^\d{2}-\d{8}-\d{1}$ }
```

---

## MIDDLEWARE CHECKLIST

- [x] `requireAuth` — Verify session + attach req.user
- [x] `requireRole('usuario' | 'chofer' | 'admin')` — Check role
- [ ] Owner check — In route handlers (not middleware)
  ```typescript
  if (viaje.usuario_id !== req.user!.id && viaje.chofer_id !== req.user!.id) {
    throw new AppError(403, 'Not owner', 'FORBIDDEN');
  }
  ```
- [ ] Input validation — Zod schemas in route handlers
- [x] Error handler — AppError in middleware/errorHandler.ts
- [ ] Audit logging — Call AuditService.log() after state changes

---

## DATABASE CHANGES NEEDED

### New Indexes (recommended)
```sql
-- For trip queries
CREATE INDEX idx_viajes_usuario_id ON viajes(usuario_id, estado);
CREATE INDEX idx_viajes_chofer_id ON viajes(chofer_id, estado);
CREATE INDEX idx_viajes_estado_solicitado ON viajes(estado) WHERE estado='solicitado';
CREATE INDEX idx_viajes_solicitado_at ON viajes(solicitado_at DESC);

-- For truck queries
CREATE INDEX idx_camiones_user_id ON camiones(user_id) WHERE is_deleted=false;
CREATE INDEX idx_camiones_estado ON camiones(estado) WHERE is_deleted=false;

-- For audit log queries
CREATE INDEX idx_audit_log_user_id_created ON audit_log(user_id, created_at DESC);
CREATE INDEX idx_audit_log_action ON audit_log(action, created_at DESC);
```

**Note**: These can be added as migration 002_fase3_indexes.sql

---

## CRITICAL OPERATIONS

### 1. Create Trip
```typescript
VALIDATE coords + toneladas
CALCULATE distance (Haversine)
GET tariffs (from configuracion)
CALCULATE price
INSERT viaje (estado='solicitado')
RETURN viaje
```

### 2. Accept Trip (TRANSACTIONAL)
```typescript
GET viaje + check estado='solicitado'
GET chofer + check exists
GET truck + check estado='disponible'
BEGIN TRANSACTION
  UPDATE viajes (estado='aceptado', version++)
  UPDATE camiones (estado='ocupado')
COMMIT or ROLLBACK
LOG audit_log
RETURN viaje
```

### 3. Cancel Trip (TRANSACTIONAL)
```typescript
GET viaje
CHECK owner (usuario_id or chofer_id)
CHECK estado ('solicitado' or 'aceptado')
BEGIN TRANSACTION
  UPDATE viajes (estado='cancelado')
  IF chofer_id SET:
    UPDATE camiones (estado='disponible')
COMMIT
LOG audit_log
RETURN viaje
```

### 4. Mark Completed
```typescript
GET viaje + check estado='en_camino', chofer_id matches
UPDATE viajes (estado='completado')
UPDATE camiones (estado='disponible')
LOG audit_log
RETURN viaje
```

---

## COMMON ERRORS

| Error | Status | Code | Cause | Fix |
|-------|--------|------|-------|-----|
| Invalid coords | 400 | VALIDATION_ERROR | lat/lng out of range | Validate [-90..90] / [-180..180] |
| User not found | 404 | NOT_FOUND | userId doesn't exist | Check repo |
| Not owner | 403 | FORBIDDEN | userId ≠ viaje owner | Add owner check |
| Truck occupied | 409 | CONFLICT | estado ≠ 'disponible' | Wait or cancel |
| Optimistic lock | 409 | CONFLICT | version mismatch | Retry with new version |
| Trip not solicitado | 422 | INVALID_STATE | estado ≠ 'solicitado' | Check trip state |
| No truck assigned | 403 | FORBIDDEN | chofer.truck = null | Assign truck first |
| Permission denied | 403 | FORBIDDEN | requireRole failed | Check user role |

---

## TESTING CHECKLIST

### Unit Tests (~55 tests)
- [ ] TripService.calculateDistance() — Haversine accuracy
- [ ] TripService.calculatePrice() — Pricing logic
- [ ] TripService.createTrip() — Validation
- [ ] TripService.acceptTrip() — Optimistic lock
- [ ] TripService.cancelTrip() — State transitions
- [ ] TruckService.*
- [ ] AdminService.*

### Integration Tests (~40 tests)
- [ ] POST /viajes — Happy path + validation errors
- [ ] POST /viajes/:id/aceptar — Concurrent attempts
- [ ] POST /viajes/:id/cancelar — Owner checks
- [ ] GET /camiones/disponibles
- [ ] POST /camiones/:id/ubicacion
- [ ] GET /admin/usuarios
- [ ] PUT /admin/configuracion

### Permission Tests (~20 tests)
- [ ] usuario can create trip, not accept
- [ ] chofer can accept trip, not create
- [ ] admin can update config, users can't
- [ ] Owner can cancel, non-owner can't

---

## IMPLEMENTATION ORDER

```
1. Create file stubs + register routes
2. Implement TripRepository
3. Implement TruckRepository + ConfigRepository
4. Implement TripService (calculate distance/price, CRUD)
5. Implement TruckService
6. Implement AdminService
7. Implement Trips routes (all 8 endpoints)
8. Implement Trucks + Admin routes
9. Unit tests for services
10. Integration tests for routes
11. Manual testing + validation report
```

**Total commits**: 8-10

---

## USEFUL QUERIES

```sql
-- Check optimistic lock
SELECT id, version, estado FROM viajes WHERE id='...' FOR UPDATE;

-- Check truck state
SELECT id, estado FROM camiones WHERE user_id='...';

-- Check active trips for truck
SELECT COUNT(*) FROM viajes 
WHERE camion_id='...' AND estado IN ('aceptado', 'en_camino');

-- Recent audit log
SELECT * FROM audit_log WHERE user_id='...' ORDER BY created_at DESC LIMIT 10;

-- Revert trip to solicitado (for testing)
UPDATE viajes SET estado='solicitado', chofer_id=NULL, camion_id=NULL, version=version+1 
WHERE id='...' RETURNING *;
```

---

## TIPS & TRICKS

1. **Testing Haversine**: Buenos Aires (lat=-34.6037, lng=-58.3816) to NYC (40.7128, -74.0060) ≈ 11,062 km

2. **Mock Configuration**: 
   ```typescript
   const mockConfig = {
     tarifa_por_km: 100,
     tarifa_por_tonelada: 500,
   };
   ```

3. **Rate Limiting**: Consider adding limiter for trip creation:
   ```typescript
   const tripCreationLimiter = rateLimit({ windowMs: 60s, max: 5 });
   router.post('/', tripCreationLimiter, ...);
   ```

4. **Audit Logging**: Always log after state change:
   ```typescript
   await AuditService.log('trip_accepted', 'viaje', {
     userId: req.user!.id,
     entityId: tripId,
     details: { chofer_id, camion_id },
   });
   ```

5. **Error Responses**: Consistent format:
   ```typescript
   {
     success: false,
     error: { code: 'INVALID_STATE', message: 'Trip not in solicitado state' },
     timestamp: '2026-08-09T...'
   }
   ```

---

## DEADLINE & RESOURCES

**Est. Duration**: 8-10 hours (1 developer)  
**Commits**: 8-10  
**Test Coverage**: 85%+  
**Status**: Ready to implement

Start → PR → Review → Merge → FASE 4
