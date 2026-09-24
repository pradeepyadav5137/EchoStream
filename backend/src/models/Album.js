const mongoose = require('mongoose');

const albumSchema = new mongoose.Schema({
  title: { type: String, required: true },
  artist: { type: String, required: true },
  artistId: { type: String, default: '' },
  artworkUrl: { type: String, default: '' },
  releaseYear: { type: Number, default: 2024 },
  genre: { type: String, default: 'Pop' },
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model('Album', albumSchema);
