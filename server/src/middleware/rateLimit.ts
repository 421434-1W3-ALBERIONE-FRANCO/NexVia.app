import { Request, Response, NextFunction } from 'express';
import { config } from '../config/env';
import { redis } from '../config/redis';

interface RateLimitOptions {
  windowMs?: number;
  maxRequests?: number;
  keyPrefix?: string;
}

export function rateLimit(options: RateLimitOptions = {}) {
  const windowMs = options.windowMs || config.RATE_LIMIT_WINDOW_MS;
  const maxRequests = options.maxRequests || config.RATE_LIMIT_MAX_REQUESTS;
  const keyPrefix = options.keyPrefix || 'ratelimit:';

  return async (req: Request, res: Response, next: NextFunction) => {
    if (!redis) {
      return next();
    }

    const key = `${keyPrefix}${req.ip}`;

    try {
      const current = await redis.incr(key);

      if (current === 1) {
        await redis.expire(key, Math.ceil(windowMs / 1000));
      }

      res.setHeader('X-RateLimit-Limit', maxRequests);
      res.setHeader('X-RateLimit-Remaining', Math.max(0, maxRequests - current));

      if (current > maxRequests) {
        return res.status(429).json({
          error: {
            message: 'Too many requests',
            code: 'RATE_LIMIT_EXCEEDED',
            statusCode: 429,
            retryAfter: windowMs / 1000,
          },
        });
      }

      next();
    } catch (error) {
      console.error('Rate limit error:', error);
      next();
    }
  };
}

// Specialized rate limiters
export const loginRateLimit = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  maxRequests: 5,
  keyPrefix: 'ratelimit:login:',
});

export const registerRateLimit = rateLimit({
  windowMs: 60 * 60 * 1000, // 1 hour
  maxRequests: 3,
  keyPrefix: 'ratelimit:register:',
});

export const passwordResetRateLimit = rateLimit({
  windowMs: 60 * 60 * 1000, // 1 hour
  maxRequests: 3,
  keyPrefix: 'ratelimit:password_reset:',
});

export const apiRateLimit = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  maxRequests: 100,
  keyPrefix: 'ratelimit:api:',
});
