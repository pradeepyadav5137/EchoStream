const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const mongoose = require('mongoose');
const User = require('../models/User');
const store = require('../store');
const { requireAuth } = require('../middleware/auth');
const { getEmailService } = require('../services/emailService');
const rateLimit = require('express-rate-limit');

const router = express.Router();
const JWT_SECRET = process.env.JWT_SECRET || 'echostream_super_secret_jwt_key_2025_safe_and_long';

const OTP_EXPIRY_MINUTES = 10;
const MAX_OTP_ATTEMPTS = 3;
const MAX_OTP_REQUESTS_PER_HOUR = 3;

// Rate limiter for OTP requests: max 5 per 15 minutes per IP
const otpRequestLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 5,
  message: { success: false, message: 'Too many requests. Please try again later.' }
});

// Helper: validate email format
function isValidEmail(email) {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
}

// Helper: generate cryptographically secure 6-digit OTP
function generateOTP() {
  const buffer = crypto.randomBytes(4);
  const num = buffer.readUInt32BE(0);
  const otp = (num % 900000 + 100000).toString();
  return otp;
}

// Helper: hash OTP using SHA-256
function hashOTP(otp) {
  return crypto.createHash('sha256').update(otp).digest('hex');
}

// Register
router.post('/register', async (req, res) => {
  try {
    const { username, email, password, displayName } = req.body;
    if (!username || !email || !password) {
      return res.status(400).json({ success: false, error: 'Username, email and password are required' });
    }

    if (!isValidEmail(email)) {
      return res.status(400).json({ success: false, error: 'Invalid email format' });
    }

    if (password.length < 6) {
      return res.status(400).json({ success: false, error: 'Password must be at least 6 characters' });
    }

    const hashedPassword = await bcrypt.hash(password, 12);

    let userObj;
    if (mongoose.connection.readyState === 1) {
      const existingUser = await User.findOne({ $or: [{ email: email.toLowerCase() }, { username }] });
      if (existingUser) {
        return res.status(400).json({ success: false, error: 'Username or email already exists' });
      }
      const newUser = new User({
        username,
        email: email.toLowerCase(),
        password: hashedPassword,
        displayName: displayName || username,
        avatarUrl: `https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500`
      });
      await newUser.save();
      userObj = { id: newUser._id.toString(), username: newUser.username, email: newUser.email, displayName: newUser.displayName, avatarUrl: newUser.avatarUrl };
    } else {
      // In-memory fallback
      const existing = store.users.find(u => u.email === email.toLowerCase() || u.username === username);
      if (existing) {
        return res.status(400).json({ success: false, error: 'Username or email already exists' });
      }
      userObj = {
        id: 'usr_' + Date.now(),
        username,
        email: email.toLowerCase(),
        password: hashedPassword,
        displayName: displayName || username,
        avatarUrl: `https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500`
      };
      store.users.push(userObj);
    }

    const token = jwt.sign({ id: userObj.id, username: userObj.username, email: userObj.email }, JWT_SECRET, { expiresIn: '30d' });

    res.status(201).json({
      success: true,
      message: 'User registered successfully',
      token,
      user: {
        id: userObj.id,
        username: userObj.username,
        email: userObj.email,
        displayName: userObj.displayName,
        avatarUrl: userObj.avatarUrl
      }
    });
  } catch (err) {
    console.error('Register error:', err);
    res.status(500).json({ success: false, error: 'Internal server error' });
  }
});

// Login
router.post('/login', async (req, res) => {
  try {
    const { email, password } = req.body;
    if (!email || !password) {
      return res.status(400).json({ success: false, error: 'Email and password are required' });
    }

    if (!isValidEmail(email)) {
      return res.status(400).json({ success: false, error: 'Invalid email format' });
    }

    let userObj;
    if (mongoose.connection.readyState === 1) {
      userObj = await User.findOne({ email: email.toLowerCase() });
      if (!userObj) {
        return res.status(401).json({ success: false, error: 'Invalid email or password' });
      }
      const match = await bcrypt.compare(password, userObj.password);
      if (!match) {
        return res.status(401).json({ success: false, error: 'Invalid email or password' });
      }
      userObj = { id: userObj._id.toString(), username: userObj.username, email: userObj.email, displayName: userObj.displayName, avatarUrl: userObj.avatarUrl };
    } else {
      const user = store.users.find(u => u.email === email.toLowerCase());
      if (!user) {
        return res.status(401).json({ success: false, error: 'Invalid email or password' });
      }
      const match = await bcrypt.compare(password, user.password);
      if (!match) {
        return res.status(401).json({ success: false, error: 'Invalid email or password' });
      }
      userObj = { id: user.id, username: user.username, email: user.email, displayName: user.displayName, avatarUrl: user.avatarUrl };
    }

    const token = jwt.sign({ id: userObj.id, username: userObj.username, email: userObj.email }, JWT_SECRET, { expiresIn: '30d' });

    res.json({
      success: true,
      message: 'Login successful',
      token,
      user: userObj
    });
  } catch (err) {
    console.error('Login error:', err);
    res.status(500).json({ success: false, error: 'Internal server error' });
  }
});

// Forgot Password - Send OTP
router.post('/forgot-password', otpRequestLimiter, async (req, res) => {
  try {
    const { email } = req.body;
    if (!email) {
      return res.status(400).json({ success: false, message: 'Email is required' });
    }

    if (!isValidEmail(email)) {
      return res.status(400).json({ success: false, message: 'Invalid email format' });
    }

    // Generic response to prevent account enumeration
    const genericResponse = { success: true, message: 'If an account with that email exists, a password reset OTP has been sent.' };

    if (mongoose.connection.readyState !== 1) {
      // In-memory fallback for forgot password
      const user = store.users.find(u => u.email === email.toLowerCase());
      if (!user) {
        return res.json(genericResponse);
      }

      const otp = generateOTP();
      user.passwordResetOtpHash = hashOTP(otp);
      user.passwordResetOtpExpiresAt = new Date(Date.now() + OTP_EXPIRY_MINUTES * 60 * 1000);
      user.passwordResetOtpAttempts = 0;
      user.passwordResetOtpVerified = false;

      const emailService = getEmailService();
      await emailService.sendPasswordResetOTP(email.toLowerCase(), otp, OTP_EXPIRY_MINUTES);

      return res.json(genericResponse);
    }

    const user = await User.findOne({ email: email.toLowerCase() });
    if (!user) {
      return res.json(genericResponse);
    }

    // Rate limit OTP requests per email: max 3 per hour
    const now = new Date();
    const oneHourAgo = new Date(now.getTime() - 60 * 60 * 1000);

    if (user.passwordResetOtpRequestWindowStart && user.passwordResetOtpRequestWindowStart > oneHourAgo) {
      if (user.passwordResetOtpRequestCount >= MAX_OTP_REQUESTS_PER_HOUR) {
        return res.status(429).json({ success: false, message: 'Too many OTP requests. Please try again later.' });
      }
      user.passwordResetOtpRequestCount += 1;
    } else {
      user.passwordResetOtpRequestWindowStart = now;
      user.passwordResetOtpRequestCount = 1;
    }

    // Generate and store OTP
    const otp = generateOTP();
    user.passwordResetOtpHash = hashOTP(otp);
    user.passwordResetOtpExpiresAt = new Date(Date.now() + OTP_EXPIRY_MINUTES * 60 * 1000);
    user.passwordResetOtpAttempts = 0;
    user.passwordResetOtpVerified = false;

    await user.save();

    // Send OTP via email service
    const emailService = getEmailService();
    await emailService.sendPasswordResetOTP(email.toLowerCase(), otp, OTP_EXPIRY_MINUTES);

    res.json(genericResponse);
  } catch (err) {
    console.error('Forgot password error:', err);
    res.status(500).json({ success: false, message: 'Internal server error' });
  }
});

// Verify OTP
router.post('/verify-reset-otp', async (req, res) => {
  try {
    const { email, otp } = req.body;
    if (!email || !otp) {
      return res.status(400).json({ success: false, message: 'Email and OTP are required' });
    }

    if (mongoose.connection.readyState !== 1) {
      // In-memory fallback
      const user = store.users.find(u => u.email === email.toLowerCase());
      if (!user || !user.passwordResetOtpHash) {
        return res.status(400).json({ success: false, message: 'No OTP request found. Please request a new OTP.' });
      }

      if (user.passwordResetOtpExpiresAt && new Date() > new Date(user.passwordResetOtpExpiresAt)) {
        return res.status(400).json({ success: false, message: 'OTP has expired. Please request a new OTP.' });
      }

      if ((user.passwordResetOtpAttempts || 0) >= MAX_OTP_ATTEMPTS) {
        return res.status(400).json({ success: false, message: 'Too many OTP attempts. Please request a new OTP.' });
      }

      const hashedInput = hashOTP(otp);
      if (hashedInput !== user.passwordResetOtpHash) {
        user.passwordResetOtpAttempts = (user.passwordResetOtpAttempts || 0) + 1;
        const remaining = MAX_OTP_ATTEMPTS - user.passwordResetOtpAttempts;
        return res.status(400).json({ 
          success: false, 
          message: remaining > 0 ? `Invalid OTP. ${remaining} attempt(s) remaining.` : 'Too many OTP attempts. Please request a new OTP.'
        });
      }

      user.passwordResetOtpVerified = true;
      return res.json({ success: true, message: 'OTP verified successfully' });
    }

    const user = await User.findOne({ email: email.toLowerCase() });
    if (!user || !user.passwordResetOtpHash) {
      return res.status(400).json({ success: false, message: 'No OTP request found. Please request a new OTP.' });
    }

    // Check expiry
    if (user.passwordResetOtpExpiresAt && new Date() > user.passwordResetOtpExpiresAt) {
      return res.status(400).json({ success: false, message: 'OTP has expired. Please request a new OTP.' });
    }

    // Check attempts
    if (user.passwordResetOtpAttempts >= MAX_OTP_ATTEMPTS) {
      return res.status(400).json({ success: false, message: 'Too many OTP attempts. Please request a new OTP.' });
    }

    // Verify OTP
    const hashedInput = hashOTP(otp);
    if (hashedInput !== user.passwordResetOtpHash) {
      user.passwordResetOtpAttempts += 1;
      await user.save();

      const remaining = MAX_OTP_ATTEMPTS - user.passwordResetOtpAttempts;
      return res.status(400).json({ 
        success: false, 
        message: remaining > 0 ? `Invalid OTP. ${remaining} attempt(s) remaining.` : 'Too many OTP attempts. Please request a new OTP.'
      });
    }

    // OTP is correct - mark as verified
    user.passwordResetOtpVerified = true;
    await user.save();

    res.json({ success: true, message: 'OTP verified successfully' });
  } catch (err) {
    console.error('Verify OTP error:', err);
    res.status(500).json({ success: false, message: 'Internal server error' });
  }
});

// Reset Password (after OTP verification)
router.post('/reset-password', async (req, res) => {
  try {
    const { email, newPassword } = req.body;
    if (!email || !newPassword) {
      return res.status(400).json({ success: false, message: 'Email and new password are required' });
    }

    if (newPassword.length < 6) {
      return res.status(400).json({ success: false, message: 'Password must be at least 6 characters' });
    }

    if (mongoose.connection.readyState !== 1) {
      // In-memory fallback
      const user = store.users.find(u => u.email === email.toLowerCase());
      if (!user) {
        return res.status(400).json({ success: false, message: 'Invalid request' });
      }

      if (!user.passwordResetOtpVerified) {
        return res.status(400).json({ success: false, message: 'OTP verification required before resetting password' });
      }

      user.password = await bcrypt.hash(newPassword, 12);
      user.passwordResetOtpHash = null;
      user.passwordResetOtpExpiresAt = null;
      user.passwordResetOtpAttempts = 0;
      user.passwordResetOtpVerified = false;

      return res.json({ success: true, message: 'Password reset successfully' });
    }

    const user = await User.findOne({ email: email.toLowerCase() });
    if (!user) {
      return res.status(400).json({ success: false, message: 'Invalid request' });
    }

    // Server-side verification: only allow if OTP was verified
    if (!user.passwordResetOtpVerified) {
      return res.status(400).json({ success: false, message: 'OTP verification required before resetting password' });
    }

    // Hash new password and update
    const hashedPassword = await bcrypt.hash(newPassword, 12);
    user.password = hashedPassword;
    
    // Invalidate OTP state
    user.passwordResetOtpHash = null;
    user.passwordResetOtpExpiresAt = null;
    user.passwordResetOtpAttempts = 0;
    user.passwordResetOtpVerified = false;

    await user.save();

    res.json({ success: true, message: 'Password reset successfully' });
  } catch (err) {
    console.error('Reset password error:', err);
    res.status(500).json({ success: false, message: 'Internal server error' });
  }
});

// Get current user profile
router.get('/me', requireAuth, async (req, res) => {
  try {
    let userObj;
    if (mongoose.connection.readyState === 1) {
      const u = await User.findById(req.user.id).select('-password -passwordResetOtpHash -passwordResetOtpExpiresAt -passwordResetOtpAttempts -passwordResetOtpVerified');
      if (!u) return res.status(404).json({ success: false, error: 'User not found' });
      userObj = { id: u._id.toString(), username: u.username, email: u.email, displayName: u.displayName, avatarUrl: u.avatarUrl, preferences: u.preferences };
    } else {
      const u = store.users.find(x => x.id === req.user.id);
      if (!u) return res.status(404).json({ success: false, error: 'User not found' });
      userObj = { id: u.id, username: u.username, email: u.email, displayName: u.displayName, avatarUrl: u.avatarUrl };
    }
    res.json(userObj);
  } catch (err) {
    res.status(500).json({ success: false, error: 'Internal server error' });
  }
});

// Update user profile
router.patch('/me', requireAuth, async (req, res) => {
  try {
    const { displayName, avatarUrl, preferences } = req.body;
    
    if (mongoose.connection.readyState === 1) {
      const updateFields = {};
      if (displayName !== undefined) updateFields.displayName = displayName;
      if (avatarUrl !== undefined) updateFields.avatarUrl = avatarUrl;
      if (preferences !== undefined) updateFields.preferences = preferences;

      const u = await User.findByIdAndUpdate(req.user.id, updateFields, { new: true })
        .select('-password -passwordResetOtpHash -passwordResetOtpExpiresAt -passwordResetOtpAttempts -passwordResetOtpVerified');
      
      if (!u) return res.status(404).json({ success: false, error: 'User not found' });
      res.json({ success: true, user: { id: u._id.toString(), username: u.username, email: u.email, displayName: u.displayName, avatarUrl: u.avatarUrl, preferences: u.preferences } });
    } else {
      const u = store.users.find(x => x.id === req.user.id);
      if (!u) return res.status(404).json({ success: false, error: 'User not found' });
      if (displayName !== undefined) u.displayName = displayName;
      if (avatarUrl !== undefined) u.avatarUrl = avatarUrl;
      res.json({ success: true, user: { id: u.id, username: u.username, email: u.email, displayName: u.displayName, avatarUrl: u.avatarUrl } });
    }
  } catch (err) {
    res.status(500).json({ success: false, error: 'Internal server error' });
  }
});

// Logout
router.post('/logout', (req, res) => {
  res.json({ success: true, message: 'Logged out successfully' });
});

module.exports = router;
