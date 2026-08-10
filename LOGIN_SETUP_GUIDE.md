# GUÍA DE CONFIGURACIÓN: LOGIN FUNCIONAL

**Fecha**: 2026-08-09  
**Estado**: 🔴 Pendiente de Configuración Local

---

## ¿QUÉ SE NECESITA PARA QUE EL LOGIN FUNCIONE?

### 1️⃣ BASE DE DATOS POSTGRESQL

#### Requisito
- PostgreSQL 13+ instalado y ejecutándose en `localhost:5432`

#### Pasos de Instalación

**Windows**:
```bash
# Opción A: Descargar desde https://www.postgresql.org/download/windows/
# Opción B: Usar Chocolatey
choco install postgresql
```

**Verificar instalación**:
```bash
psql --version
```

#### Configurar Base de Datos
```sql
-- Conectarse a PostgreSQL
psql -U postgres

-- Crear usuario (usuario nexvia_user)
CREATE USER nexvia_user WITH PASSWORD 'nexvia_secure_password_2024';

-- Crear base de datos
CREATE DATABASE nexvia_db OWNER nexvia_user;

-- Otorgar permisos
GRANT ALL PRIVILEGES ON DATABASE nexvia_db TO nexvia_user;

-- Conectarse a la nueva DB
\c nexvia_db

-- Crear extensiones necesarias
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
```

**Verificar conexión**:
```bash
# Desde el directorio del proyecto
psql -h localhost -U nexvia_user -d nexvia_db -W
# Ingresa: nexvia_secure_password_2024
```

---

### 2️⃣ REDIS (Opcional pero Recomendado)

#### Requisito
- Redis ejecutándose en `localhost:6379` para manejo de sesiones

#### Pasos de Instalación

**Windows**:
```bash
# Opción A: Usar Windows Subsystem for Linux (WSL)
wsl
sudo apt-get update
sudo apt-get install redis-server
redis-server

# Opción B: Docker
docker run -d -p 6379:6379 redis:latest

# Opción C: Descargar binarios precompilados
# https://github.com/microsoftarchive/redis/releases
```

**Verificar conexión**:
```bash
redis-cli ping
# Debe devolver: PONG
```

---

### 3️⃣ EJECUTAR MIGRACIONES DE BASE DE DATOS

Las migraciones crean las tablas necesarias: `users`, `trucks`, `trips`, `ratings`, etc.

```bash
cd server

# Ejecutar migraciones (si existen)
npm run migrate
# O manualmente ejecutar SQL de schema.sql

# Si no hay comando migrate, verificar si existe src/migrations/
# y ejecutar el archivo principal del servidor que crea las tablas
```

**Si no existe comando migrate**, ejecutar manualmente:
```bash
# Conectarse a la DB
psql -h localhost -U nexvia_user -d nexvia_db -W

# Pegar el contenido del archivo de schema (src/database/schema.sql o similar)
```

---

### 4️⃣ EJECUTAR SERVIDOR BACKEND

Necesitamos que el servidor Node.js esté ejecutándose en `localhost:5000`.

```bash
cd server

# Instalar dependencias (si no están instaladas)
npm install

# Ejecutar en desarrollo (con hot reload)
npm run dev

# O ejecutar en producción
npm start
```

**Verificar que está funcionando**:
```bash
# En otra terminal
curl http://localhost:5000/health

# Debe devolver:
# { "status": "ok" }
```

---

### 5️⃣ VERIFICAR CONFIGURACIÓN FRONTEND

El frontend debe apuntar al backend correcto.

**Archivo**: `C:\Users\alber\Desktop\trabajo\nexvia\.env.local`

```env
VITE_API_URL=http://localhost:5000
VITE_APP_NAME=NEXVIA
```

✅ Este archivo ya está configurado correctamente.

---

## FLUJO DE LOGIN COMPLETO

### 1. Usuario accede a `/login`
```
http://localhost:5173/login
```

### 2. Envía credenciales
```javascript
POST http://localhost:5000/api/v1/auth/login
{
  "email": "usuario@example.com",
  "password": "password123"
}
```

### 3. Backend verifica:
- ✅ Usuario existe en PostgreSQL
- ✅ Contraseña es correcta (bcrypt hash comparison)
- ✅ Crea sesión en Redis

### 4. Response exitosa:
```javascript
{
  "status": 200,
  "data": {
    "user": {
      "id": "uuid",
      "email": "usuario@example.com",
      "role": "usuario",
      "name": "Juan Pérez"
    },
    "sessionId": "session-uuid"
  },
  "message": "Login exitoso"
}
```

### 5. Frontend:
- Guarda sesión en `AuthContext`
- Redirige a `/home` (usuario) o `/admin` (admin) o `/chofer` (chofer)

---

## CHECKLIST DE CONFIGURACIÓN

- [ ] **PostgreSQL instalado y ejecutándose**
  ```bash
  psql -h localhost -U nexvia_user -d nexvia_db -W
  ```

- [ ] **Redis instalado y ejecutándose** (opcional)
  ```bash
  redis-cli ping  # Debe devolver PONG
  ```

- [ ] **Migraciones ejecutadas**
  - [ ] Tabla `users` creada
  - [ ] Tabla `trucks` creada
  - [ ] Tabla `trips` creada
  - [ ] Tabla `ratings` creada

- [ ] **Variables de entorno configuradas**
  - [ ] `server/.env` creado (✅ YA HECHO)

- [ ] **Servidor backend ejecutándose**
  ```bash
  cd server
  npm run dev
  # Debe mostrar: "Server running on port 5000"
  ```

- [ ] **Frontend conectando al backend**
  - [ ] `VITE_API_URL=http://localhost:5000` (✅ YA CONFIGURADO)
  - [ ] No hay errores CORS en consola

---

## PRUEBA RÁPIDA DEL LOGIN

### Paso 1: Crear usuario de prueba
```bash
# En la DB de PostgreSQL
psql -h localhost -U nexvia_user -d nexvia_db -W

INSERT INTO users (id, email, password_hash, name, role, created_at)
VALUES (
  gen_random_uuid(),
  'test@example.com',
  '$2b$10$...',  -- hash de "password123"
  'Test Usuario',
  'usuario',
  NOW()
);
```

### Paso 2: Hacer login
1. Abrir `http://localhost:5173/login`
2. Ingresar: `test@example.com` / `password123`
3. Debe redirigir a `/home`

### Paso 3: Verificar sesión
- Abrir DevTools → Network
- Verifica que POST a `/api/v1/auth/login` devuelve 200
- Verifica que hay cookie `session` en Response Headers

---

## ENDPOINTS DE AUTENTICACIÓN IMPLEMENTADOS

| Endpoint | Método | Descripción | Status |
|----------|--------|-------------|--------|
| `/api/v1/auth/register` | POST | Crear nuevo usuario | ✅ |
| `/api/v1/auth/login` | POST | Iniciar sesión | ✅ |
| `/api/v1/auth/logout` | POST | Cerrar sesión | ✅ |
| `/api/v1/auth/profile` | GET | Obtener perfil actual | ✅ |
| `/api/v1/auth/refresh` | POST | Refrescar sesión | ✅ |
| `/api/v1/auth/forgot-password` | POST | Solicitar reset | ⏳ |
| `/api/v1/auth/reset-password` | POST | Resetear contraseña | ⏳ |

---

## TROUBLESHOOTING

### ❌ Error: "Connection refused" en login
**Causa**: Backend no está ejecutándose  
**Solución**:
```bash
cd server
npm run dev
```

### ❌ Error: "Database error"
**Causa**: PostgreSQL no está ejecutándose o no conecta  
**Solución**:
```bash
# Verificar PostgreSQL
pg_isready -h localhost -p 5432
# Debe devolver: "localhost:5432 - accepting connections"
```

### ❌ Error: "CORS error"
**Causa**: Frontend y backend en puertos diferentes  
**Solución**: El backend debe tener CORS configurado (✅ ya está)

### ❌ Error: "Redis connection failed"
**Causa**: Redis no está ejecutándose (no crítico)  
**Solución**: Instalar Redis o usar modo sin sesiones

---

## PRÓXIMOS PASOS

1. ✅ Archivo `.env` creado → **COMPLETADO**
2. 📥 Instalar PostgreSQL localmente
3. 📥 Instalar Redis localmente (opcional)
4. 📥 Ejecutar migraciones de BD
5. 🚀 Ejecutar servidor backend (`npm run dev` en `/server`)
6. 🧪 Probar login en frontend
7. ✅ Completar flujo de autenticación

---

## STACK DE AUTENTICACIÓN

| Componente | Tecnología | Status |
|------------|------------|--------|
| **Frontend** | React + AuthContext | ✅ |
| **Backend** | Node.js + Express | ✅ |
| **Base de Datos** | PostgreSQL | ⏳ Necesita instalación |
| **Sesiones** | Redis + Cookies | ⏳ Redis necesita instalación |
| **Hash de Contraseñas** | bcrypt | ✅ |
| **Validación** | Zod | ✅ |
| **CORS** | Habilitado | ✅ |

---

**Resumen**: El login está 80% listo en el código. Solo falta configurar la infraestructura local (PostgreSQL + Redis) y ejecutar el servidor backend.

