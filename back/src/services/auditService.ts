import { query } from '../config/database';

export type AuditAction =
  | 'create'
  | 'update'
  | 'delete'
  | 'login'
  | 'logout'
  | 'password_change'
  | 'role_change';

export class AuditService {
  static async log(
    action: AuditAction,
    entityType: string,
    {
      userId,
      entityId,
      details,
      ipAddress,
      userAgent,
    }: {
      userId?: string;
      entityId?: string;
      details?: Record<string, any>;
      ipAddress?: string;
      userAgent?: string;
    }
  ): Promise<void> {
    try {
      await query(
        `INSERT INTO audit_log (user_id, action, entity_type, entity_id, details, ip_address, user_agent)
         VALUES ($1, $2, $3, $4, $5, $6, $7)`,
        [
          userId || null,
          action,
          entityType,
          entityId || null,
          details ? JSON.stringify(details) : null,
          ipAddress || null,
          userAgent || null,
        ]
      );
    } catch (error) {
      console.error('Audit logging error:', error);
    }
  }

  static async getLog(userId: string, limit: number = 50) {
    const result = await query(
      `SELECT id, action, entity_type, entity_id, details, created_at
       FROM audit_log
       WHERE user_id = $1
       ORDER BY created_at DESC
       LIMIT $2`,
      [userId, limit]
    );
    return result.rows;
  }
}
