const mongoose = require('mongoose');

const commentSchema = new mongoose.Schema({
  userId: { type: String, required: true },
  username: { type: String, required: true },
  avatarUrl: { type: String, default: '' },
  songId: { type: String, required: true },
  text: { type: String, required: true },
  createdAt: { type: Date, default: Date.now }
});

commentSchema.index({ songId: 1, createdAt: -1 });

module.exports = mongoose.model('Comment', commentSchema);
