# NEXVIA

Plataforma de logística agro — conectá carga y transporte en tiempo real.

Monorepo independiente, **sin dependencias de Base44**.

## Estructura

```
nexvia/
├── front/   # Frontend React + Vite (landing 3D + app)
└── back/    # Backend Express + PostgreSQL + TypeScript
```

## Backend (`back/`)

API REST en `Express + TypeScript`, sesiones por cookie, PostgreSQL.

```bash
cd back
npm install
npm run migrate   # aplicar migraciones
npm run dev       # http://localhost:3000  (API en /api/v1)
```

## Frontend (`front/`)

React + Vite + Tailwind. Consume el backend vía `/api/v1` (proxy de Vite → `http://localhost:3000`).

```bash
cd front
npm install
npm run dev       # http://localhost:5173
```

La capa de datos vive en `front/src/api/`:
- `apiClient.js` — wrapper `fetch` (cookies incluidas).
- `client.js` — objeto `nexvia` con `auth`, `entities` y `functions`, mapeado a los endpoints reales del backend.

## Migración desde Base44

Este proyecto reemplazó el SDK de Base44 por un backend propio. No debe reintroducirse
ninguna dependencia `@base44/*` ni URLs `media.base44.com`.
