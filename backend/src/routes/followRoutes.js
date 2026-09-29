const express = require('express');
const { requireAuth } = require('../middleware/auth');
const Follow = require('../models/Follow');

const router = express.Router();

router.use(requireAuth);

// Get followed artists
router.get('/', async (req, res) => {
  try {
    const follows = await Follow.find({ userId: req.user.id }).sort({ createdAt: -1 });
    res.json({ success: true, follows });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to get follows', error: error.message });
  }
});

// Follow an artist
router.post('/:artistId', async (req, res) => {
  try {
    const { artistId } = req.params;
    const { artistName, artistImage } = req.body;
    
    let follow = await Follow.findOne({ userId: req.user.id, artistId });
    if (follow) {
      return res.status(400).json({ success: false, message: 'Already following this artist' });
    }

    follow = new Follow({
      userId: req.user.id,
      artistId,
      artistName: artistName || '',
      artistImage: artistImage || ''
    });
    await follow.save();

    res.status(201).json({ success: true, follow });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to follow artist', error: error.message });
  }
});

// Unfollow an artist
router.delete('/:artistId', async (req, res) => {
  try {
    const { artistId } = req.params;
    await Follow.findOneAndDelete({ userId: req.user.id, artistId });
    res.json({ success: true, message: 'Unfollowed artist' });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to unfollow artist', error: error.message });
  }
});

module.exports = router;
