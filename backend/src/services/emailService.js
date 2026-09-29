const nodemailer = require('nodemailer');

/**
 * Email Service with console and SMTP modes.
 * 
 * EMAIL_MODE=console  → Logs OTP to console (for development/testing)
 * EMAIL_MODE=smtp     → Sends real emails via SMTP (for production)
 */
class EmailService {
  constructor() {
    this.mode = process.env.EMAIL_MODE || 'console';
    this.transporter = null;

    if (this.mode === 'smtp') {
      this._initSMTP();
    }

    console.log(`[EmailService] Initialized in ${this.mode.toUpperCase()} mode`);
  }

  _initSMTP() {
    try {
      this.transporter = nodemailer.createTransport({
        host: process.env.SMTP_HOST,
        port: parseInt(process.env.SMTP_PORT || '587'),
        secure: parseInt(process.env.SMTP_PORT || '587') === 465,
        auth: {
          user: process.env.SMTP_USER,
          pass: process.env.SMTP_PASSWORD
        }
      });
      console.log('[EmailService] SMTP transporter configured');
    } catch (err) {
      console.error('[EmailService] Failed to initialize SMTP transporter:', err.message);
    }
  }

  /**
   * Send a password reset OTP email.
   * @param {string} email - Recipient email address
   * @param {string} otp - The 6-digit OTP (plaintext, for display only)
   * @param {number} expiryMinutes - OTP expiry time in minutes
   */
  async sendPasswordResetOTP(email, otp, expiryMinutes = 10) {
    if (this.mode === 'console') {
      return this._logToConsole(email, otp, expiryMinutes);
    }

    return this._sendViaSMTP(email, otp, expiryMinutes);
  }

  _logToConsole(email, otp, expiryMinutes) {
    const expiresAt = new Date(Date.now() + expiryMinutes * 60 * 1000);
    console.log('\n==================================================');
    console.log('PASSWORD RESET OTP');
    console.log('==================================================');
    console.log(`Email: ${email}`);
    console.log(`OTP: ${otp}`);
    console.log(`Expires: ${expiresAt.toISOString()} (${expiryMinutes} minutes)`);
    console.log('==================================================\n');
    return { success: true, mode: 'console' };
  }

  async _sendViaSMTP(email, otp, expiryMinutes) {
    if (!this.transporter) {
      console.error('[EmailService] SMTP transporter not initialized');
      throw new Error('Email service not available');
    }

    const mailFrom = process.env.MAIL_FROM || process.env.SMTP_USER || 'noreply@echostream.com';

    const htmlContent = `
      <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%); border-radius: 12px;">
        <div style="text-align: center; padding: 20px;">
          <h1 style="color: #8A2BE2; font-size: 28px; margin-bottom: 5px;">🎵 EchoStream</h1>
          <p style="color: #B0B0C0; font-size: 14px;">Your Music, Your Way</p>
        </div>
        <div style="background: #222228; border-radius: 8px; padding: 30px; margin: 20px 0;">
          <h2 style="color: #FFFFFF; font-size: 20px; margin-bottom: 15px;">Password Reset OTP</h2>
          <p style="color: #B0B0C0; font-size: 14px; line-height: 1.6;">
            You requested a password reset for your EchoStream account. Use the OTP below to verify your identity:
          </p>
          <div style="text-align: center; margin: 25px 0;">
            <div style="display: inline-block; background: linear-gradient(135deg, #8A2BE2, #FF007F); padding: 15px 40px; border-radius: 8px; letter-spacing: 8px; font-size: 32px; font-weight: bold; color: #FFFFFF;">
              ${otp}
            </div>
          </div>
          <p style="color: #FF4D4D; font-size: 13px; text-align: center;">
            ⏰ This OTP expires in ${expiryMinutes} minutes.
          </p>
        </div>
        <div style="text-align: center; padding: 15px;">
          <p style="color: #6E6E7E; font-size: 12px; line-height: 1.6;">
            If you did not request a password reset, you can safely ignore this email.<br>
            Your password will remain unchanged.
          </p>
        </div>
      </div>
    `;

    const textContent = `Your EchoStream Password Reset OTP

Your password reset OTP is:

${otp}

This OTP expires in ${expiryMinutes} minutes.

If you did not request a password reset, you can ignore this email.`;

    try {
      const info = await this.transporter.sendMail({
        from: `"EchoStream Music" <${mailFrom}>`,
        to: email,
        subject: 'Your EchoStream Password Reset OTP',
        text: textContent,
        html: htmlContent
      });

      console.log(`[EmailService] OTP email sent to ${email}, messageId: ${info.messageId}`);
      return { success: true, mode: 'smtp', messageId: info.messageId };
    } catch (err) {
      console.error('[EmailService] Failed to send OTP email:', err.message);
      throw new Error('Failed to send OTP email');
    }
  }
}

// Singleton
let emailServiceInstance = null;

function getEmailService() {
  if (!emailServiceInstance) {
    emailServiceInstance = new EmailService();
  }
  return emailServiceInstance;
}

module.exports = { getEmailService };
