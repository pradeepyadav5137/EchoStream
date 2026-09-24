const Favorite = require('../models/Favorite');

const addFavorite = async (req, res) => {
  try {
    const { songId } = req.params;
    const { title, artist, album, thumbnail, duration, source } = req.body;
    
    let fav = await Favorite.findOne({ userId: req.user.id, songId });
    if (fav) return res.status(400).json({ success: false, message: 'Already favorited' });

    fav = new Favorite({
      userId: req.user.id,
      songId, title, artist, album, thumbnail, duration, source
    });
    await fav.save();
    res.status(201).json({ success: true, favorite: fav });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to add favorite', error: error.message });
  }
};

const removeFavorite = async (req, res) => {
  try {
    const { songId } = req.params;
    await Favorite.findOneAndDelete({ userId: req.user.id, songId });
    res.json({ success: true, message: 'Removed from favorites' });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to remove favorite', error: error.message });
  }
};

const getFavorites = async (req, res) => {
  try {
    const favorites = await Favorite.find({ userId: req.user.id }).sort({ createdAt: -1 });
    res.json({ success: true, favorites });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to get favorites', error: error.message });
  }
};

module.exports = {
  addFavorite, removeFavorite, getFavorites
};
