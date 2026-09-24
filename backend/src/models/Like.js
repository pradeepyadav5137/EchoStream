const mongoose = require('mongoose');

const likeSchema = new mongoose.Schema({
  userId: { type: String, required: true },
  songId: { type: String, required: true },
  createdAt: { type: Date, default: Date.now }
});

likeSchema.index({ userId: 1, songId: 1 }, { unique: true });

module.exports = mongoose.model('Like', likeSchema);
