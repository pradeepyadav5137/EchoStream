const mongoose = require('mongoose');

const userSchema = new mongoose.Schema({
  username: { type: String, required: true, unique: true, trim: true },
  email: { type: String, required: true, unique: true, trim: true, lowercase: true },
  password: { type: String, required: true },
  displayName: { type: String, default: '' },
  avatarUrl: { type: String, default: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500' },
  profileImage: { type: String, default: '' },
  preferences: {
    favoriteGenres: [{ type: String }],
    favoriteArtists: [{ type: String }],
    theme: { type: String, default: 'dark' },
    notifications: { type: Boolean, default: true }
  },
  // Password reset OTP fields
  passwordResetOtpHash: { type: String, default: null },
  passwordResetOtpExpiresAt: { type: Date, default: null },
  passwordResetOtpAttempts: { type: Number, default: 0 },
  passwordResetOtpVerified: { type: Boolean, default: false },
  passwordResetOtpRequestCount: { type: Number, default: 0 },
  passwordResetOtpRequestWindowStart: { type: Date, default: null },
  createdAt: { type: Date, default: Date.now },
  updatedAt: { type: Date, default: Date.now }
});

// Update updatedAt on save
userSchema.pre('save', function(next) {
  this.updatedAt = new Date();
  next();
});

module.exports = mongoose.model('User', userSchema);
