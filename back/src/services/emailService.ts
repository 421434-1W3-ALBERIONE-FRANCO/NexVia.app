import nodemailer from 'nodemailer';
import { config } from '../config/env';

const transporter = nodemailer.createTransport({
  host: config.SMTP_HOST,
  port: config.SMTP_PORT,
  secure: config.SMTP_PORT === 465,
  auth: {
    user: config.SMTP_USER,
    pass: config.SMTP_PASS,
  },
});

export class EmailService {
  static async sendOTP(email: string, otp: string): Promise<boolean> {
    // Dev convenience: surface the code in the server console when there is no
    // real SMTP configured, so local testing doesn't depend on receiving email.
    if (config.NODE_ENV !== 'production') {
      console.log(`\n📧 [DEV] OTP for ${email}: ${otp}\n`);
    }
    try {
      await transporter.sendMail({
        from: config.SMTP_FROM,
        to: email,
        subject: 'NEXVIA — Verification Code',
        html: this.getOTPEmailTemplate(otp),
      });
      return true;
    } catch (error) {
      console.error('Error sending OTP email:', error);
      return false;
    }
  }

  static async sendPasswordReset(email: string, resetLink: string): Promise<boolean> {
    try {
      await transporter.sendMail({
        from: config.SMTP_FROM,
        to: email,
        subject: 'NEXVIA — Password Reset',
        html: this.getPasswordResetEmailTemplate(resetLink),
      });
      return true;
    } catch (error) {
      console.error('Error sending password reset email:', error);
      return false;
    }
  }

  private static getOTPEmailTemplate(otp: string): string {
    return `
      <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; max-width: 600px; margin: 0 auto;">
        <h2 style="color: #0b3d2e;">Verify Your Email</h2>
        <p>Your verification code is:</p>
        <div style="background: #f3f4f6; padding: 20px; border-radius: 8px; text-align: center; margin: 20px 0;">
          <code style="font-size: 32px; font-weight: bold; letter-spacing: 4px; color: #0b3d2e;">${otp}</code>
        </div>
        <p>This code expires in 15 minutes.</p>
        <p style="color: #6b6a65; font-size: 12px;">If you didn't request this code, please ignore this email.</p>
      </div>
    `;
  }

  private static getPasswordResetEmailTemplate(resetLink: string): string {
    return `
      <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; max-width: 600px; margin: 0 auto;">
        <h2 style="color: #0b3d2e;">Reset Your Password</h2>
        <p>Click the link below to reset your password:</p>
        <a href="${resetLink}" style="display: inline-block; background: #0b3d2e; color: white; padding: 12px 24px; border-radius: 6px; text-decoration: none; margin: 20px 0;">
          Reset Password
        </a>
        <p>Or copy this link: <code>${resetLink}</code></p>
        <p>This link expires in 1 hour.</p>
        <p style="color: #6b6a65; font-size: 12px;">If you didn't request this, please ignore this email.</p>
      </div>
    `;
  }
}
