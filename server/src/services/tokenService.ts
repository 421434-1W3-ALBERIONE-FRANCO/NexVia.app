import { nanoid } from 'nanoid';
import crypto from 'crypto';

export class TokenService {
  static generateOTP(): string {
    return Math.floor(100000 + Math.random() * 900000).toString();
  }

  static hashOTP(otp: string): string {
    return crypto.createHash('sha256').update(otp).digest('hex');
  }

  static generateToken(length: number = 48): string {
    return nanoid(length);
  }

  static hashToken(token: string): string {
    return crypto.createHash('sha256').update(token).digest('hex');
  }

  static verifyOTP(provided: string, stored: string): boolean {
    return this.hashOTP(provided) === stored;
  }

  static verifyToken(provided: string, stored: string): boolean {
    return this.hashToken(provided) === stored;
  }
}
