const mongoose = require('mongoose');

const historySchema = new mongoose.Schema({
  userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  songId: { type: String, required: true },
  title: { type: String, required: true },
  artist: { type: String, required: true },
  album: { type: String, default: 'Unknown Album' },
  thumbnail: { type: String, default: '' },
  duration: { type: Number, default: 0 },
  source: { type: String, default: 'jiosaavn' },
  playedAt: { type: Date, default: Date.now }
});

historySchema.index({ userId: 1, playedAt: -1 });
historySchema.index({ userId: 1, songId: 1 });

module.exports = mongoose.model('History', historySchema);
