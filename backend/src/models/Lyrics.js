const mongoose = require('mongoose');

const lyricsSchema = new mongoose.Schema({
  songId: { type: String, required: true, unique: true },
  plainLyrics: { type: String, default: '' },
  syncedLyrics: [
    {
      timeMs: Number,
      text: String
    }
  ],
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Lyrics', lyricsSchema);
