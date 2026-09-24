const mongoose = require('mongoose');

const songSchema = new mongoose.Schema({
  title: { type: String, required: true },
  artist: { type: String, required: true },
  artistId: { type: String, default: '' },
  album: { type: String, default: '' },
  albumId: { type: String, default: '' },
  genre: { type: String, default: 'Pop' },
  duration: { type: Number, default: 180 }, // in seconds
  audioUrl: { type: String, required: true },
  artworkUrl: { type: String, default: '' },
  playCount: { type: Number, default: 0 },
  likeCount: { type: Number, default: 0 },
  isTrending: { type: Boolean, default: false },
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Song', songSchema);
