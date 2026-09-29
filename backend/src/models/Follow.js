const mongoose = require('mongoose');

const followSchema = new mongoose.Schema({
  userId: { type: String, required: true },
  artistId: { type: String, required: true },
  artistName: { type: String, default: '' },
  artistImage: { type: String, default: '' },
  createdAt: { type: Date, default: Date.now }
});

followSchema.index({ userId: 1, artistId: 1 }, { unique: true });
followSchema.index({ userId: 1 });
followSchema.index({ artistId: 1 });

module.exports = mongoose.model('Follow', followSchema);
