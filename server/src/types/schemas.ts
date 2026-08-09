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
