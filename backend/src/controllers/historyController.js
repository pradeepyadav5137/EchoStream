const History = require('../models/History');

const addHistory = async (req, res) => {
  try {
    const { songId, title, artist, album, thumbnail, duration, source } = req.body;
    
    // Remove previous instance if exists to avoid duplicates
    await History.findOneAndDelete({ userId: req.user.id, songId });

    const history = new History({
      userId: req.user.id,
      songId, title, artist, album, thumbnail, duration, source
    });
    await history.save();

    // Keep only last 100
    const count = await History.countDocuments({ userId: req.user.id });
    if (count > 100) {
      const oldest = await History.find({ userId: req.user.id }).sort({ playedAt: 1 }).limit(count - 100);
      const oldestIds = oldest.map(h => h._id);
      await History.deleteMany({ _id: { $in: oldestIds } });
    }

    res.status(201).json({ success: true, history });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to add history', error: error.message });
  }
};

const getHistory = async (req, res) => {
  try {
    const history = await History.find({ userId: req.user.id }).sort({ playedAt: -1 });
    res.json({ success: true, history });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to get history', error: error.message });
  }
};

const clearHistory = async (req, res) => {
  try {
    await History.deleteMany({ userId: req.user.id });
    res.json({ success: true, message: 'History cleared' });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to clear history', error: error.message });
  }
};

module.exports = {
  addHistory, getHistory, clearHistory
};
