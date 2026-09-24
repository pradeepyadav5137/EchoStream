const mongoose = require('mongoose');

const followSchema = new mongoose.Schema({
  userId: { type: String, required: true },
  artistId: { type: String, required: true },
  createdAt: { type: Date, default: Date.now }
});

followSchema.index({ userId: 1, artistId: 1 }, { unique: true });

module.exports = mongoose.model('Follow', followSchema);
