export interface User {
  id: string;
  email: string;
  email_verified: boolean;
  password_hash?: string;
  full_name?: string;
  phone?: string;
  avatar_url?: string;
  role: 'admin' | 'chofer' | 'usuario';
  google_id?: string;
  is_active: boolean;
  is_deleted: boolean;
  created_at: string;
  updated_at: string;
  last_login_at?: string;
}

export interface Viaje {
  id: string;
  usuario_id: string;
  chofer_id?: string;
  camion_id?: string;
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
  estado: 'solicitado' | 'aceptado' | 'en_camino' | 'completado' | 'cancelado';
  version: number;
  solicitado_at: string;
  aceptado_at?: string;
  en_camino_at?: string;
  completado_at?: string;
  cancelado_at?: string;
  cancelacion_razon?: string;
  created_at: string;
  updated_at: string;
}

export interface Camion {
  id: string;
  user_id?: string;
  patente: string;
  patente_acoplado?: string;
  transporte_nombre?: string;
  transporte_cuit?: string;
  chofer_nombre: string;
  chofer_cuit?: string;
  telefono?: string;
  capacidad_kg?: number;
  lat: number;
  lng: number;
  estado: 'disponible' | 'ocupado' | 'inactivo';
  is_deleted: boolean;
  created_at: string;
  updated_at: string;
}

export interface Configuracion {
  id: string;
  zona_nombre: string;
  centro_lat: number;
  centro_lng: number;
  tarifa_por_km: number;
  tarifa_por_tonelada: number;
  created_at: string;
  updated_at: string;
}

export interface LugarGuardado {
  id: string;
  user_id: string;
  nombre: string;
  lat: number;
  lng: number;
  tipo: 'pueblo' | 'hacienda' | 'campo' | 'otro';
  is_deleted: boolean;
  created_at: string;
  updated_at: string;
}

export interface Session {
  id: string;
  user_id: string;
  ip_address?: string;
  user_agent?: string;
  created_at: string;
  expires_at: string;
  last_activity_at: string;
}

export interface AuditLog {
  id: string;
  user_id?: string;
  action: 'create' | 'update' | 'delete' | 'login' | 'logout' | 'password_change' | 'role_change';
  entity_type: string;
  entity_id?: string;
  details?: Record<string, any>;
  ip_address?: string;
  user_agent?: string;
  created_at: string;
}

// Request extensions
declare global {
  namespace Express {
    interface Request {
      user?: User;
      sessionId?: string;
      clientIp?: string;
      userAgent?: string;
    }
  }
}
