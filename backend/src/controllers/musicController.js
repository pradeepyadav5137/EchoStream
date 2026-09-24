const musicService = require('../services/musicService');

const search = async (req, res) => {
  try {
    const { q, page = 1, limit = 20 } = req.query;
    if (!q) {
      return res.status(400).json({ success: false, message: 'Query parameter q is required' });
    }
    
    // yt-search doesn't have native pagination but we can simulate it by fetching more or just returning the top N.
    // Given yt-search usually returns ~50-100 results, we can slice it.
    const allResults = await musicService.searchSongs(q, 100);
    const startIndex = (Number(page) - 1) * Number(limit);
    const endIndex = startIndex + Number(limit);
    
    const results = allResults.slice(startIndex, endIndex);
    const hasMore = endIndex < allResults.length;
    
    res.json({
      success: true,
      query: q,
      page: Number(page),
      limit: Number(limit),
      hasMore,
      results
    });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to search songs', error: error.message });
  }
};

const getSong = async (req, res) => {
  try {
    const { id } = req.params;
    const song = await musicService.getSongDetails(id);
    res.json({ success: true, song });
  } catch (error) {
    res.status(404).json({ success: false, message: 'Song not found', error: error.message });
  }
};

const getStream = async (req, res) => {
  try {
    const { id } = req.params;
    const streamUrl = await musicService.getStreamUrl(id);
    res.json({ success: true, url: streamUrl });
  } catch (error) {
    res.status(500).json({ success: false, message: 'Failed to resolve stream URL', error: error.message });
  }
};

module.exports = {
  search,
  getSong,
  getStream
};
