const Playlist = require('../models/Playlist');

const createPlaylist = async (req, res) => {
  try {
    const { name, description } = req.body;
    if (!name) return res.status(400).json({ success: false, message: 'Name is required' });

    let playlist = await Playlist.findOne({ userId: req.user.id, name });
    if (playlist) {
      if (description) {
        playlist.description = description;
        await playlist.save();
      }
      return res.status(200).json({ success: true, playlist });
    }

    const newPlaylist = new Playlist({
      userId: req.user.id,
      name,
      description
    });
    await newPlaylist.save();
    res.status(201).json({ success: true, playlist: newPlaylist });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to create playlist', error: error.message });
  }
};

const getPlaylists = async (req, res) => {
  try {
    const playlists = await Playlist.find({ userId: req.user.id }).sort({ createdAt: -1 });
    res.json({ success: true, playlists });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to get playlists', error: error.message });
  }
};

const getPlaylist = async (req, res) => {
  try {
    const playlist = await Playlist.findOne({ _id: req.params.id, userId: req.user.id });
    if (!playlist) return res.status(404).json({ success: false, message: 'Playlist not found' });
    res.json({ success: true, playlist });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to get playlist', error: error.message });
  }
};

const updatePlaylist = async (req, res) => {
  try {
    const { name, description } = req.body;
    const playlist = await Playlist.findOneAndUpdate(
      { _id: req.params.id, userId: req.user.id },
      { name, description },
      { new: true }
    );
    if (!playlist) return res.status(404).json({ success: false, message: 'Playlist not found' });
    res.json({ success: true, playlist });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to update playlist', error: error.message });
  }
};

const deletePlaylist = async (req, res) => {
  try {
    const playlist = await Playlist.findOneAndDelete({ _id: req.params.id, userId: req.user.id });
    if (!playlist) return res.status(404).json({ success: false, message: 'Playlist not found' });
    res.json({ success: true, message: 'Playlist deleted' });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to delete playlist', error: error.message });
  }
};

const addSong = async (req, res) => {
  try {
    const { songId, title, artist, album, thumbnail, duration, source } = req.body;
    const playlist = await Playlist.findOne({ _id: req.params.id, userId: req.user.id });
    if (!playlist) return res.status(404).json({ success: false, message: 'Playlist not found' });

    // Prevent duplicate
    if (playlist.songs.find(s => s.songId === songId)) {
      return res.status(400).json({ success: false, message: 'Song already in playlist' });
    }

    playlist.songs.push({ songId, title, artist, album, thumbnail, duration, source });
    
    if (!playlist.coverImage && thumbnail) {
      playlist.coverImage = thumbnail;
    }
    
    await playlist.save();
    res.json({ success: true, playlist });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to add song', error: error.message });
  }
};

const removeSong = async (req, res) => {
  try {
    const playlist = await Playlist.findOne({ _id: req.params.id, userId: req.user.id });
    if (!playlist) return res.status(404).json({ success: false, message: 'Playlist not found' });

    playlist.songs = playlist.songs.filter(s => s.songId !== req.params.songId);
    await playlist.save();
    res.json({ success: true, playlist });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to remove song', error: error.message });
  }
};

module.exports = {
  createPlaylist, getPlaylists, getPlaylist, updatePlaylist, deletePlaylist, addSong, removeSong
};
