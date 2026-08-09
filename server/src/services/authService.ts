import { UserRepository } from '../repositories/userRepository';
import { EmailVerificationTokenRepository, PasswordResetTokenRepository } from '../repositories/tokenRepository';
import { PasswordService } from './passwordService';
import { TokenService } from './tokenService';
import { EmailService } from './emailService';
import { AuditService } from './auditService';
import { SessionService } from './sessionService';
import { AppError } from '../middleware/errorHandler';
import { config } from '../config/env';
import { User } from '../types';

export class AuthService {
  // REGISTRO
  static async register(email: string, password: string): Promise<User> {
    // Validate email format
    if (!email || !email.includes('@')) {
      throw new AppError(400, 'Invalid email address', 'INVALID_EMAIL');
    }

    // Check if user exists
    const existingUser = await UserRepository.findByEmail(email);
    if (existingUser) {
      throw new AppError(409, 'Email already registered', 'EMAIL_EXISTS');
    }

    // Validate password policy
    const isValidPassword = await PasswordService.isValidPolicy(password);
    if (!isValidPassword) {
      throw new AppError(
        400,
        'Password must be 8+ chars with uppercase, lowercase, and digit',
        'WEAK_PASSWORD'
      );
    }

    // Hash password
    const passwordHash = await PasswordService.hash(password);

    // Create user
    const user = await UserRepository.create({
      email,
      password_hash: passwordHash,
    });

    // Generate OTP
    const otp = TokenService.generateOTP();
    const otpHash = TokenService.hashOTP(otp);
    const expiresAt = new Date(Date.now() + 15 * 60 * 1000); // 15 minutes

    await EmailVerificationTokenRepository.create(user.id, otpHash, expiresAt);

    // Send OTP email
    await EmailService.sendOTP(email, otp);

    // Log registration
    await AuditService.log('create', 'user', {
      userId: user.id,
      entityId: user.id,
      details: { email },
    });

    return user;
  }

  // VERIFICAR EMAIL (OTP)
  static async verifyEmail(email: string, code: string): Promise<{ user: User; sessionId: string }> {
    const user = await UserRepository.findByEmail(email);
    if (!user) {
      throw new AppError(401, 'Invalid credentials', 'INVALID_CREDENTIALS');
    }

    if (!user.is_active) {
      throw new AppError(403, 'Account is inactive', 'ACCOUNT_INACTIVE');
    }

    const token = await EmailVerificationTokenRepository.findValid(user.id);
    if (!token) {
      throw new AppError(401, 'No valid verification token found', 'NO_VALID_TOKEN');
    }

    // Check attempts
    if (token.attempts >= 5) {
      throw new AppError(429, 'Too many verification attempts', 'TOO_MANY_ATTEMPTS');
    }

    // Verify code
    const isValid = TokenService.verifyOTP(code, token.id);
    if (!isValid) {
      await EmailVerificationTokenRepository.incrementAttempts(token.id);
      throw new AppError(401, 'Invalid verification code', 'INVALID_CODE');
    }

    // Mark token as used
    await EmailVerificationTokenRepository.markAsUsed(token.id);

    // Update user email_verified
    await UserRepository.updateEmail(user.id, true);

    // Create session
    const sessionId = await SessionService.createSession(user.id);

    // Update last login
    await UserRepository.updateLastLogin(user.id);

    // Log login
    await AuditService.log('login', 'user', {
      userId: user.id,
      entityId: user.id,
    });

    return { user, sessionId };
  }

  // LOGIN
  static async login(email: string, password: string): Promise<{ user: User; sessionId: string }> {
    const user = await UserRepository.findByEmail(email);

    if (!user || !user.password_hash) {
      throw new AppError(401, 'Invalid credentials', 'INVALID_CREDENTIALS');
    }

    if (!user.is_active) {
      throw new AppError(403, 'Account is inactive', 'ACCOUNT_INACTIVE');
    }

    if (!user.email_verified) {
      throw new AppError(403, 'Email not verified', 'EMAIL_NOT_VERIFIED');
    }

    // Compare password
    const passwordMatches = await PasswordService.compare(password, user.password_hash);
    if (!passwordMatches) {
      throw new AppError(401, 'Invalid credentials', 'INVALID_CREDENTIALS');
    }

    // Create session
    const sessionId = await SessionService.createSession(user.id);

    // Update last login
    await UserRepository.updateLastLogin(user.id);

    // Log login
    await AuditService.log('login', 'user', {
      userId: user.id,
      entityId: user.id,
    });

    return { user, sessionId };
  }

  // PASSWORD RESET REQUEST
  static async forgotPassword(email: string): Promise<{ token: string }> {
    const user = await UserRepository.findByEmail(email);

    const token = TokenService.generateToken();
    const tokenHash = TokenService.hashToken(token);
    const expiresAt = new Date(Date.now() + 60 * 60 * 1000); // 1 hour

    if (user) {
      await PasswordResetTokenRepository.create(user.id, tokenHash, expiresAt);

      const resetLink = `${config.FRONTEND_URL}/reset-password?token=${token}`;
      await EmailService.sendPasswordReset(email, resetLink);

      await AuditService.log('password_change', 'user', {
        userId: user.id,
        entityId: user.id,
        details: { action: 'password_reset_requested' },
      });
    }

    // Always return generic response (anti-enumeration)
    return { token: 'password_reset_email_sent' };
  }

  // RESET PASSWORD
  static async resetPassword(token: string, newPassword: string): Promise<User> {
    const tokenHash = TokenService.hashToken(token);
    const tokenRecord = await PasswordResetTokenRepository.findValid(tokenHash);

    if (!tokenRecord) {
      throw new AppError(401, 'Invalid or expired reset token', 'INVALID_TOKEN');
    }

    // Validate password
    const isValidPassword = await PasswordService.isValidPolicy(newPassword);
    if (!isValidPassword) {
      throw new AppError(
        400,
        'Password must be 8+ chars with uppercase, lowercase, and digit',
        'WEAK_PASSWORD'
      );
    }

    // Hash new password
    const passwordHash = await PasswordService.hash(newPassword);

    // Update password
    await UserRepository.updatePassword(tokenRecord.user_id, passwordHash);

    // Mark token as used
    await PasswordResetTokenRepository.markAsUsed(tokenRecord.id);

    // Invalidate all sessions (force re-login)
    await SessionService.destroyAllUserSessions(tokenRecord.user_id);

    // Fetch updated user
    const user = await UserRepository.findById(tokenRecord.user_id);
    if (!user) {
      throw new AppError(500, 'User not found after password reset', 'USER_NOT_FOUND');
    }

    // Log password change
    await AuditService.log('password_change', 'user', {
      userId: user.id,
      entityId: user.id,
    });

    return user;
  }

  // CHOOSE ROLE (Self-assign)
  static async chooseRole(userId: string, role: 'usuario' | 'chofer'): Promise<User> {
    const user = await UserRepository.findById(userId);
    if (!user) {
      throw new AppError(404, 'User not found', 'USER_NOT_FOUND');
    }

    // Only allow role assignment if not already completed (role is still 'usuario')
    if (user.role !== 'usuario') {
      throw new AppError(400, 'Role already assigned', 'ROLE_ALREADY_ASSIGNED');
    }

    // Validate role (only usuario or chofer allowed via self-assign)
    if (!['usuario', 'chofer'].includes(role)) {
      throw new AppError(400, 'Invalid role', 'INVALID_ROLE');
    }

    // Update role
    await UserRepository.updateRole(userId, role);

    // Log role change
    await AuditService.log('role_change', 'user', {
      userId,
      entityId: userId,
      details: { from: 'usuario', to: role },
    });

    // Fetch updated user
    const updatedUser = await UserRepository.findById(userId);
    if (!updatedUser) {
      throw new AppError(500, 'User not found after role change', 'USER_NOT_FOUND');
    }

    return updatedUser;
  }

  // GOOGLE OAUTH
  static async handleGoogleOAuth(googleId: string, email: string, fullName?: string): Promise<{ user: User; sessionId: string; isNewUser: boolean }> {
    // Try to find existing user by Google ID
    let user = await UserRepository.findByGoogleId(googleId);

    if (!user) {
      // Try to find by email
      user = await UserRepository.findByEmail(email);

      if (user) {
        // Link Google ID to existing user
        await UserRepository.linkGoogleId(user.id, googleId);
      } else {
        // Create new user
        user = await UserRepository.create({
          email,
          google_id: googleId,
          full_name: fullName,
        });

        // Log new user creation
        await AuditService.log('create', 'user', {
          userId: user.id,
          entityId: user.id,
          details: { email, via: 'google_oauth' },
        });
      }
    }

    if (!user.is_active) {
      throw new AppError(403, 'Account is inactive', 'ACCOUNT_INACTIVE');
    }

    // Create session
    const sessionId = await SessionService.createSession(user.id);

    // Update last login
    await UserRepository.updateLastLogin(user.id);

    // Log login
    await AuditService.log('login', 'user', {
      userId: user.id,
      entityId: user.id,
      details: { via: 'google_oauth' },
    });

    return { user, sessionId, isNewUser: !user.password_hash };
  }

  // GET CURRENT USER
  static async getCurrentUser(userId: string): Promise<User | null> {
    return UserRepository.findById(userId);
  }

  // LOGOUT
  static async logout(sessionId: string, userId: string): Promise<void> {
    await SessionService.destroySession(sessionId);

    await AuditService.log('logout', 'user', {
      userId,
      entityId: userId,
    });
  }
}
