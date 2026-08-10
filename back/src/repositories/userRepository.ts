import { query } from '../config/database';
import { User } from '../types';

export class UserRepository {
  static async findById(id: string): Promise<User | null> {
    const result = await query(
      `SELECT id, email, email_verified, full_name, phone, avatar_url, role,
              google_id, is_active, is_deleted, created_at, updated_at, last_login_at
       FROM users
       WHERE id = $1 AND is_deleted = FALSE`,
      [id]
    );
    return result.rows[0] || null;
  }

  static async findByEmail(email: string): Promise<User | null> {
    const result = await query(
      `SELECT id, email, email_verified, password_hash, full_name, phone, avatar_url, role,
              google_id, is_active, is_deleted, created_at, updated_at, last_login_at
       FROM users
       WHERE LOWER(email) = LOWER($1) AND is_deleted = FALSE`,
      [email]
    );
    return result.rows[0] || null;
  }

  static async findByGoogleId(googleId: string): Promise<User | null> {
    const result = await query(
      `SELECT id, email, email_verified, full_name, phone, avatar_url, role,
              google_id, is_active, is_deleted, created_at, updated_at, last_login_at
       FROM users
       WHERE google_id = $1 AND is_deleted = FALSE`,
      [googleId]
    );
    return result.rows[0] || null;
  }

  static async create(data: {
    email: string;
    password_hash?: string;
    google_id?: string;
    full_name?: string;
  }): Promise<User> {
    const result = await query(
      `INSERT INTO users (email, password_hash, google_id, full_name, role, email_verified, is_active)
       VALUES ($1, $2, $3, $4, $5, $6, $7)
       RETURNING id, email, email_verified, full_name, phone, avatar_url, role,
                 google_id, is_active, is_deleted, created_at, updated_at, last_login_at`,
      [
        data.email.toLowerCase(),
        data.password_hash || null,
        data.google_id || null,
        data.full_name || null,
        'usuario',
        data.google_id ? true : false, // Auto-verify Google OAuth users
        true,
      ]
    );
    return result.rows[0];
  }

  static async updateEmail(userId: string, emailVerified: boolean): Promise<void> {
    await query(
      `UPDATE users
       SET email_verified = $1, updated_at = NOW()
       WHERE id = $2`,
      [emailVerified, userId]
    );
  }

  static async updatePassword(userId: string, passwordHash: string): Promise<void> {
    await query(
      `UPDATE users
       SET password_hash = $1, updated_at = NOW()
       WHERE id = $2`,
      [passwordHash, userId]
    );
  }

  static async updateRole(userId: string, role: 'admin' | 'chofer' | 'usuario'): Promise<void> {
    await query(
      `UPDATE users
       SET role = $1, updated_at = NOW()
       WHERE id = $2`,
      [role, userId]
    );
  }

  static async updateLastLogin(userId: string): Promise<void> {
    await query(
      `UPDATE users
       SET last_login_at = NOW(), updated_at = NOW()
       WHERE id = $1`,
      [userId]
    );
  }

  static async updateProfile(
    userId: string,
    data: { full_name?: string; phone?: string; avatar_url?: string }
  ): Promise<void> {
    const updates: string[] = [];
    const params: any[] = [];
    let paramCount = 1;

    if (data.full_name !== undefined) {
      updates.push(`full_name = $${paramCount++}`);
      params.push(data.full_name);
    }
    if (data.phone !== undefined) {
      updates.push(`phone = $${paramCount++}`);
      params.push(data.phone);
    }
    if (data.avatar_url !== undefined) {
      updates.push(`avatar_url = $${paramCount++}`);
      params.push(data.avatar_url);
    }

    if (updates.length === 0) return;

    updates.push(`updated_at = NOW()`);
    params.push(userId);

    await query(
      `UPDATE users SET ${updates.join(', ')} WHERE id = $${paramCount}`,
      params
    );
  }

  static async linkGoogleId(userId: string, googleId: string): Promise<void> {
    await query(
      `UPDATE users
       SET google_id = $1, updated_at = NOW()
       WHERE id = $2`,
      [googleId, userId]
    );
  }

  static toResponse(user: User) {
    const { password_hash, ...safeUser } = user;
    return safeUser;
  }
}
