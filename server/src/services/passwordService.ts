import bcrypt from 'bcrypt';

const COST_FACTOR = 12;

export class PasswordService {
  static async hash(password: string): Promise<string> {
    return bcrypt.hash(password, COST_FACTOR);
  }

  static async compare(password: string, hash: string): Promise<boolean> {
    return bcrypt.compare(password, hash);
  }

  static async isValidPolicy(password: string): Promise<boolean> {
    const hasMinLength = password.length >= 8;
    const hasUppercase = /[A-Z]/.test(password);
    const hasLowercase = /[a-z]/.test(password);
    const hasDigit = /[0-9]/.test(password);

    return hasMinLength && hasUppercase && hasLowercase && hasDigit;
  }
}
