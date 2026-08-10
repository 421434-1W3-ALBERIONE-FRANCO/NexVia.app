import { Request, Response, NextFunction } from 'express';
import { SessionService } from '../services/sessionService';
import { UserRepository } from '../repositories/userRepository';
import { AppError } from './errorHandler';

export async function authMiddleware(
  req: Request,
  res: Response,
  next: NextFunction
) {
  try {
    const sessionId = req.cookies?.[SessionService.getCookieName()];

    if (!sessionId) {
      return next(new AppError(401, 'Not authenticated', 'NOT_AUTHENTICATED'));
    }

    // Get session
    const session = await SessionService.getSession(sessionId);

    if (!session) {
      return next(new AppError(401, 'Session expired or invalid', 'SESSION_INVALID'));
    }

    // Get user
    const user = await UserRepository.findById(session.userId);

    if (!user || !user.is_active) {
      return next(new AppError(401, 'User not found or inactive', 'USER_INACTIVE'));
    }

    // Attach to request
    req.user = user;
    req.sessionId = sessionId;

    next();
  } catch (error) {
    next(error);
  }
}

export function requireAuth(req: Request, res: Response, next: NextFunction) {
  if (!req.user) {
    return next(new AppError(401, 'Authentication required', 'AUTH_REQUIRED'));
  }
  next();
}

export function requireRole(...roles: string[]) {
  return (req: Request, res: Response, next: NextFunction) => {
    if (!req.user) {
      return next(new AppError(401, 'Authentication required', 'AUTH_REQUIRED'));
    }

    if (!roles.includes(req.user.role)) {
      return next(
        new AppError(403, 'Insufficient permissions', 'FORBIDDEN')
      );
    }

    next();
  };
}
