import { z } from 'zod';

// Password validation
const passwordSchema = z
  .string()
  .min(8, 'Password must be at least 8 characters')
  .regex(/[A-Z]/, 'Password must contain at least one uppercase letter')
  .regex(/[a-z]/, 'Password must contain at least one lowercase letter')
  .regex(/[0-9]/, 'Password must contain at least one digit');

// Email validation
const emailSchema = z.string().email('Invalid email address').toLowerCase();

// Auth schemas
export const RegisterSchema = z.object({
  email: emailSchema,
  password: passwordSchema,
});

export type RegisterInput = z.infer<typeof RegisterSchema>;

export const VerifyEmailSchema = z.object({
  email: emailSchema,
  code: z.string().length(6, 'Code must be 6 digits'),
});

export type VerifyEmailInput = z.infer<typeof VerifyEmailSchema>;

export const LoginSchema = z.object({
  email: emailSchema,
  password: z.string().min(1, 'Password required'),
});

export type LoginInput = z.infer<typeof LoginSchema>;

export const ForgotPasswordSchema = z.object({
  email: emailSchema,
});

export type ForgotPasswordInput = z.infer<typeof ForgotPasswordSchema>;

export const ResetPasswordSchema = z.object({
  token: z.string().min(1, 'Token required'),
  newPassword: passwordSchema,
});

export type ResetPasswordInput = z.infer<typeof ResetPasswordSchema>;

export const ChooseRoleSchema = z.object({
  role: z.enum(['usuario', 'chofer']),
});

export type ChooseRoleInput = z.infer<typeof ChooseRoleSchema>;

// Response schemas
export const UserResponseSchema = z.object({
  id: z.string().uuid(),
  email: z.string().email(),
  full_name: z.string().optional(),
  role: z.enum(['admin', 'chofer', 'usuario']),
  avatar_url: z.string().optional(),
  created_at: z.string(),
});

export type UserResponse = z.infer<typeof UserResponseSchema>;

export const AuthResponseSchema = z.object({
  user: UserResponseSchema,
  sessionId: z.string(),
});

export type AuthResponse = z.infer<typeof AuthResponseSchema>;

// FASE 3: Viajes (Trips)
export const CreateTripSchema = z.object({
  origen_lat: z.number().min(-90).max(90),
  origen_lng: z.number().min(-180).max(180),
  destino_lat: z.number().min(-90).max(90),
  destino_lng: z.number().min(-180).max(180),
  toneladas: z.number().min(0.1).optional(),
  tipo_tarifa: z.enum(['por_km', 'por_tonelada', 'mixta']).default('por_km'),
  carga: z.string().optional(),
});

export type CreateTripInput = z.infer<typeof CreateTripSchema>;

export const CancelTripSchema = z.object({
  razon: z.string().min(1, 'Reason required').max(255),
});

export type CancelTripInput = z.infer<typeof CancelTripSchema>;

export const GetAvailableTripsSchema = z.object({
  page: z.number().int().min(1).default(1),
  limit: z.number().int().min(1).max(50).default(10),
  estado: z.enum(['solicitado', 'aceptado', 'en_camino', 'completado', 'cancelado']).optional(),
});

export type GetAvailableTripsInput = z.infer<typeof GetAvailableTripsSchema>;

// FASE 3: Camiones (Trucks)
export const UpdateTruckLocationSchema = z.object({
  lat: z.number().min(-90).max(90),
  lng: z.number().min(-180).max(180),
});

export type UpdateTruckLocationInput = z.infer<typeof UpdateTruckLocationSchema>;

export const UpdateTruckSchema = z.object({
  patente: z.string().min(1).max(20).optional(),
  patente_acoplado: z.string().max(20).optional(),
  transporte_nombre: z.string().max(100).optional(),
  transporte_cuit: z.string().max(20).optional(),
  chofer_nombre: z.string().min(1).max(100).optional(),
  chofer_cuit: z.string().max(20).optional(),
  telefono: z.string().max(20).optional(),
  capacidad_kg: z.number().int().min(100).optional(),
  estado: z.enum(['disponible', 'ocupado', 'inactivo']).optional(),
});

export type UpdateTruckInput = z.infer<typeof UpdateTruckSchema>;

export const GetAvailableTrucksSchema = z.object({
  page: z.number().int().min(1).default(1),
  limit: z.number().int().min(1).max(50).default(10),
});

export type GetAvailableTrucksInput = z.infer<typeof GetAvailableTrucksSchema>;

// FASE 3: Admin
export const UpdateConfigSchema = z.object({
  zona_nombre: z.string().min(1).max(255).optional(),
  centro_lat: z.number().min(-90).max(90).optional(),
  centro_lng: z.number().min(-180).max(180).optional(),
  tarifa_por_km: z.number().min(0).optional(),
  tarifa_por_tonelada: z.number().min(0).optional(),
});

export type UpdateConfigInput = z.infer<typeof UpdateConfigSchema>;

export const GetAuditLogSchema = z.object({
  user_id: z.string().uuid().optional(),
  action: z.string().optional(),
  page: z.number().int().min(1).default(1),
  limit: z.number().int().min(1).max(100).default(25),
});

export type GetAuditLogInput = z.infer<typeof GetAuditLogSchema>;

// Response schemas
export const TripResponseSchema = z.object({
  id: z.string().uuid(),
  usuario_id: z.string().uuid(),
  chofer_id: z.string().uuid().optional(),
  camion_id: z.string().uuid().optional(),
  origen_lat: z.number(),
  origen_lng: z.number(),
  destino_lat: z.number(),
  destino_lng: z.number(),
  distancia_km: z.number(),
  toneladas: z.number().optional(),
  tipo_tarifa: z.string(),
  precio: z.number(),
  estado: z.string(),
  created_at: z.string(),
  updated_at: z.string(),
});

export type TripResponse = z.infer<typeof TripResponseSchema>;

export const TruckResponseSchema = z.object({
  id: z.string().uuid(),
  user_id: z.string().uuid().optional(),
  patente: z.string(),
  chofer_nombre: z.string(),
  capacidad_kg: z.number().optional(),
  lat: z.number(),
  lng: z.number(),
  estado: z.string(),
  created_at: z.string(),
});

export type TruckResponse = z.infer<typeof TruckResponseSchema>;
