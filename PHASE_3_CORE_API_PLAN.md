# FASE 3 — PLAN DETALLADO: CORE API BACKEND

**Fecha de Creación**: 2026-08-09  
**Estado**: 📋 PLANIFICACIÓN  
**Dependencia**: FASE 2 (Auth Backend) ✅ Completada

---

## ÍNDICE

1. [Resumen Ejecutivo](#resumen-ejecutivo)
2. [Servicios a Crear](#servicios-a-crear)
3. [Repositorios a Crear](#repositorios-a-crear)
4. [Schemas de Validación](#schemas-de-validación)
5. [Rutas a Implementar](#rutas-a-implementar)
6. [Middleware Necesario](#middleware-necesario)
7. [Orden de Implementación](#orden-de-implementación)
8. [Consideraciones de Seguridad](#consideraciones-de-seguridad)
9. [Estrategia de Testing](#estrategia-de-testing)
10. [Casos de Borde y Mitigaciones](#casos-de-borde-y-mitigaciones)
11. [Rollback Strategy](#rollback-strategy)
12. [Criterios de Aceptación](#criterios-de-aceptación)

---

## RESUMEN EJECUTIVO

### Objetivos FASE 3

- Implementar **17 endpoints** de gestión de viajes, camiones y administración
- Garantizar **integridad transaccional** en operaciones críticas (aceptar viaje)
- Implementar **cálculos precisos** de distancia (Haversine) y precios
- Aplicar **RBAC** (Role-Based Access Control) en todos los endpoints
- Agregar **auditoría completa** de operaciones sensibles
- Implementar **validación exhaustiva** con Zod

### Endpoints a Implementar

| Categoría | Count | Endpoints |
|-----------|-------|-----------|
| **Viajes** | 8 | Create, GetMine, GetAvailable, Accept, Cancel, EnRoute, Complete |
| **Camiones** | 5 | GetAvailable, GetMine, UpdateLocation, Update (admin), Delete (admin) |
| **Admin** | 4+ | ListUsers, GetUserDetails, UpdateConfig, AuditLog |
| **Utilidades** | — | Health checks, etc. |

**Total**: 17+ endpoints en 3 rutas principales

---

## SERVICIOS A CREAR

### 1. **TripService** (src/services/tripService.ts)

**Responsabilidades**:
- Gestión completa del ciclo de vida de viajes
- Cálculos de precio y distancia
- Lógica de aceptación/cancelación/estado

**Métodos principales**:

```typescript
export class TripService {
  // CREAR VIAJE
  static async createTrip(
    usuarioId: string,
    origen: { lat: number; lng: number },
    destino: { lat: number; lng: number },
    toneladas?: number,
    carga?: string
  ): Promise<Viaje>
  
  // Validaciones:
  // - usuarioId existe y está activo
  // - origen/destino coords válidas (lat -90..90, lng -180..180)
  // - si toneladas, debe ser > 0
  // - calcula distancia (Haversine)
  // - aplica tarifa según config
  // - crea viaje con estado='solicitado'
  
  // OBTENER MIS VIAJES (usuario o chofer)
  static async getMyTrips(
    userId: string,
    estado?: Viaje['estado'],
    limit?: number,
    offset?: number
  ): Promise<{ viajes: Viaje[]; total: number }>
  
  // Validaciones:
  // - userId autenticado
  // - filtra por estado si se proporciona
  // - paginación
  // - order by created_at DESC
  
  // OBTENER VIAJES DISPONIBLES (chofer only)
  static async getAvailableTrips(
    choferId: string,
    limit?: number,
    offset?: number
  ): Promise<{ viajes: Viaje[]; total: number }>
  
  // Validaciones:
  // - choferId existe, tiene camion, está activo
  // - estado='solicitado' (no aceptados)
  // - OPTIONAL: filtrar por distancia/capacidad del camion
  // - order by solicitado_at ASC (más antiguos primero)
  
  // ACEPTAR VIAJE (chofer only, TRANSACTIONAL)
  static async acceptTrip(
    tripId: string,
    choferId: string
  ): Promise<Viaje>
  
  // Validaciones (CRÍTICAS - ver transacciones):
  // - viaje existe y estado='solicitado'
  // - chofer existe, tiene camion disponible
  // - optimistic locking: version debe coincidir
  // - TRANSACTION:
  //   1. UPDATE viajes SET estado='aceptado', chofer_id=?, camion_id=?, version=version+1, aceptado_at=NOW()
  //   2. UPDATE camiones SET estado='ocupado' WHERE id=camion_id
  //   3. Si version mismatch → rollback, return 409 Conflict
  // - log: audit_log INSERT (trip_accepted)
  
  // CANCELAR VIAJE (usuario o chofer, owner only)
  static async cancelTrip(
    tripId: string,
    canceledByUserId: string,
    reason?: string
  ): Promise<Viaje>
  
  // Validaciones:
  // - viaje existe
  // - canceledByUserId es usuario_id o chofer_id del viaje
  // - estado debe ser 'solicitado' o 'aceptado' (no en_camino/completado)
  // - TRANSACTION:
  //   1. UPDATE viajes SET estado='cancelado', cancelado_at=NOW(), cancelacion_razon=?
  //   2. Si chofer_id exists: UPDATE camiones SET estado='disponible'
  // - log: audit_log INSERT (trip_cancelled, reason)
  
  // MARCAR EN CAMINO (chofer only)
  static async markEnRoute(tripId: string, choferId: string): Promise<Viaje>
  
  // Validaciones:
  // - viaje existe, estado='aceptado', chofer_id=choferId
  // - UPDATE viajes SET estado='en_camino', en_camino_at=NOW()
  // - log: audit_log INSERT
  
  // MARCAR COMPLETADO (chofer only)
  static async markCompleted(
    tripId: string,
    choferId: string,
    completionNote?: string
  ): Promise<Viaje>
  
  // Validaciones:
  // - viaje existe, estado='en_camino', chofer_id=choferId
  // - UPDATE viajes SET estado='completado', completado_at=NOW()
  // - UPDATE camiones SET estado='disponible' (libera camion)
  // - log: audit_log INSERT
  
  // OBTENER VIAJE POR ID
  static async getTripById(tripId: string): Promise<Viaje>
  
  // OBTENER TARIFA
  private static async getTariff(): Promise<{ por_km: number; por_tonelada: number }>
  
  // Obtiene de configuracion tabla (singleton)
  
  // CALCULAR DISTANCIA (Haversine)
  private static calculateDistance(
    lat1: number, lng1: number,
    lat2: number, lng2: number
  ): number
  
  // Retorna distancia en KM (formula Haversine)
  // Formula: a = sin²(Δφ/2) + cos φ1 ⋅ cos φ2 ⋅ sin²(Δλ/2)
  //          c = 2 ⋅ atan2( √a, √(1−a) )
  //          d = R ⋅ c (R = 6,371 km)
  
  // CALCULAR PRECIO
  static calculatePrice(
    distancia: number,
    toneladas?: number,
    tarifaPorKm?: number,
    tarifaPorTonelada?: number
  ): { precio: number; tarifaUnitaria: number; tipoTarifa: 'por_km' | 'por_tonelada' }
  
  // Lógica:
  // - si toneladas: precio = distancia * tarifaPorKm + toneladas * tarifaPorTonelada
  //                 tarifaUnitaria = promedio ponderado
  //   OPCIÓN 2: precio = max(distancia * tarifaPorKm, toneladas * tarifaPorTonelada)
  // - sino: precio = distancia * tarifaPorKm
  //         tarifaUnitaria = tarifaPorKm
  // Recomendación: DISCUTIR LÓGICA DE PRICING CON NEGOCIO
}
```

**Dependencias**:
- `TripRepository`, `ConfigRepository`, `AuditService`, `CamionRepository`

**Errores manejados**:
- 400: Validación de coords, datos inválidos
- 409: Optimistic lock mismatch en acceptTrip
- 404: Viaje/chofer/camion no encontrado
- 403: Permisos insuficientes (not owner, wrong state)
- 422: Estado de viaje inválido para operación

---

### 2. **TruckService** (src/services/truckService.ts)

**Responsabilidades**:
- Gestión de camiones
- Validación de capacidad y disponibilidad
- Actualización de ubicación GPS

**Métodos principales**:

```typescript
export class TruckService {
  // OBTENER CAMIONES DISPONIBLES (all authenticated)
  static async getAvailableTrucks(
    limit?: number,
    offset?: number
  ): Promise<{ trucks: Camion[]; total: number }>
  
  // Validaciones:
  // - estado='disponible', is_deleted=false
  // - order by created_at DESC
  // - paginación
  
  // OBTENER MI CAMION (chofer only)
  static async getMyTruck(choferId: string): Promise<Camion>
  
  // Validaciones:
  // - choferId existe, es chofer, está activo
  // - find camion WHERE user_id=choferId, is_deleted=false
  // - if not found: throw 404 'No truck assigned'
  
  // ACTUALIZAR UBICACIÓN GPS (chofer only)
  static async updateLocation(
    choferId: string,
    lat: number,
    lng: number
  ): Promise<Camion>
  
  // Validaciones:
  // - choferId autenticado, es chofer
  // - coords válidas
  // - find camion WHERE user_id=choferId
  // - UPDATE camiones SET lat=?, lng=?, updated_at=NOW()
  // - log: audit_log INSERT (location_updated)
  
  // ACTUALIZAR CAMION (admin only)
  static async updateTruck(
    truckId: string,
    data: Partial<{
      patente: string;
      patente_acoplado: string;
      transporte_nombre: string;
      transporte_cuit: string;
      chofer_nombre: string;
      chofer_cuit: string;
      telefono: string;
      capacidad_kg: number;
      estado: Camion['estado'];
    }>
  ): Promise<Camion>
  
  // Validaciones:
  // - truckId existe, is_deleted=false
  // - patente única (si se actualiza)
  // - CUIT formato válido (si se proporciona)
  // - UPDATE con los campos proporcionados
  // - log: audit_log INSERT (truck_updated, changes)
  
  // ELIMINAR CAMION SOFT DELETE (admin only)
  static async deleteTruck(truckId: string): Promise<void>
  
  // Validaciones:
  // - truckId existe
  // - check: no hay viajes activos (estado != 'completado', 'cancelado')
  // - si hay viajes activos: throw 409 'Cannot delete truck with active trips'
  // - UPDATE camiones SET is_deleted=true, updated_at=NOW()
  // - log: audit_log INSERT (truck_deleted)
  
  // OBTENER CAMION POR ID
  static async getTruckById(truckId: string): Promise<Camion>
  
  // Validaciones:
  // - is_deleted=false
}
```

**Dependencias**:
- `TruckRepository`, `TripRepository`, `AuditService`

**Errores manejados**:
- 400: Coords/datos inválidos
- 404: Camion no encontrado
- 403: Permisos insuficientes
- 409: Camion con viajes activos (delete attempt)
- 422: Patente ya existe

---

### 3. **AdminService** (src/services/adminService.ts)

**Responsabilidades**:
- Gestión de configuración
- Visualización de usuarios
- Auditoría y reportes

**Métodos principales**:

```typescript
export class AdminService {
  // LISTAR USUARIOS (admin only)
  static async listUsers(
    role?: User['role'],
    isActive?: boolean,
    limit?: number,
    offset?: number
  ): Promise<{ users: User[]; total: number }>
  
  // Validaciones:
  // - admin autenticado
  // - filtro por rol si se proporciona
  // - filtro por is_active si se proporciona
  // - paginación
  // - excluir password_hash en respuesta
  // - order by created_at DESC
  
  // OBTENER DETALLES DE USUARIO (admin only)
  static async getUserDetails(userId: string): Promise<{
    user: User;
    tripCount: number;
    truckCount: number;
    lastLogin: string | null;
    auditLog: AuditLog[];
  }>
  
  // Validaciones:
  // - userId existe
  // - return user + aggregated stats
  // - audit log últimas 20 acciones del usuario
  
  // ACTUALIZAR CONFIGURACIÓN (admin only)
  static async updateConfiguration(
    data: Partial<{
      zona_nombre: string;
      centro_lat: number;
      centro_lng: number;
      tarifa_por_km: number;
      tarifa_por_tonelada: number;
    }>
  ): Promise<Configuracion>
  
  // Validaciones:
  // - admin autenticado
  // - coords válidas (si se proporciona centro)
  // - tarifas > 0 (si se proporciona)
  // - UPDATE configuracion SET ... (singleton table)
  // - log: audit_log INSERT (config_updated, old vs new)
  
  // OBTENER CONFIGURACIÓN ACTUAL (any auth)
  static async getConfiguration(): Promise<Configuracion>
  
  // Validaciones:
  // - fetch singleton row from configuracion
  // - cached en Redis (TTL 5 min)
  
  // OBTENER AUDIT LOG (admin only, paginado)
  static async getAuditLog(
    filters?: {
      userId?: string;
      action?: AuditLog['action'];
      entityType?: string;
      startDate?: string;
      endDate?: string;
    },
    limit?: number,
    offset?: number
  ): Promise<{ logs: AuditLog[]; total: number }>
  
  // Validaciones:
  // - admin autenticado
  // - paginación
  // - filtros opcionales
  // - order by created_at DESC
  
  // EXPORTAR AUDIT LOG (admin only, CSV/JSON)
  static async exportAuditLog(
    format: 'csv' | 'json',
    filters?: any
  ): Promise<string>
  
  // Retorna CSV o JSON serializado
}
```

**Dependencias**:
- `UserRepository`, `ConfigRepository`, `AuditService`, `TripRepository`, `TruckRepository`

**Errores manejados**:
- 400: Datos inválidos
- 403: Permisos insuficientes (admin only)
- 404: Recurso no encontrado

---

### 4. **ConfigService** (src/services/configService.ts)

**Responsabilidades**:
- Caché de configuración
- Validación de tariffs

**Métodos principales**:

```typescript
export class ConfigService {
  // GET CONFIGURATION (cached)
  static async getConfig(): Promise<Configuracion>
  
  // Validaciones:
  // - check Redis first (key: 'config:main', TTL 5 min)
  // - si no existe, fetch de DB
  // - cache en Redis
  
  // INVALIDATE CACHE
  static invalidateCache(): Promise<void>
  
  // Elimina cache de Redis
}
```

**Nota**: Podría simplificarse como métodos en AdminService.

---

## REPOSITORIOS A CREAR

### 1. **TripRepository** (src/repositories/tripRepository.ts)

**Métodos**:

```typescript
export class TripRepository {
  // CREATE
  static async create(data: {
    usuario_id: string;
    origen_lat: number;
    origen_lng: number;
    destino_lat: number;
    destino_lng: number;
    distancia_km: number;
    toneladas?: number;
    tipo_tarifa: 'por_km' | 'por_tonelada';
    precio: number;
    tarifa_unitaria: number;
    carga?: string;
  }): Promise<Viaje>
  
  // INSERT with estado='solicitado', version=1, solicitado_at=NOW()
  
  // FIND BY ID
  static async findById(id: string): Promise<Viaje | null>
  
  // FIND BY USER (usuario or chofer)
  static async findByUserId(userId: string, estado?: Viaje['estado']): Promise<Viaje[]>
  
  // FIND AVAILABLE (estado='solicitado')
  static async findAvailable(): Promise<Viaje[]>
  
  // UPDATE STATE
  static async updateState(
    id: string,
    estado: Viaje['estado'],
    versionCheck?: number
  ): Promise<Viaje>
  
  // UPDATE with optimistic locking if versionCheck provided
  // If version mismatch: return null (caller checks and returns 409)
  
  // ACCEPT TRIP (chofer + camion assignment with transaction)
  static async acceptTrip(
    id: string,
    choferId: string,
    camionId: string,
    currentVersion: number
  ): Promise<Viaje | null>
  
  // BEGIN TRANSACTION
  //   UPDATE viajes SET estado='aceptado', chofer_id=?, camion_id=?, version=version+1, aceptado_at=NOW()
  //   WHERE id=? AND version=?
  //   UPDATE camiones SET estado='ocupado' WHERE id=?
  // COMMIT or return null if version mismatch
  
  // COUNT BY STATE
  static async countByState(estado: Viaje['estado']): Promise<number>
  
  // PAGINADO
  static async findPaginated(
    filters: {
      userId?: string;
      estado?: Viaje['estado'];
    },
    limit: number,
    offset: number
  ): Promise<{ trips: Viaje[]; total: number }>
}
```

---

### 2. **TruckRepository** (src/repositories/truckRepository.ts)

**Métodos**:

```typescript
export class TruckRepository {
  // CREATE
  static async create(data: Omit<Camion, 'id' | 'created_at' | 'updated_at' | 'is_deleted'>): Promise<Camion>
  
  // FIND BY ID
  static async findById(id: string): Promise<Camion | null>
  
  // FIND BY USER_ID (chofer)
  static async findByUserId(userId: string): Promise<Camion | null>
  
  // FIND AVAILABLE
  static async findAvailable(): Promise<Camion[]>
  
  // FIND ALL (admin)
  static async findAll(): Promise<Camion[]>
  
  // UPDATE
  static async update(id: string, data: Partial<Omit<Camion, 'id' | 'created_at' | 'updated_at' | 'is_deleted'>>): Promise<Camion>
  
  // SOFT DELETE
  static async softDelete(id: string): Promise<void>
  
  // UPDATE LOCATION
  static async updateLocation(id: string, lat: number, lng: number): Promise<Camion>
  
  // UPDATE STATE
  static async updateState(id: string, estado: Camion['estado']): Promise<Camion>
  
  // PAGINADO
  static async findPaginated(
    filters: {
      estado?: Camion['estado'];
    },
    limit: number,
    offset: number
  ): Promise<{ trucks: Camion[]; total: number }>
}
```

---

### 3. **ConfigRepository** (src/repositories/configRepository.ts)

**Métodos**:

```typescript
export class ConfigRepository {
  // GET (singleton)
  static async get(): Promise<Configuracion>
  
  // UPDATE
  static async update(data: Partial<Omit<Configuracion, 'id' | 'created_at' | 'updated_at'>>): Promise<Configuracion>
}
```

---

## SCHEMAS DE VALIDACIÓN

### Agregar a `src/types/schemas.ts`:

```typescript
// ============ TRIP SCHEMAS ============

export const CreateTripSchema = z.object({
  origen: z.object({
    lat: z.number().min(-90).max(90),
    lng: z.number().min(-180).max(180),
  }),
  destino: z.object({
    lat: z.number().min(-90).max(90),
    lng: z.number().min(-180).max(180),
  }),
  toneladas: z.number().min(0.1).optional(),
  carga: z.string().max(255).optional(),
});
export type CreateTripInput = z.infer<typeof CreateTripSchema>;

export const CancelTripSchema = z.object({
  reason: z.string().max(500).optional(),
});
export type CancelTripInput = z.infer<typeof CancelTripSchema>;

// ============ TRUCK SCHEMAS ============

export const UpdateTruckLocationSchema = z.object({
  lat: z.number().min(-90).max(90),
  lng: z.number().min(-180).max(180),
});
export type UpdateTruckLocationInput = z.infer<typeof UpdateTruckLocationSchema>;

export const UpdateTruckSchema = z.object({
  patente: z.string().min(3).max(20).optional(),
  patente_acoplado: z.string().max(20).optional(),
  transporte_nombre: z.string().max(100).optional(),
  transporte_cuit: z.string().regex(/^\d{2}-\d{8}-\d{1}$/).optional(),
  chofer_nombre: z.string().min(3).max(100).optional(),
  chofer_cuit: z.string().regex(/^\d{2}-\d{8}-\d{1}$/).optional(),
  telefono: z.string().max(20).optional(),
  capacidad_kg: z.number().min(100).optional(),
  estado: z.enum(['disponible', 'ocupado', 'inactivo']).optional(),
});
export type UpdateTruckInput = z.infer<typeof UpdateTruckSchema>;

// ============ ADMIN SCHEMAS ============

export const UpdateConfigSchema = z.object({
  zona_nombre: z.string().min(1).max(100).optional(),
  centro_lat: z.number().min(-90).max(90).optional(),
  centro_lng: z.number().min(-180).max(180).optional(),
  tarifa_por_km: z.number().min(0).optional(),
  tarifa_por_tonelada: z.number().min(0).optional(),
});
export type UpdateConfigInput = z.infer<typeof UpdateConfigSchema>;

// ============ QUERY SCHEMAS ============

export const PaginationSchema = z.object({
  limit: z.coerce.number().min(1).max(100).default(20),
  offset: z.coerce.number().min(0).default(0),
});
export type PaginationInput = z.infer<typeof PaginationSchema>;

export const TripsFilterSchema = PaginationSchema.extend({
  estado: z.enum(['solicitado', 'aceptado', 'en_camino', 'completado', 'cancelado']).optional(),
});
export type TripsFilterInput = z.infer<typeof TripsFilterSchema>;

export const AuditFilterSchema = PaginationSchema.extend({
  userId: z.string().uuid().optional(),
  action: z.string().optional(),
  entityType: z.string().optional(),
  startDate: z.string().datetime().optional(),
  endDate: z.string().datetime().optional(),
});
export type AuditFilterInput = z.infer<typeof AuditFilterSchema>;
```

---

## RUTAS A IMPLEMENTAR

### 1. **Viajes Routes** (`src/routes/trips.ts`)

```typescript
import { Router, Request, Response } from 'express';
import { requireAuth, requireRole } from '../middleware/auth';
import { TripService } from '../services/tripService';
import { CreateTripSchema, CancelTripSchema, TripsFilterSchema } from '../types/schemas';
import { AppError } from '../middleware/errorHandler';

const router = Router();

// POST /api/v1/viajes — Create trip (usuario only)
router.post(
  '/',
  requireAuth,
  requireRole('usuario'),
  async (req: Request, res: Response) => {
    const validation = CreateTripSchema.safeParse(req.body);
    if (!validation.success) {
      throw new AppError(400, 'Invalid trip data', 'VALIDATION_ERROR');
    }
    
    const viaje = await TripService.createTrip(
      req.user!.id,
      validation.data.origen,
      validation.data.destino,
      validation.data.toneladas,
      validation.data.carga
    );
    
    res.status(201).json({
      success: true,
      data: viaje,
      message: 'Trip created successfully',
    });
  }
);

// GET /api/v1/viajes/mis-viajes — Get my trips (any role)
router.get(
  '/mis-viajes',
  requireAuth,
  async (req: Request, res: Response) => {
    const validation = TripsFilterSchema.safeParse(req.query);
    if (!validation.success) {
      throw new AppError(400, 'Invalid filters', 'VALIDATION_ERROR');
    }
    
    const { viajes, total } = await TripService.getMyTrips(
      req.user!.id,
      validation.data.estado,
      validation.data.limit,
      validation.data.offset
    );
    
    res.json({
      success: true,
      data: viajes,
      pagination: { total, limit: validation.data.limit, offset: validation.data.offset },
    });
  }
);

// GET /api/v1/viajes/disponibles — Get available trips (chofer only)
router.get(
  '/disponibles',
  requireAuth,
  requireRole('chofer'),
  async (req: Request, res: Response) => {
    const validation = PaginationSchema.safeParse(req.query);
    if (!validation.success) {
      throw new AppError(400, 'Invalid pagination', 'VALIDATION_ERROR');
    }
    
    const { viajes, total } = await TripService.getAvailableTrips(
      req.user!.id,
      validation.data.limit,
      validation.data.offset
    );
    
    res.json({
      success: true,
      data: viajes,
      pagination: { total, limit: validation.data.limit, offset: validation.data.offset },
    });
  }
);

// POST /api/v1/viajes/:id/aceptar — Accept trip (chofer only, TRANSACTIONAL)
router.post(
  '/:id/aceptar',
  requireAuth,
  requireRole('chofer'),
  async (req: Request, res: Response) => {
    const viaje = await TripService.acceptTrip(req.params.id, req.user!.id);
    
    res.json({
      success: true,
      data: viaje,
      message: 'Trip accepted successfully',
    });
  }
);

// POST /api/v1/viajes/:id/cancelar — Cancel trip (usuario or chofer, owner only)
router.post(
  '/:id/cancelar',
  requireAuth,
  async (req: Request, res: Response) => {
    const validation = CancelTripSchema.safeParse(req.body);
    if (!validation.success) {
      throw new AppError(400, 'Invalid cancel data', 'VALIDATION_ERROR');
    }
    
    const viaje = await TripService.cancelTrip(
      req.params.id,
      req.user!.id,
      validation.data.reason
    );
    
    res.json({
      success: true,
      data: viaje,
      message: 'Trip cancelled successfully',
    });
  }
);

// POST /api/v1/viajes/:id/en-camino — Mark en route (chofer only)
router.post(
  '/:id/en-camino',
  requireAuth,
  requireRole('chofer'),
  async (req: Request, res: Response) => {
    const viaje = await TripService.markEnRoute(req.params.id, req.user!.id);
    
    res.json({
      success: true,
      data: viaje,
      message: 'Trip marked as en route',
    });
  }
);

// POST /api/v1/viajes/:id/completar — Mark completed (chofer only)
router.post(
  '/:id/completar',
  requireAuth,
  requireRole('chofer'),
  async (req: Request, res: Response) => {
    const viaje = await TripService.markCompleted(
      req.params.id,
      req.user!.id,
      req.body.nota
    );
    
    res.json({
      success: true,
      data: viaje,
      message: 'Trip marked as completed',
    });
  }
);

export default router;
```

### 2. **Trucks Routes** (`src/routes/trucks.ts`)

```typescript
import { Router, Request, Response } from 'express';
import { requireAuth, requireRole } from '../middleware/auth';
import { TruckService } from '../services/truckService';
import { UpdateTruckLocationSchema, UpdateTruckSchema, PaginationSchema } from '../types/schemas';
import { AppError } from '../middleware/errorHandler';

const router = Router();

// GET /api/v1/camiones/disponibles — Get available trucks
router.get(
  '/disponibles',
  requireAuth,
  async (req: Request, res: Response) => {
    const validation = PaginationSchema.safeParse(req.query);
    if (!validation.success) {
      throw new AppError(400, 'Invalid pagination', 'VALIDATION_ERROR');
    }
    
    const { trucks, total } = await TruckService.getAvailableTrucks(
      validation.data.limit,
      validation.data.offset
    );
    
    res.json({
      success: true,
      data: trucks,
      pagination: { total, limit: validation.data.limit, offset: validation.data.offset },
    });
  }
);

// GET /api/v1/camiones/mi-camion — Get my truck (chofer only)
router.get(
  '/mi-camion',
  requireAuth,
  requireRole('chofer'),
  async (req: Request, res: Response) => {
    const truck = await TruckService.getMyTruck(req.user!.id);
    
    res.json({
      success: true,
      data: truck,
    });
  }
);

// POST /api/v1/camiones/:id/ubicacion — Update GPS location (chofer only)
router.post(
  '/:id/ubicacion',
  requireAuth,
  requireRole('chofer'),
  async (req: Request, res: Response) => {
    const validation = UpdateTruckLocationSchema.safeParse(req.body);
    if (!validation.success) {
      throw new AppError(400, 'Invalid location data', 'VALIDATION_ERROR');
    }
    
    const truck = await TruckService.updateLocation(
      req.user!.id,
      validation.data.lat,
      validation.data.lng
    );
    
    res.json({
      success: true,
      data: truck,
      message: 'Location updated successfully',
    });
  }
);

// PUT /api/v1/camiones/:id — Update truck (admin only)
router.put(
  '/:id',
  requireAuth,
  requireRole('admin'),
  async (req: Request, res: Response) => {
    const validation = UpdateTruckSchema.safeParse(req.body);
    if (!validation.success) {
      throw new AppError(400, 'Invalid truck data', 'VALIDATION_ERROR');
    }
    
    const truck = await TruckService.updateTruck(req.params.id, validation.data);
    
    res.json({
      success: true,
      data: truck,
      message: 'Truck updated successfully',
    });
  }
);

// DELETE /api/v1/camiones/:id — Soft delete (admin only)
router.delete(
  '/:id',
  requireAuth,
  requireRole('admin'),
  async (req: Request, res: Response) => {
    await TruckService.deleteTruck(req.params.id);
    
    res.json({
      success: true,
      message: 'Truck deleted successfully',
    });
  }
);

export default router;
```

### 3. **Admin Routes** (`src/routes/admin.ts`)

```typescript
import { Router, Request, Response } from 'express';
import { requireAuth, requireRole } from '../middleware/auth';
import { AdminService } from '../services/adminService';
import { UpdateConfigSchema, PaginationSchema, AuditFilterSchema } from '../types/schemas';
import { AppError } from '../middleware/errorHandler';

const router = Router();

// GET /api/v1/admin/usuarios — List users (admin only)
router.get(
  '/usuarios',
  requireAuth,
  requireRole('admin'),
  async (req: Request, res: Response) => {
    const validation = PaginationSchema.safeParse(req.query);
    if (!validation.success) {
      throw new AppError(400, 'Invalid pagination', 'VALIDATION_ERROR');
    }
    
    const { users, total } = await AdminService.listUsers(
      req.query.role as any,
      req.query.isActive === 'true',
      validation.data.limit,
      validation.data.offset
    );
    
    res.json({
      success: true,
      data: users,
      pagination: { total, limit: validation.data.limit, offset: validation.data.offset },
    });
  }
);

// GET /api/v1/admin/usuarios/:id — Get user details (admin only)
router.get(
  '/usuarios/:id',
  requireAuth,
  requireRole('admin'),
  async (req: Request, res: Response) => {
    const details = await AdminService.getUserDetails(req.params.id);
    
    res.json({
      success: true,
      data: details,
    });
  }
);

// PUT /api/v1/admin/configuracion — Update configuration (admin only)
router.put(
  '/configuracion',
  requireAuth,
  requireRole('admin'),
  async (req: Request, res: Response) => {
    const validation = UpdateConfigSchema.safeParse(req.body);
    if (!validation.success) {
      throw new AppError(400, 'Invalid config data', 'VALIDATION_ERROR');
    }
    
    const config = await AdminService.updateConfiguration(validation.data);
    
    res.json({
      success: true,
      data: config,
      message: 'Configuration updated successfully',
    });
  }
);

// GET /api/v1/admin/audit-log — View audit log (admin only)
router.get(
  '/audit-log',
  requireAuth,
  requireRole('admin'),
  async (req: Request, res: Response) => {
    const validation = AuditFilterSchema.safeParse(req.query);
    if (!validation.success) {
      throw new AppError(400, 'Invalid filters', 'VALIDATION_ERROR');
    }
    
    const { logs, total } = await AdminService.getAuditLog(
      {
        userId: validation.data.userId,
        action: validation.data.action as any,
        entityType: validation.data.entityType,
        startDate: validation.data.startDate,
        endDate: validation.data.endDate,
      },
      validation.data.limit,
      validation.data.offset
    );
    
    res.json({
      success: true,
      data: logs,
      pagination: { total, limit: validation.data.limit, offset: validation.data.offset },
    });
  }
);

// GET /api/v1/admin/configuracion — Get current config (any auth)
router.get(
  '/configuracion',
  requireAuth,
  async (req: Request, res: Response) => {
    const config = await AdminService.getConfiguration();
    
    res.json({
      success: true,
      data: config,
    });
  }
);

export default router;
```

---

## MIDDLEWARE NECESARIO

### 1. **Extender authMiddleware** (`src/middleware/auth.ts`)

Cambios necesarios:
- `authMiddleware`: Mantener como está (attach `req.user`)
- `requireAuth`: Mantener como está (check `req.user`)
- `requireRole(...roles)`: Implementado ya, asegurar que soporte múltiples roles

```typescript
// Verificar que existe:
export const requireRole = (...roles: string[]) => {
  return (req: Request, res: Response, next: NextFunction) => {
    if (!req.user || !roles.includes(req.user.role)) {
      throw new AppError(403, 'Insufficient permissions', 'FORBIDDEN');
    }
    next();
  };
};
```

### 2. **Enhancer: Audit Logging Middleware** (`src/middleware/auditLogger.ts`) - OPCIONAL

Captura cambios importantes automáticamente:

```typescript
export const auditLogger = (action: string, entityType: string) => {
  return async (req: Request, res: Response, next: NextFunction) => {
    const originalSend = res.send;
    
    res.send = function(data) {
      if (res.statusCode >= 200 && res.statusCode < 300) {
        // Log successful operation
        AuditService.log(action, entityType, {
          userId: req.user?.id,
          ip: req.ip,
          details: req.body,
        }).catch(err => console.error('Audit log error:', err));
      }
      return originalSend.call(this, data);
    };
    
    next();
  };
};
```

### 3. **Enhancer: Transaction Middleware** (`src/middleware/transaction.ts`) - OPCIONAL

Soporte para transacciones en rutas críticas:

```typescript
export const withTransaction = async (fn: Function) => {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const result = await fn(client);
    await client.query('COMMIT');
    return result;
  } catch (error) {
    await client.query('ROLLBACK');
    throw error;
  } finally {
    client.release();
  }
};
```

---

## ORDEN DE IMPLEMENTACIÓN

### Fase A: Preparación (1-2 commits)

1. **Crear archivos esqueleto**:
   - `src/services/tripService.ts` (skeleton con métodos vacíos)
   - `src/services/truckService.ts`
   - `src/services/adminService.ts`
   - `src/repositories/tripRepository.ts`
   - `src/repositories/truckRepository.ts`
   - `src/repositories/configRepository.ts`
   - `src/routes/trips.ts`
   - `src/routes/trucks.ts`
   - `src/routes/admin.ts`

2. **Agregar schemas a `src/types/schemas.ts`**:
   - `CreateTripSchema`, `CancelTripSchema`, `TripsFilterSchema`
   - `UpdateTruckLocationSchema`, `UpdateTruckSchema`
   - `UpdateConfigSchema`
   - `PaginationSchema`, `AuditFilterSchema`

3. **Registrar rutas en `src/index.ts`**:
   ```typescript
   app.use('/api/v1/viajes', tripsRouter);
   app.use('/api/v1/camiones', trucksRouter);
   app.use('/api/v1/admin', adminRouter);
   ```

### Fase B: Repositorios (1 commit)

4. **Implementar `TripRepository`**:
   - `create()`
   - `findById()`
   - `findByUserId()`
   - `findAvailable()`
   - `updateState()`
   - `acceptTrip()` con transacción
   - `findPaginated()`

5. **Implementar `TruckRepository`**:
   - CRUD básico
   - `updateLocation()`
   - `updateState()`
   - `softDelete()`
   - `findPaginated()`

6. **Implementar `ConfigRepository`**:
   - `get()` (singleton)
   - `update()`

### Fase C: Servicios — Parte 1 (2-3 commits)

7. **Implementar `TripService` — Métodos básicos**:
   - `getTripById()`
   - `calculateDistance()` (Haversine)
   - `calculatePrice()`
   - `createTrip()` con validaciones
   - `getMyTrips()`
   - Commit: "feat: TripService - Trip creation and queries"

8. **Implementar `TripService` — Operaciones de estado**:
   - `getAvailableTrips()`
   - `acceptTrip()` (transactional)
   - `cancelTrip()` (transactional)
   - `markEnRoute()`
   - `markCompleted()`
   - Commit: "feat: TripService - State transitions and transactions"

### Fase D: Servicios — Parte 2 (1-2 commits)

9. **Implementar `TruckService`**:
   - `getAvailableTrucks()`
   - `getMyTruck()`
   - `updateLocation()`
   - `updateTruck()`
   - `deleteTruck()`
   - Commit: "feat: TruckService - Truck management"

10. **Implementar `AdminService`**:
    - `listUsers()`
    - `getUserDetails()`
    - `getConfiguration()`
    - `updateConfiguration()`
    - `getAuditLog()`
    - Commit: "feat: AdminService - Admin operations"

### Fase E: Rutas (1-2 commits)

11. **Implementar Trips Routes**:
    - Todos 8 endpoints
    - Integración con TripService
    - Manejo de errores
    - Commit: "feat: Trips API - 8 endpoints"

12. **Implementar Trucks Routes** + **Admin Routes**:
    - Todos 5 endpoints de camiones
    - Todos 4+ endpoints de admin
    - Commit: "feat: Trucks and Admin APIs - 9 endpoints"

### Fase F: Validación y Testing (1-2 commits)

13. **Testing e integración**:
    - Pruebas unitarias de servicios
    - Pruebas de integración de endpoints
    - Pruebas de transacciones (Haversine)
    - Pruebas de permisos (RBAC)
    - Commit: "test: FASE 3 comprehensive test suite"

14. **Documentación y validación final**:
    - README.md updates
    - Postman/Swagger collection
    - Manual validation report
    - Commit: "docs: FASE 3 validation report"

**Total esperado**: 8-10 commits

---

## CONSIDERACIONES DE SEGURIDAD

### 1. **Autorización y Autenticación**

- ✅ `requireAuth` middleware en todos los endpoints
- ✅ `requireRole('usuario' | 'chofer' | 'admin')` en rutas sensitivas
- ✅ **Owner check**: En operaciones como cancelar viaje, verificar que `req.user.id === viaje.usuario_id || viaje.chofer_id`

**Patrón**:
```typescript
const viaje = await TripService.getTripById(tripId);
if (viaje.usuario_id !== req.user!.id && viaje.chofer_id !== req.user!.id) {
  throw new AppError(403, 'Not owner of this trip', 'FORBIDDEN');
}
```

### 2. **Validación de Entrada**

- ✅ Zod schemas para todo input
- ✅ Coords (lat/lng) validadas: lat ∈ [-90, 90], lng ∈ [-180, 180]
- ✅ CUIT format: `XX-XXXXXXXX-X` (regex)
- ✅ Distancias/precios: No negativos

### 3. **Integridad Transaccional**

- ✅ Optimistic locking en viajes (version column)
- ✅ Transacción al aceptar viaje (update viajes + camiones atómicamente)
- ✅ Rollback en caso de error
- ✅ No race conditions en acceptTrip

### 4. **Auditoría**

- ✅ Log: trip_created, trip_accepted, trip_cancelled, trip_completed
- ✅ Log: truck_updated, location_updated, truck_deleted
- ✅ Log: config_updated (old vs new values)
- ✅ Timestamp + user_id + ip_address en cada log

### 5. **Rate Limiting**

Considerar implementar rate limiting específico:

```typescript
// En src/middleware/rateLimit.ts
export const tripCreationLimiter = rateLimit({
  windowMs: 60 * 1000, // 1 min
  max: 5, // 5 viajes por minuto
  key: (req) => req.user?.id || req.ip,
});
```

### 6. **Datos Sensibles**

- ✅ No exponer `password_hash` en respuestas
- ✅ No exponer `google_id` sin necesidad
- ✅ CUIT y teléfono: considerar mascarado en listados públicos

### 7. **Soft Deletes**

- ✅ Todos los queries incluyen `WHERE is_deleted = false`
- ✅ Deletes actualizan flag en vez de borrar
- ✅ Audit log preserva datos

---

## ESTRATEGIA DE TESTING

### Unit Tests (src/services/__tests__)

**TripService tests** (~30 tests):

```typescript
describe('TripService', () => {
  describe('createTrip', () => {
    test('should create trip with valid data', async () => { /* */ });
    test('should reject negative distance', () => { /* */ });
    test('should reject invalid coords', () => { /* */ });
    test('should calculate Haversine correctly', () => { /* */ });
  });
  
  describe('acceptTrip', () => {
    test('should accept trip with valid chofer', async () => { /* */ });
    test('should reject if trip not in solicitado state', () => { /* */ });
    test('should handle optimistic lock conflict', () => { /* */ });
    test('should mark truck as ocupado', async () => { /* */ });
  });
  
  describe('calculatePrice', () => {
    test('should calculate por_km correctly', () => { /* */ });
    test('should calculate mixed por_km + por_tonelada', () => { /* */ });
    test('should round to 2 decimals', () => { /* */ });
  });
  
  describe('cancelTrip', () => {
    test('should cancel if owner is usuario_id', async () => { /* */ });
    test('should cancel if owner is chofer_id', async () => { /* */ });
    test('should reject if estado=en_camino', () => { /* */ });
    test('should free up truck if chofer_id set', async () => { /* */ });
  });
});
```

**TruckService tests** (~15 tests):

```typescript
describe('TruckService', () => {
  describe('getMyTruck', () => {
    test('should return truck for chofer', async () => { /* */ });
    test('should throw 404 if no truck assigned', () => { /* */ });
  });
  
  describe('updateLocation', () => {
    test('should update GPS coordinates', async () => { /* */ });
    test('should validate coords', () => { /* */ });
  });
  
  describe('deleteTruck', () => {
    test('should soft delete truck', async () => { /* */ });
    test('should prevent delete if active trips exist', () => { /* */ });
  });
});
```

**AdminService tests** (~10 tests):

```typescript
describe('AdminService', () => {
  describe('updateConfiguration', () => {
    test('should update singleton config', async () => { /* */ });
    test('should validate tariff > 0', () => { /* */ });
    test('should log old vs new values', async () => { /* */ });
  });
  
  describe('getAuditLog', () => {
    test('should return paginated audit log', async () => { /* */ });
    test('should filter by userId', async () => { /* */ });
    test('should filter by action', async () => { /* */ });
    test('should filter by date range', async () => { /* */ });
  });
});
```

### Integration Tests (src/routes/__tests__)

**Trips routes** (~20 tests):

```typescript
describe('POST /api/v1/viajes', () => {
  test('should create trip for usuario', async () => {
    const res = await request(app)
      .post('/api/v1/viajes')
      .set('Cookie', `nexvia_session=${sessionId}`)
      .send({
        origen: { lat: -34.6037, lng: -58.3816 },
        destino: { lat: -34.7037, lng: -58.4816 },
      });
    
    expect(res.status).toBe(201);
    expect(res.body.data.id).toBeDefined();
    expect(res.body.data.estado).toBe('solicitado');
  });
  
  test('should reject chofer creating trip', () => { /* */ });
  test('should reject invalid coords', () => { /* */ });
  test('should calculate distance correctly', () => { /* */ });
});

describe('POST /api/v1/viajes/:id/aceptar', () => {
  test('should accept trip for chofer with truck', async () => { /* */ });
  test('should update truck state to ocupado', async () => { /* */ });
  test('should handle optimistic lock conflict', async () => { /* */ });
  test('should reject if chofer has no truck', () => { /* */ });
  test('should reject non-chofer', () => { /* */ });
});

describe('POST /api/v1/viajes/:id/cancelar', () => {
  test('should cancel trip if owner is usuario', () => { /* */ });
  test('should cancel trip if owner is chofer', () => { /* */ });
  test('should reject if not owner', () => { /* */ });
  test('should free truck if chofer_id set', () => { /* */ });
});
```

**Trucks routes** (~10 tests):

```typescript
describe('GET /api/v1/camiones/mi-camion', () => {
  test('should return chofer\'s truck', async () => { /* */ });
  test('should reject non-chofer', () => { /* */ });
  test('should throw 404 if no truck', () => { /* */ });
});

describe('POST /api/v1/camiones/:id/ubicacion', () => {
  test('should update GPS location', async () => { /* */ });
  test('should validate coords', () => { /* */ });
  test('should log to audit', () => { /* */ });
});
```

**Admin routes** (~10 tests):

```typescript
describe('GET /api/v1/admin/usuarios', () => {
  test('should list users (admin only)', async () => { /* */ });
  test('should reject non-admin', () => { /* */ });
  test('should filter by role', () => { /* */ });
  test('should not expose password_hash', () => { /* */ });
});

describe('PUT /api/v1/admin/configuracion', () => {
  test('should update configuration', async () => { /* */ });
  test('should log changes', () => { /* */ });
  test('should invalidate cache', () => { /* */ });
});
```

### Test Database Setup

```typescript
// tests/setup.ts
import { Pool } from 'pg';

export const testPool = new Pool({
  host: 'localhost',
  port: 5432,
  database: 'nexvia_test',
  user: 'postgres',
  password: 'postgres',
});

beforeEach(async () => {
  // Cleanup: DELETE all tables
  await testPool.query('TRUNCATE TABLE viajes, camiones, users, configuracion CASCADE');
  
  // Seed test data
  await seedTestData();
});

afterAll(async () => {
  await testPool.end();
});

async function seedTestData() {
  // Insert test users, trucks, config
}
```

### Test Coverage Target

- **Statements**: 85%+
- **Branches**: 80%+
- **Functions**: 90%+
- **Lines**: 85%+

---

## CASOS DE BORDE Y MITIGACIONES

### 1. **Race Condition en Aceptar Viaje**

**Escenario**: Dos choferes intentan aceptar el mismo viaje simultáneamente.

**Mitigación**:
- ✅ Optimistic locking en viajes table (version column)
- ✅ Transacción: UPDATE viajes WHERE id=? AND version=? AND estado='solicitado'
- ✅ Si version mismatch: retorna 409 Conflict
- ✅ Cliente reintenta con data actualizado

**Test**:
```typescript
test('should handle concurrent accept attempts', async () => {
  const [res1, res2] = await Promise.all([
    TripService.acceptTrip(tripId, chofer1Id),
    TripService.acceptTrip(tripId, chofer2Id),
  ]);
  
  expect(res1.status).toBe(200);
  expect(res2.status).toBe(409); // Conflict
});
```

### 2. **Truck Disponible pero Sin Viajes Activos**

**Escenario**: Truck se marca disponible antes de que viaje esté completado.

**Mitigación**:
- ✅ Al crear viaje → truck estado = 'ocupado'
- ✅ Al completar/cancelar → truck estado = 'disponible'
- ✅ Verificación en aceptar viaje: `estado = 'disponible'`

### 3. **Coords Inválidas o Fuera de Rango**

**Escenario**: Usuario envía lat=200 (inválido).

**Mitigación**:
- ✅ Zod schema valida lat ∈ [-90, 90], lng ∈ [-180, 180]
- ✅ Retorna 400 Bad Request con mensaje claro

### 4. **Precio Negativo o Cero**

**Escenario**: Tarifa configurada como 0 o negativa.

**Mitigación**:
- ✅ ConfigRepository valida: tarifa > 0
- ✅ En calculatePrice: retorna error si tarifa <= 0
- ✅ Admin UI debe validar antes de actualizar

### 5. **Viaje Cancelado Antes de Ser Aceptado**

**Escenario**: Usuario cancela viaje que chofer está intentando aceptar.

**Mitigación**:
- ✅ acceptTrip verifica estado='solicitado'
- ✅ Si viaje ya cancelado: estado='cancelado' → throw 422 'Trip not available'

### 6. **Chofer Sin Camion Asignado**

**Escenario**: Chofer intenta aceptar viaje pero no tiene camion.

**Mitigación**:
- ✅ En acceptTrip: verificar TruckRepository.findByUserId(choferId) != null
- ✅ Si null: throw 403 'No truck assigned'

### 7. **Admin Intenta Eliminar Camion con Viajes Activos**

**Escenario**: Admin llama DELETE /camiones/:id pero hay viaje en estado 'en_camino'.

**Mitigación**:
- ✅ deleteTruck verifica: no hay viajes con estado != 'completado' || 'cancelado'
- ✅ Si hay: throw 409 'Cannot delete truck with active trips'
- ✅ Admin debe esperar a que viajes terminen

### 8. **Audit Log Muy Grande**

**Escenario**: Tabla audit_log crece muy rápido, queries son lentos.

**Mitigación**:
- ✅ Índice en (user_id, created_at)
- ✅ Índice en (action, created_at)
- ✅ Paginación en getAuditLog (max 100 items)
- ✅ OPCIONALMENTE: archivado de logs antiguos (> 90 días)

### 9. **Distancia Haversine Incorrecta**

**Escenario**: Coords en antípodas o edge cases.

**Mitigación**:
- ✅ Fórmula Haversine maneja casos extremos
- ✅ Tests unitarios con coords conocidas (ej: Buenos Aires a Nueva York ≈ 11,000 km)
- ✅ Verificar con herramientas online (Google Maps)

### 10. **Session Expirada Durante Operación**

**Escenario**: Chofer acepta viaje pero sesión expira.

**Mitigación**:
- ✅ authMiddleware verifica sesión en cada request
- ✅ Si expirada: throw 401 Unauthorized
- ✅ Cliente debe re-login
- ✅ Viaje en estado 'solicitado' permanece disponible

---

## ROLLBACK STRATEGY

Si surge error crítico durante FASE 3:

### Option 1: Revert FASE 3 commit
```bash
git reset --hard <commit-ante-fase-3>
git push origin migration --force
```

### Option 2: Disable endpoints (sin revert)
```typescript
// En index.ts, comentar:
// app.use('/api/v1/viajes', tripsRouter);
// app.use('/api/v1/camiones', trucksRouter);
// app.use('/api/v1/admin', adminRouter);

// Los endpoints 404, pero código está en repo
```

### Option 3: Feature flag
```typescript
if (config.ENABLE_FASE3_APIS) {
  app.use('/api/v1/viajes', tripsRouter);
  app.use('/api/v1/camiones', trucksRouter);
  app.use('/api/v1/admin', adminRouter);
}
```

**Tiempo de rollback**: < 5 min

**Data loss**: Ninguno (todas las operaciones son inserts/updates, reversibles en DB)

---

## CRITERIOS DE ACEPTACIÓN

### ✅ Endpoints Funcionan
- [ ] POST /api/v1/viajes (crear viaje)
- [ ] GET /api/v1/viajes/mis-viajes (listar mis viajes)
- [ ] GET /api/v1/viajes/disponibles (disponibles para chofer)
- [ ] POST /api/v1/viajes/:id/aceptar (aceptar)
- [ ] POST /api/v1/viajes/:id/cancelar (cancelar)
- [ ] POST /api/v1/viajes/:id/en-camino (en ruta)
- [ ] POST /api/v1/viajes/:id/completar (completado)
- [ ] GET /api/v1/camiones/disponibles (trucks disponibles)
- [ ] GET /api/v1/camiones/mi-camion (mi truck)
- [ ] POST /api/v1/camiones/:id/ubicacion (GPS)
- [ ] PUT /api/v1/camiones/:id (actualizar, admin only)
- [ ] DELETE /api/v1/camiones/:id (soft delete, admin only)
- [ ] GET /api/v1/admin/usuarios (listar usuarios)
- [ ] GET /api/v1/admin/usuarios/:id (detalles)
- [ ] PUT /api/v1/admin/configuracion (actualizar config)
- [ ] GET /api/v1/admin/audit-log (audit log)

### ✅ Validación y Seguridad
- [ ] Zod schemas válidan todo input
- [ ] RBAC funciona (requireRole middleware)
- [ ] Owner checks en operaciones de usuario
- [ ] Optimistic locking en acceptTrip
- [ ] Transacciones atómicas (viaje + truck)
- [ ] Audit logging en operaciones críticas
- [ ] Errores manejo correcto (AppError)
- [ ] No password_hash en respuestas

### ✅ Funcionalidad de Negocio
- [ ] Haversine calcula distancia correctamente
- [ ] Pricing se aplica correctamente (por_km, por_tonelada)
- [ ] Estados de viaje fluyen correctamente
- [ ] Estados de truck sincronizados con viajes
- [ ] Viajes cancelados liberan trucks
- [ ] Choferes solo ven viajes disponibles
- [ ] Usuarios solo ven sus viajes

### ✅ Testing
- [ ] Cobertura > 85% (statements)
- [ ] Unit tests para servicios
- [ ] Integration tests para endpoints
- [ ] Tests de permisos (RBAC)
- [ ] Tests de transacciones
- [ ] Tests de edge cases

### ✅ Documentación
- [ ] README actualizado con nuevos endpoints
- [ ] Swagger/OpenAPI spec (opcional pero recomendado)
- [ ] Manual validation report
- [ ] Postman collection
- [ ] Ejemplos de curl para cada endpoint

### ✅ Git State
- [ ] 8-10 commits coherentes
- [ ] Branch: `migration`
- [ ] Listo para merge a `main`
- [ ] Sin cambios sin commitear
- [ ] Historial limpio

---

## PRÓXIMAS FASES

- **FASE 4**: Payment Integration (Stripe/MercadoPago)
- **FASE 5**: Notifications (WebSocket, Push, Email)
- **FASE 6**: Maps Integration (Google Maps API)
- **FASE 7**: Frontend Integration (React components)
- **FASE 8**: DevOps & Deployment (Docker, CI/CD)

---

## Conclusión

Este plan proporciona:
- ✅ **17+ endpoints** bien definidos
- ✅ **3 servicios** con lógica de negocio clara
- ✅ **Integridad transaccional** en operaciones críticas
- ✅ **RBAC completo** con owner checks
- ✅ **Validación exhaustiva** con Zod
- ✅ **Auditoría completa** de operaciones
- ✅ **Mitigaciones** para casos de borde
- ✅ **Testing strategy** con >85% cobertura
- ✅ **Orden de implementación** claro (8-10 commits)

**Implementación estimada**: 8-10 horas (para un desarrollador)

**Inicio recomendado**: Inmediatamente después de FASE 2
