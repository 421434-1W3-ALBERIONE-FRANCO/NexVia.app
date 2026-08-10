import { query } from '../config/database';
import { ConfigRepository, Config } from '../repositories/configRepository';
import { UserRepository } from '../repositories/userRepository';
import { AuditService } from './auditService';
import { AppError } from '../middleware/errorHandler';

export class AdminService {
  static async listUsers(page: number = 1, limit: number = 25): Promise<{ users: any[]; total: number }> {
    const offset = (page - 1) * limit;

    const usersResult = await query(
      `SELECT id, email, full_name, role, created_at, is_active, last_login_at
       FROM users WHERE is_deleted = FALSE
       ORDER BY created_at DESC LIMIT $1 OFFSET $2`,
      [limit, offset]
    );

    const totalResult = await query('SELECT COUNT(*) as count FROM users WHERE is_deleted = FALSE');
    const total = parseInt(totalResult.rows[0].count);

    return { users: usersResult.rows, total };
  }

  static async getUserDetails(userId: string): Promise<any> {
    const user = await UserRepository.findById(userId);
    if (!user) {
      throw new AppError(404, 'User not found', 'USER_NOT_FOUND');
    }

    // Get user's trucks and trips
    const trucksResult = await query(
      'SELECT COUNT(*) as count FROM camiones WHERE user_id = $1 AND is_deleted = FALSE',
      [userId]
    );

    const tripsResult = await query(
      'SELECT COUNT(*) as count FROM viajes WHERE usuario_id = $1',
      [userId]
    );

    const auditResult = await query(
      'SELECT COUNT(*) as count FROM audit_log WHERE user_id = $1',
      [userId]
    );

    return {
      id: user.id,
      email: user.email,
      full_name: user.full_name,
      phone: user.phone,
      role: user.role,
      avatar_url: user.avatar_url,
      is_active: user.is_active,
      created_at: user.created_at,
      updated_at: user.updated_at,
      last_login_at: user.last_login_at,
      stats: {
        trucks: parseInt(trucksResult.rows[0].count),
        trips: parseInt(tripsResult.rows[0].count),
        audit_entries: parseInt(auditResult.rows[0].count),
      },
    };
  }

  static async updateConfiguration(data: Partial<Config>, adminId: string): Promise<Config> {
    const config = await ConfigRepository.update(data);

    await AuditService.log('update', 'configuracion', {
      userId: adminId,
      details: {
        updated_fields: Object.keys(data),
      },
    });

    return config;
  }

  static async getAuditLog(
    options?: { user_id?: string; action?: string; page?: number; limit?: number }
  ): Promise<{ logs: any[]; total: number }> {
    const page = options?.page || 1;
    const limit = options?.limit || 25;
    const offset = (page - 1) * limit;

    let whereClause = '1=1';
    const params: any[] = [];
    let paramCount = 1;

    if (options?.user_id) {
      whereClause += ` AND user_id = $${paramCount++}`;
      params.push(options.user_id);
    }

    if (options?.action) {
      whereClause += ` AND action = $${paramCount++}`;
      params.push(options.action);
    }

    const logsResult = await query(
      `SELECT id, user_id, action, entity_type, entity_id, details, ip_address, user_agent, created_at
       FROM audit_log WHERE ${whereClause}
       ORDER BY created_at DESC LIMIT $${paramCount + 1} OFFSET $${paramCount + 2}`,
      [...params, limit, offset]
    );

    const countResult = await query(
      `SELECT COUNT(*) as count FROM audit_log WHERE ${whereClause}`,
      params
    );

    const total = parseInt(countResult.rows[0].count);

    return { logs: logsResult.rows, total };
  }
}
