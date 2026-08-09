-- Create ENUM types
CREATE TYPE user_role AS ENUM ('admin', 'chofer', 'usuario');
CREATE TYPE viaje_estado AS ENUM ('solicitado', 'aceptado', 'en_camino', 'completado', 'cancelado');
CREATE TYPE camion_estado AS ENUM ('disponible', 'ocupado', 'inactivo');
CREATE TYPE lugar_tipo AS ENUM ('pueblo', 'hacienda', 'campo', 'otro');
CREATE TYPE audit_action AS ENUM ('create', 'update', 'delete', 'login', 'logout', 'password_change', 'role_change');

-- Users table
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email TEXT UNIQUE NOT NULL,
  email_verified BOOLEAN DEFAULT FALSE,
  password_hash TEXT,
  full_name TEXT,
  phone TEXT,
  avatar_url TEXT,
  role user_role DEFAULT 'usuario' NOT NULL,
  google_id TEXT UNIQUE,
  is_active BOOLEAN DEFAULT TRUE NOT NULL,
  is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
  last_login_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_users_email ON users(email) WHERE NOT is_deleted;
CREATE INDEX idx_users_google_id ON users(google_id) WHERE NOT is_deleted;
CREATE INDEX idx_users_role ON users(role) WHERE NOT is_deleted;

-- Configuracion table (singleton)
CREATE TABLE configuracion (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  zona_nombre TEXT DEFAULT 'Default Zone' NOT NULL,
  centro_lat DOUBLE PRECISION DEFAULT -32.4341 NOT NULL,
  centro_lng DOUBLE PRECISION DEFAULT -63.2433 NOT NULL,
  tarifa_por_km DECIMAL(10, 2) DEFAULT 500.00 NOT NULL,
  tarifa_por_tonelada DECIMAL(10, 2) DEFAULT 1000.00 NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_configuracion_id ON configuracion(id);

-- Camiones table
CREATE TABLE camiones (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  patente TEXT UNIQUE NOT NULL,
  patente_acoplado TEXT,
  transporte_nombre TEXT,
  transporte_cuit TEXT,
  chofer_nombre TEXT NOT NULL,
  chofer_cuit TEXT,
  telefono TEXT,
  capacidad_kg INTEGER,
  lat DOUBLE PRECISION DEFAULT 0,
  lng DOUBLE PRECISION DEFAULT 0,
  estado camion_estado DEFAULT 'disponible' NOT NULL,
  is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_camiones_user_id ON camiones(user_id) WHERE NOT is_deleted;
CREATE INDEX idx_camiones_patente ON camiones(patente) WHERE NOT is_deleted;
CREATE INDEX idx_camiones_estado ON camiones(estado) WHERE NOT is_deleted;

-- Viajes table
CREATE TABLE viajes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  usuario_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  chofer_id UUID REFERENCES users(id) ON DELETE SET NULL,
  camion_id UUID REFERENCES camiones(id) ON DELETE SET NULL,
  origen_lat DOUBLE PRECISION NOT NULL,
  origen_lng DOUBLE PRECISION NOT NULL,
  destino_lat DOUBLE PRECISION NOT NULL,
  destino_lng DOUBLE PRECISION NOT NULL,
  distancia_km DECIMAL(10, 2) NOT NULL,
  toneladas DECIMAL(10, 2),
  tipo_tarifa VARCHAR(20) DEFAULT 'por_km' NOT NULL,
  precio DECIMAL(12, 2) NOT NULL,
  tarifa_unitaria DECIMAL(10, 2) NOT NULL,
  carga TEXT,
  estado viaje_estado DEFAULT 'solicitado' NOT NULL,
  version INTEGER DEFAULT 1 NOT NULL,
  solicitado_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
  aceptado_at TIMESTAMP WITH TIME ZONE,
  en_camino_at TIMESTAMP WITH TIME ZONE,
  completado_at TIMESTAMP WITH TIME ZONE,
  cancelado_at TIMESTAMP WITH TIME ZONE,
  cancelacion_razon TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_viajes_usuario_id ON viajes(usuario_id);
CREATE INDEX idx_viajes_chofer_id ON viajes(chofer_id);
CREATE INDEX idx_viajes_camion_id ON viajes(camion_id);
CREATE INDEX idx_viajes_estado ON viajes(estado);
CREATE INDEX idx_viajes_created_at ON viajes(created_at DESC);
CREATE INDEX idx_viajes_version ON viajes(version);

-- Lugares guardados table
CREATE TABLE lugares_guardados (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  nombre TEXT NOT NULL,
  lat DOUBLE PRECISION NOT NULL,
  lng DOUBLE PRECISION NOT NULL,
  tipo lugar_tipo DEFAULT 'otro',
  is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_lugares_user_id ON lugares_guardados(user_id) WHERE NOT is_deleted;
CREATE INDEX idx_lugares_nombre ON lugares_guardados(nombre) WHERE NOT is_deleted;

-- Email verification tokens
CREATE TABLE email_verification_tokens (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  code_hash TEXT NOT NULL,
  attempts INTEGER DEFAULT 0,
  is_used BOOLEAN DEFAULT FALSE NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
  expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_email_tokens_user_id ON email_verification_tokens(user_id);
CREATE INDEX idx_email_tokens_expires_at ON email_verification_tokens(expires_at);

-- Password reset tokens
CREATE TABLE password_reset_tokens (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash TEXT NOT NULL UNIQUE,
  is_used BOOLEAN DEFAULT FALSE NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
  expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_expires_at ON password_reset_tokens(expires_at);

-- Audit log (append-only)
CREATE TABLE audit_log (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  action audit_action NOT NULL,
  entity_type VARCHAR(50) NOT NULL,
  entity_id UUID,
  details JSONB,
  ip_address INET,
  user_agent TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX idx_audit_log_action ON audit_log(action);
CREATE INDEX idx_audit_log_entity_type ON audit_log(entity_type);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at DESC);

-- Sessions table (for backup/durability, primary storage is Redis)
CREATE TABLE sessions (
  id VARCHAR(255) PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  ip_address INET,
  user_agent TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
  expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
  last_activity_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_sessions_expires_at ON sessions(expires_at);

-- Migrations tracking table
CREATE TABLE IF NOT EXISTS _migrations (
  id SERIAL PRIMARY KEY,
  name TEXT UNIQUE NOT NULL,
  executed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
