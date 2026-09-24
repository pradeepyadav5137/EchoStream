// In-memory fallback data store for when MongoDB is not connected
const store = {
  users: [],
  songs: [],
  artists: [],
  albums: [],
  playlists: [],
  likes: [],
  comments: [],
  follows: [],
  history: [],
  lyrics: []
};

module.exports = store;
