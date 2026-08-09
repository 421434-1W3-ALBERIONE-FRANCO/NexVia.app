import { nanoid } from 'nanoid';
import { redis } from '../config/redis';
import { query } from '../config/database';
import { User } from '../types';

const SESSION_DURATION = 7 * 24 * 60 * 60; // 7 days in seconds
const SESSION_COOKIE_NAME = 'nexvia_session';

export class SessionService {
  static generateSessionId(): string {
    return nanoid(32);
  }

  static async createSession(
    userId: string,
    ipAddress?: string,
    userAgent?: string
  ): Promise<string> {
    const sessionId = this.generateSessionId();
    const expiresAt = new Date(Date.now() + SESSION_DURATION * 1000);

    // Store in Redis (primary)
    if (redis) {
      const sessionData = JSON.stringify({
        userId,
        createdAt: new Date().toISOString(),
        ipAddress,
        userAgent,
      });

      await redis.setex(
        `session:${sessionId}`,
        SESSION_DURATION,
        sessionData
      );
    }

    // Store in PostgreSQL (backup)
    await query(
      `INSERT INTO sessions (id, user_id, ip_address, user_agent, expires_at)
       VALUES ($1, $2, $3, $4, $5)`,
      [sessionId, userId, ipAddress || null, userAgent || null, expiresAt]
    );

    return sessionId;
  }

  static async getSession(
    sessionId: string
  ): Promise<{ userId: string; createdAt: string; ipAddress?: string } | null> {
    // Try Redis first
    if (redis) {
      try {
        const data = await redis.get(`session:${sessionId}`);
        if (data) {
          return JSON.parse(data);
        }
      } catch (error) {
        console.error('Redis session retrieval error:', error);
      }
    }

    // Fallback to PostgreSQL
    try {
      const result = await query(
        `SELECT user_id as "userId", created_at as "createdAt", ip_address as "ipAddress"
         FROM sessions
         WHERE id = $1 AND expires_at > NOW()`,
        [sessionId]
      );

      if (result.rows.length > 0) {
        return result.rows[0];
      }
    } catch (error) {
      console.error('PostgreSQL session retrieval error:', error);
    }

    return null;
  }

  static async destroySession(sessionId: string): Promise<void> {
    // Delete from Redis
    if (redis) {
      await redis.del(`session:${sessionId}`);
    }

    // Delete from PostgreSQL
    await query('DELETE FROM sessions WHERE id = $1', [sessionId]);
  }

  static async destroyAllUserSessions(userId: string): Promise<void> {
    // Delete from Redis
    if (redis) {
      const pattern = `session:*`;
      const keys = await redis.keys(pattern);
      for (const key of keys) {
        const data = await redis.get(key);
        if (data) {
          const session = JSON.parse(data);
          if (session.userId === userId) {
            await redis.del(key);
          }
        }
      }
    }

    // Delete from PostgreSQL
    await query('DELETE FROM sessions WHERE user_id = $1', [userId]);
  }

  static getCookieOptions(isDevelopment: boolean) {
    return {
      httpOnly: true,
      secure: !isDevelopment, // Secure in production
      sameSite: 'lax' as const,
      maxAge: SESSION_DURATION * 1000, // milliseconds
      path: '/',
    };
  }

  static getCookieName(): string {
    return SESSION_COOKIE_NAME;
  }
}
