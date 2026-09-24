const mongoose = require('mongoose');

const favoriteSchema = new mongoose.Schema({
  userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  songId: { type: String, required: true },
  title: { type: String, required: true },
  artist: { type: String, required: true },
  album: { type: String, default: 'Unknown Album' },
  thumbnail: { type: String, default: '' },
  duration: { type: Number, default: 0 },
  source: { type: String, default: 'youtube' }
}, { timestamps: true });

// Ensure unique favorite per user
favoriteSchema.index({ userId: 1, songId: 1 }, { unique: true });

module.exports = mongoose.model('Favorite', favoriteSchema);
