import { Router, Request, Response } from 'express';
import { z } from 'zod';
import { AuthService } from '../services/authService';
import { SessionService } from '../services/sessionService';
import { UserRepository } from '../repositories/userRepository';
import { asyncHandler, AppError } from '../middleware/errorHandler';
import { authMiddleware, requireAuth, requireRole } from '../middleware/auth';
import { loginRateLimit, registerRateLimit, passwordResetRateLimit } from '../middleware/rateLimit';
import {
  RegisterSchema,
  VerifyEmailSchema,
  LoginSchema,
  ForgotPasswordSchema,
  ResetPasswordSchema,
  ChooseRoleSchema,
} from '../types/schemas';
import { config } from '../config/env';

const router = Router();

// REGISTER
router.post(
  '/register',
  registerRateLimit,
  asyncHandler(async (req: Request, res: Response) => {
    const validation = RegisterSchema.safeParse(req.body);
    if (!validation.success) {
      throw new AppError(400, 'Validation failed', 'VALIDATION_ERROR');
    }

    const { email, password } = validation.data;
    const user = await AuthService.register(email, password);

    res.json({
      message: 'Registration successful. Please check your email for verification code.',
      user: UserRepository.toResponse(user),
    });
  })
);

// VERIFY EMAIL (OTP)
router.post(
  '/verify-email',
  asyncHandler(async (req: Request, res: Response) => {
    const validation = VerifyEmailSchema.safeParse(req.body);
    if (!validation.success) {
      throw new AppError(400, 'Validation failed', 'VALIDATION_ERROR');
    }

    const { email, code } = validation.data;
    const { user, sessionId } = await AuthService.verifyEmail(email, code);

    const cookieOptions = SessionService.getCookieOptions(config.NODE_ENV === 'development');
    res.cookie(SessionService.getCookieName(), sessionId, cookieOptions);

    res.json({
      message: 'Email verified successfully',
      user: UserRepository.toResponse(user),
    });
  })
);

// LOGIN
router.post(
  '/login',
  loginRateLimit,
  asyncHandler(async (req: Request, res: Response) => {
    const validation = LoginSchema.safeParse(req.body);
    if (!validation.success) {
      throw new AppError(400, 'Validation failed', 'VALIDATION_ERROR');
    }

    const { email, password } = validation.data;
    const { user, sessionId } = await AuthService.login(email, password);

    const cookieOptions = SessionService.getCookieOptions(config.NODE_ENV === 'development');
    res.cookie(SessionService.getCookieName(), sessionId, cookieOptions);

    res.json({
      message: 'Login successful',
      user: UserRepository.toResponse(user),
    });
  })
);

// GET CURRENT USER
router.get(
  '/me',
  authMiddleware,
  asyncHandler(async (req: Request, res: Response) => {
    const user = await AuthService.getCurrentUser(req.user!.id);

    if (!user) {
      throw new AppError(404, 'User not found', 'USER_NOT_FOUND');
    }

    res.json({
      user: UserRepository.toResponse(user),
    });
  })
);

// LOGOUT
router.post(
  '/logout',
  authMiddleware,
  asyncHandler(async (req: Request, res: Response) => {
    await AuthService.logout(req.sessionId!, req.user!.id);

    res.clearCookie(SessionService.getCookieName());

    res.json({
      message: 'Logout successful',
    });
  })
);

// FORGOT PASSWORD
router.post(
  '/forgot-password',
  passwordResetRateLimit,
  asyncHandler(async (req: Request, res: Response) => {
    const validation = ForgotPasswordSchema.safeParse(req.body);
    if (!validation.success) {
      throw new AppError(400, 'Validation failed', 'VALIDATION_ERROR');
    }

    const { email } = validation.data;
    await AuthService.forgotPassword(email);

    // Always return generic response (anti-enumeration)
    res.json({
      message: 'If an account exists for this email, a password reset link has been sent.',
    });
  })
);

// RESET PASSWORD
router.post(
  '/reset-password',
  asyncHandler(async (req: Request, res: Response) => {
    const validation = ResetPasswordSchema.safeParse(req.body);
    if (!validation.success) {
      throw new AppError(400, 'Validation failed', 'VALIDATION_ERROR');
    }

    const { token, newPassword } = validation.data;
    const user = await AuthService.resetPassword(token, newPassword);

    res.json({
      message: 'Password reset successful. Please login with your new password.',
      user: UserRepository.toResponse(user),
    });
  })
);

// CHOOSE ROLE (Self-assign)
router.post(
  '/choose-role',
  requireAuth,
  asyncHandler(async (req: Request, res: Response) => {
    const validation = ChooseRoleSchema.safeParse(req.body);
    if (!validation.success) {
      throw new AppError(400, 'Validation failed', 'VALIDATION_ERROR');
    }

    const { role } = validation.data;
    const user = await AuthService.chooseRole(req.user!.id, role);

    res.json({
      message: 'Role assigned successfully',
      user: UserRepository.toResponse(user),
    });
  })
);

// RESEND OTP (simplified for now)
router.post(
  '/resend-otp',
  registerRateLimit,
  asyncHandler(async (req: Request, res: Response) => {
    const { email } = req.body;

    if (!email) {
      throw new AppError(400, 'Email is required', 'EMAIL_REQUIRED');
    }

    // TODO: Implement OTP resend logic
    res.json({
      message: 'OTP resent to your email',
    });
  })
);

export default router;
