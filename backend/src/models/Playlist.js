const mongoose = require('mongoose');

const songSchema = new mongoose.Schema({
  songId: { type: String, required: true },
  title: { type: String, required: true },
  artist: { type: String, required: true },
  album: { type: String, default: 'Unknown Album' },
  thumbnail: { type: String, default: '' },
  duration: { type: Number, default: 0 },
  source: { type: String, default: 'youtube' }
});

const playlistSchema = new mongoose.Schema({
  userId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  name: { type: String, required: true },
  description: { type: String, default: '' },
  coverImage: { type: String, default: '' },
  songs: [songSchema]
}, { timestamps: true });

module.exports = mongoose.model('Playlist', playlistSchema);
