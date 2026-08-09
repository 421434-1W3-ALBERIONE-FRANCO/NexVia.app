import { query } from '../config/database';

export class EmailVerificationTokenRepository {
  static async create(userId: string, codeHash: string, expiresAt: Date): Promise<string> {
    const result = await query(
      `INSERT INTO email_verification_tokens (user_id, code_hash, expires_at)
       VALUES ($1, $2, $3)
       RETURNING id`,
      [userId, codeHash, expiresAt]
    );
    return result.rows[0].id;
  }

  static async findValid(userId: string): Promise<{ id: string; attempts: number } | null> {
    const result = await query(
      `SELECT id, attempts
       FROM email_verification_tokens
       WHERE user_id = $1 AND is_used = FALSE AND expires_at > NOW()
       ORDER BY created_at DESC
       LIMIT 1`,
      [userId]
    );
    return result.rows[0] || null;
  }

  static async incrementAttempts(tokenId: string): Promise<void> {
    await query(
      `UPDATE email_verification_tokens
       SET attempts = attempts + 1
       WHERE id = $1`,
      [tokenId]
    );
  }

  static async markAsUsed(tokenId: string): Promise<void> {
    await query(
      `UPDATE email_verification_tokens
       SET is_used = TRUE
       WHERE id = $1`,
      [tokenId]
    );
  }

  static async invalidateAllPending(userId: string): Promise<void> {
    await query(
      `UPDATE email_verification_tokens
       SET is_used = TRUE
       WHERE user_id = $1 AND is_used = FALSE`,
      [userId]
    );
  }
}

export class PasswordResetTokenRepository {
  static async create(userId: string, tokenHash: string, expiresAt: Date): Promise<void> {
    // Invalidate previous tokens first
    await query(
      `UPDATE password_reset_tokens
       SET is_used = TRUE
       WHERE user_id = $1 AND is_used = FALSE`,
      [userId]
    );

    // Create new token
    await query(
      `INSERT INTO password_reset_tokens (user_id, token_hash, expires_at)
       VALUES ($1, $2, $3)`,
      [userId, tokenHash, expiresAt]
    );
  }

  static async findValid(tokenHash: string): Promise<{ user_id: string; id: string } | null> {
    const result = await query(
      `SELECT user_id, id
       FROM password_reset_tokens
       WHERE token_hash = $1 AND is_used = FALSE AND expires_at > NOW()
       LIMIT 1`,
      [tokenHash]
    );
    return result.rows[0] || null;
  }

  static async markAsUsed(tokenId: string): Promise<void> {
    await query(
      `UPDATE password_reset_tokens
       SET is_used = TRUE
       WHERE id = $1`,
      [tokenId]
    );
  }

  static async invalidateAllForUser(userId: string): Promise<void> {
    await query(
      `UPDATE password_reset_tokens
       SET is_used = TRUE
       WHERE user_id = $1 AND is_used = FALSE`,
      [userId]
    );
  }
}
